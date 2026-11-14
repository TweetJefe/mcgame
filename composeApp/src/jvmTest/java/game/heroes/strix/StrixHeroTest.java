package game.heroes.strix;

import game.core.Game;
import game.draft.Draft;
import game.draft.DraftActionResult;
import game.draft.DraftRules;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroEffect;
import game.heroes.NoHeroRules;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicStep;
import game.mechanics.MechanicPhase;
import game.mechanics.Mechanics;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.bolotnik.BolotnikAbility;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.GinsengFixtures;
import game.heroes.sunwukong.SunWukongAbility;
import game.history.PositionHistoryMechanic;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.Success;
import game.moves.TurnRejection;
import game.persistence.GameSaveFile;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.state.Termination;
import game.turns.TurnFlowMechanic;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import model.Figure;
import model.Position;
import model.figures.Bishop;
import model.figures.King;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Queen;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static model.FigureColor.BLACK;
import static model.FigureColor.WHITE;
import static org.junit.Assert.*;

public class StrixHeroTest {
    private static Map<Position, Figure> board() {
        var board=GinsengFixtures.board();
        GinsengFixtures.put(board,Pawn.class,WHITE,"c2");
        GinsengFixtures.put(board,Pawn.class,WHITE,"f2");
        return board;
    }
    private static Game create(Map<Position,Figure> board,Map<Position,HeroType> other){
        var assignments=new HashMap<>(other);assignments.put(pos("c2"),HeroType.STRIX);
        return Game.fromMobaPosition(new GamePosition(board,WHITE,CastlingRights.none(),null,0),assignments,HeroRules.defaults(),()->0L);
    }
    public static Game basic(){return create(board(),Map.of());}
    public static Game ready(){return charge(basic(),"c2",2);}
    private static Game charge(Game game,String cell,int value){
        var s=game.getSnapshot();var states=new HashMap<>(s.pieces());var p=s.pieceAt(pos(cell));
        states.put(p.id(),p.withAbilityState(p.cooldown(),value,p.storedPieceId()));
        return Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),states,s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),
                s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),
                s.drawOffer(),s.heroRules(),s.draft(),s.mechanics()),()->0L);
    }
    public static Game restored(Game game)throws Exception{
        byte[] bytes=GameSaveFile.encode(game.getSnapshot());
        Game loaded=Game.fromSnapshot(GameSaveFile.decode(bytes),()->0L);
        assertArrayEquals(bytes,GameSaveFile.encode(loaded.getSnapshot()));return loaded;
    }
    private static void ok(MoveResult result){assertEquals(Success.SUCCESS,result);}
    private static void play(Game game,String from,String to){ok(game.moveFigure(game.getCurrentPlayer(),pos(from),pos(to)));ok(game.endTurn(game.getCurrentPlayer()));}
    private static void rewind(Game game,String from,String target){ok(game.useAbility(game.getCurrentPlayer(),pos(from),pos(target),StrixAbility.REWIND));}
    private static void skip(Game game,String from){ok(game.useAbility(game.getCurrentPlayer(),pos(from),null,StrixAbility.TIME_SKIP));}
    public static Game rewindReady(){
        Game game=basic();play(game,"b1","a3");play(game,"g8","f6");play(game,"f2","f3");play(game,"f6","g8");return game;
    }

    @Test public void skillRequiresTwoCompletedOwnTurnsAndNeverConsumesResourcesOnFailure()throws Exception{
        Game game=basic();byte[] before=GameSaveFile.encode(game.getSnapshot());
        assertTrue(game.getAbilityTargets(pos("c2"),StrixAbility.REWIND).isEmpty());
        assertFalse(game.useAbility(game.getCurrentPlayer(),pos("c2"),pos("b1"),StrixAbility.REWIND).isSuccess());
        assertArrayEquals(before,GameSaveFile.encode(game.getSnapshot()));
        assertEquals(AbilityRejection.ULTIMATE_NOT_READY,game.useAbility(game.getCurrentPlayer(),pos("c2"),null,StrixAbility.TIME_SKIP));
        assertArrayEquals(before,GameSaveFile.encode(game.getSnapshot()));
    }
    @Test public void alliedPieceReturnsToTheStartOfItsSecondPreviousCompletedOwnTurn()throws Exception{
        Game game=rewindReady();long target=game.getPieceState(pos("a3")).id();
        assertTrue(game.getAbilityTargets(pos("c2"),StrixAbility.REWIND).contains(pos("a3")));
        byte[] before=GameSaveFile.encode(game.getSnapshot());
        game.getAbilityTargets(pos("c2"),StrixAbility.REWIND);
        assertArrayEquals(before,GameSaveFile.encode(game.getSnapshot()));
        rewind(game,"c2","a3");
        assertEquals(target,game.getPieceState(pos("b1")).id());
        assertEquals(Knight.class,game.getPieceState(pos("b1")).type());
        assertEquals(4,game.getPieceState(pos("c2")).cooldown());
        assertFalse(game.getTurnState().moveUsed());
        assertEquals(TurnRejection.MOVE_REQUIRED,game.endTurn(game.getCurrentPlayer()));
        restored(game);
    }
    @Test public void selfRewindKeepsPawnIdentityAndItsCharge()throws Exception{
        Game game=ready();long id=game.getPieceState(pos("c2")).id();
        play(game,"c2","c3");play(game,"g8","f6");play(game,"c3","c4");play(game,"f6","g8");
        rewind(game,"c4","c4");
        assertEquals(id,game.getPieceState(pos("c2")).id());
        assertEquals(2,game.getPieceState(pos("c2")).ultimateCharge());
        restored(game);
    }
    @Test public void enemyUsesItsOwnersHistoryRatherThanTheCastersTurns()throws Exception{
        var board=board();GinsengFixtures.empty(board,"c2");GinsengFixtures.put(board,Pawn.class,WHITE,"e3");
        Game game=Game.fromMobaPosition(new GamePosition(board,WHITE,CastlingRights.none(),null,0),Map.of(pos("e3"),HeroType.STRIX),HeroRules.defaults(),()->0L);
        play(game,"b1","a3");play(game,"g8","f6");play(game,"a3","b1");play(game,"f6","g4");
        rewind(game,"e3","g4");assertEquals(Knight.class,game.getPieceState(pos("g8")).type());restored(game);
    }
    @Test public void occupiedDestinationAndKingTargetsAreRejected()throws Exception{
        var board=board();GinsengFixtures.empty(board,"a1");GinsengFixtures.put(board,King.class,WHITE,"h1");GinsengFixtures.put(board,Rook.class,WHITE,"a1");
        Game game=create(board,Map.of());play(game,"b1","a3");play(game,"g8","f6");play(game,"a1","b1");play(game,"f6","g8");
        byte[] before=GameSaveFile.encode(game.getSnapshot());
        assertFalse(game.useAbility(game.getCurrentPlayer(),pos("c2"),pos("a3"),StrixAbility.REWIND).isSuccess());
        assertFalse(game.useAbility(game.getCurrentPlayer(),pos("c2"),pos("h1"),StrixAbility.REWIND).isSuccess());
        assertArrayEquals(before,GameSaveFile.encode(game.getSnapshot()));
    }
    @Test public void ultimateGivesANewFullTurnAndSurvivesLoadingBeforeAndAfterTheSkip()throws Exception{
        Game game=ready();skip(game,"c2");assertEquals(0,game.getPieceState(pos("c2")).ultimateCharge());
        assertEquals(TurnRejection.MOVE_REQUIRED,game.endTurn(game.getCurrentPlayer()));
        game=restored(game);play(game,"b1","a3");
        assertEquals(WHITE,game.getCurrentPlayer().color());assertEquals(3,game.getTurnState().number());
        assertFalse(game.getTurnState().moveUsed());assertFalse(game.getTurnState().abilityUsed());
        assertTrue(game.getBlackPlayer().getMadeMoves().isEmpty());
        assertEquals(1,game.getEvents().stream().filter(e->e.isEffect(TurnFlowMechanic.SKIPPED)).count());
        game=restored(game);play(game,"a3","b1");assertEquals(BLACK,game.getCurrentPlayer().color());restored(game);
    }
    @Test public void extraTurnIsAllowedWhileTheOpponentRemainsInCheck()throws Exception{
        var board=board();GinsengFixtures.put(board,Rook.class,WHITE,"g5");
        Game game=charge(create(board,Map.of()),"c2",2);skip(game,"c2");play(game,"g5","h5");
        assertEquals(WHITE,game.getCurrentPlayer().color());assertFalse(game.getOutcome().isFinished());
        game=restored(game);play(game,"b1","a3");assertEquals(BLACK,game.getCurrentPlayer().color());restored(game);
    }
    @Test public void checkmateEndsTheGameBeforeASkippedTurnWouldBeTaken()throws Exception{
        var board=board();GinsengFixtures.put(board,Rook.class,WHITE,"g5");GinsengFixtures.put(board,Queen.class,WHITE,"g6");GinsengFixtures.empty(board,"g8");
        Game game=charge(create(board,Map.of()),"c2",2);skip(game,"c2");play(game,"g5","h5");
        assertEquals(Termination.CHECKMATE,game.getOutcome().reason());assertEquals(WHITE,game.getOutcome().winner());
        assertEquals(0,game.getEvents().stream().filter(e->e.isEffect(TurnFlowMechanic.SKIPPED)).count());restored(game);
    }
    @Test public void skippedTurnExpiresEnemyEffectsAndTicksTheirCooldown()throws Exception{
        var board=board();GinsengFixtures.put(board,Pawn.class,BLACK,"b7");
        Game game=charge(create(board,Map.of(pos("b7"),HeroType.SUN_WUKONG)),"c2",2);
        play(game,"b1","a3");ok(game.useAbility(game.getCurrentPlayer(),pos("b7"),null,SunWukongAbility.SHAPESHIFT));play(game,"g8","f6");
        assertNotEquals(Pawn.class,game.getPieceState(pos("b7")).type());
        skip(game,"c2");play(game,"a3","b1");
        assertEquals(WHITE,game.getCurrentPlayer().color());assertEquals(Pawn.class,game.getPieceState(pos("b7")).type());
        assertEquals(3,game.getPieceState(pos("b7")).cooldown());restored(game);
    }
    @Test public void skipExpiresEnPassantWithoutInventingAnEnemyMove()throws Exception{
        Game game=ready();skip(game,"c2");play(game,"f2","f4");
        assertNull(game.getPosition().enPassantTarget());assertEquals(1,game.getMoves().size());restored(game);
    }
    @Test public void rewindIntoMudRunsTheUsualCaptureAndRewardReactions()throws Exception{
        var board=board();GinsengFixtures.put(board,Knight.class,BLACK,"c4");
        Game game=create(board,Map.of(pos("c4"),HeroType.BOLOTNIK));
        play(game,"b1","a3");ok(game.useAbility(game.getCurrentPlayer(),pos("c4"),pos("b1"),BolotnikAbility.MUD));play(game,"g8","f6");
        play(game,"f2","f3");play(game,"f6","g8");
        rewind(game,"c2","a3");assertEquals(HeroType.BOLOTNIK,game.getPieceState(pos("b1")).hero());
        assertEquals(6,game.getEconomy().blackHalves());restored(game);
    }
    @Test public void ginsengCopiesTheUltimateWithoutAdditionalCharge()throws Exception{
        var board=board();GinsengFixtures.put(board,Bishop.class,BLACK,"e2");
        Game game=charge(charge(create(board,Map.of(pos("e2"),HeroType.GINSENG)),"c2",2),"e2",2);
        skip(game,"c2");play(game,"b1","a3");play(game,"a3","b1");
        ok(game.useAbility(game.getCurrentPlayer(),pos("e2"),pos("c2"),GinsengAbility.COPY_SPELL));play(game,"g8","f6");play(game,"b1","a3");
        ok(game.useAbility(game.getCurrentPlayer(),new AbilityRequest(pos("e2"),StrixAbility.TIME_SKIP,null,null,true)));
        play(game,"f6","g8");assertEquals(BLACK,game.getCurrentPlayer().color());
        assertEquals(HeroType.GINSENG,game.getPieceState(pos("e2")).hero());assertEquals(0,game.getPieceState(pos("e2")).ultimateCharge());restored(game);
    }
    @Test public void ginsengCopiesRewindAndPreservesBothHeroesResources()throws Exception{
        var board=board();GinsengFixtures.put(board,Bishop.class,BLACK,"e2");
        Game game=charge(create(board,Map.of(pos("e2"),HeroType.GINSENG)),"e2",2);
        play(game,"c2","c3");play(game,"g8","f6");play(game,"c3","c4");play(game,"f6","g8");
        rewind(game,"c4","c4");play(game,"b1","a3");
        ok(game.useAbility(game.getCurrentPlayer(),pos("e2"),pos("c2"),GinsengAbility.COPY_SPELL));play(game,"g8","f6");play(game,"a3","b1");
        int cooldown=game.getPieceState(pos("c2")).cooldown();
        ok(game.useAbility(game.getCurrentPlayer(),new AbilityRequest(pos("e2"),StrixAbility.REWIND,pos("c2"),null,true)));
        assertEquals(HeroType.STRIX,game.getPieceState(pos("c4")).hero());assertEquals(cooldown,game.getPieceState(pos("c4")).cooldown());
        assertEquals(0,game.getPieceState(pos("e2")).cooldown());restored(game);
    }
    @Test public void capturesChargeTheUltimateIncludingPawnsAndItCanFollowTheMandatoryMove()throws Exception{
        var board=board();GinsengFixtures.put(board,Pawn.class,BLACK,"d3");GinsengFixtures.put(board,Pawn.class,BLACK,"e4");
        Game game=create(board,Map.of());play(game,"c2","d3");play(game,"g8","f6");
        ok(game.moveFigure(game.getCurrentPlayer(),pos("d3"),pos("e4")));assertEquals(2,game.getPieceState(pos("e4")).ultimateCharge());
        skip(game,"e4");ok(game.endTurn(game.getCurrentPlayer()));assertEquals(WHITE,game.getCurrentPlayer().color());restored(game);
    }
    @Test public void draftAndXmlUseTheNewHeroAndKeepPositionFramesAndSkipRequests()throws Exception{
        var draft=new Draft(new DraftRules(0,1));assertEquals(DraftActionResult.SUCCESS,draft.pick(WHITE,HeroType.STRIX,pos("c2")));
        draft.finishPicks(BLACK);Game game=Game.mobaFromDraft(draft.getSnapshot(),HeroRules.defaults(),600,()->0L);
        String xml=new String(GameSaveFile.encode(game.getSnapshot()),StandardCharsets.UTF_8);
        assertTrue(xml.contains("formatVersion=\"19\""));assertTrue(xml.contains("strixRules"));assertTrue(xml.contains("positionHistory.frame"));
        assertEquals(draft.getSnapshot(),restored(game).getDraft());
    }

    @Test public void blackCanSkipWhiteAndReceiveANewFullTurn() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Pawn.class, BLACK, "b7");
        Game game = create(board, Map.of(pos("b7"), HeroType.STRIX));
        play(game, "b1", "a3");
        game = charge(game, "b7", 2);
        skip(game, "b7"); play(game, "g8", "f6");
        assertEquals(BLACK, game.getCurrentPlayer().color());
        assertEquals(4, game.getTurnState().number());
        assertFalse(game.getTurnState().moveUsed());
        restored(game);
    }

    @Test public void clocksChargeOnlyThePlayerWhoseActionsActuallyTakeTime() throws Exception {
        AtomicLong time = new AtomicLong();
        Game game = Game.fromSnapshot(ready().getSnapshot(), time::get);
        game.startClock(60);
        time.addAndGet(2_000_000_000L);
        skip(game, "c2");
        time.addAndGet(1_000_000_000L);
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b1"), pos("a3")));
        time.addAndGet(1_000_000_000L);
        ok(game.endTurn(game.getCurrentPlayer()));
        assertEquals(60_000, game.getClockSnapshot().getBlackMillis());
        assertEquals(56_000, game.getClockSnapshot().getWhiteMillis());
        assertEquals(WHITE, game.getClockSnapshot().activeColor());
        restored(game);
    }

    @Test public void rewindHonorsItsRadiusAndFourOwnTurnCooldown() throws Exception {
        Game game = rewindReady();
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertFalse(game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("g8"), StrixAbility.REWIND).isSuccess());
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
        rewind(game, "c2", "a3");
        play(game, "b1", "a3"); play(game, "g8", "f6");
        assertEquals(3, game.getPieceState(pos("c2")).cooldown());
        assertEquals(AbilityRejection.ABILITY_COOLDOWN, game.getAbilityAvailability(pos("c2"), StrixAbility.REWIND));
        play(game, "a3", "b1"); play(game, "f6", "g8");
        play(game, "b1", "a3"); play(game, "g8", "f6");
        play(game, "a3", "b1"); play(game, "f6", "g8");
        assertEquals(0, game.getPieceState(pos("c2")).cooldown());
        restored(game);
    }

    @Test public void forgedSkipMarkersOrMissingPositionFramesCannotBeLoaded() throws Exception {
        Game game = ready(); skip(game, "c2"); play(game, "b1", "a3");
        String xml = new String(GameSaveFile.encode(game.getSnapshot()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("turnFlow.skipped"));
        assertThrows(java.io.IOException.class, () -> GameSaveFile.decode(xml.replace("turnFlow.requested", "turnFlow.skipped")
                .getBytes(StandardCharsets.UTF_8)));
        var snapshot = game.getSnapshot();
        assertThrows(IllegalArgumentException.class, () -> Game.fromSnapshot(snapshot.withMechanics(snapshot.mechanics()
                .with(PositionHistoryMechanic.KEY, PositionHistoryMechanic.KEY.initial())), () -> 0L));
    }

    private enum Unit { INSTANCE }
    private static final MechanicKey<Unit> REJECT_KEY = new MechanicKey<>("test.rejectSkip", Unit.class, Unit.INSTANCE);
    private static final MechanicDefinition<Unit> REJECT_SKIP = new MechanicDefinition<>() {
        @Override public MechanicKey<Unit> key() { return REJECT_KEY; }
        @Override public List<MechanicStep> steps() {
            return List.of(Mechanics.step("test.rejectSkip", MechanicPhase.SKIP_TURN, "turnFlow.consume",
                    s -> AbilityRejection.INVALID_ABILITY_TARGET));
        }
    };
    private static final HeroDefinition<NoHeroRules> ORDINARY = new HeroDefinition<>(HeroType.NONE, "Обычная фигура",
            Set.of(), NoHeroRules.class, NoHeroRules.INSTANCE, null, OrdinaryHero::new, List.of(REJECT_SKIP));
    private static final class OrdinaryHero extends AbstractHero {
        OrdinaryHero(NoHeroRules rules) { super(ORDINARY, List.of(), null, 0); }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                        PieceState target, AbilityRequest request) { return HeroEffect.empty(); }
    }

    @Test public void rejectedAutomaticTransitionDoesNotPublishAnyPartialEndOrSkip() throws Exception {
        var definitions = new ArrayList<>(HeroDefinitions.all());
        definitions.removeIf(d -> d.type() == HeroType.NONE); definitions.add(ORDINARY);
        Game game = Game.fromMobaPosition(new GamePosition(board(), WHITE, CastlingRights.none(), null, 0),
                Map.of(pos("c2"), HeroType.STRIX), HeroRules.defaults(definitions), () -> 0L);
        game = charge(game, "c2", 2); skip(game, "c2");
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b1"), pos("a3")));
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, game.endTurn(game.getCurrentPlayer()));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
    }

    @Test public void anUnconsumedSkipCannotRemainInThePastEvenIfItsLedgerMatches() {
        Game game = ready(); skip(game, "c2"); play(game, "b1", "a3");
        var s = game.getSnapshot();
        var events = s.events().stream().filter(e -> !e.isEffect(TurnFlowMechanic.SKIPPED)).toList();
        var pending = TurnFlowMechanic.INSTANCE.restore(new GameSnapshot(s.version(), s.mode(), s.field(), s.pieces(),
                s.turn(), s.active(), s.castlingRights(), s.enPassantTarget(), s.halfmoveClock(), s.repetitions(), s.clock(),
                s.outcome(), s.moves(), events, s.whitePoints(), s.blackPoints(), s.whiteCaptures(), s.blackCaptures(),
                s.drawOffer(), s.heroRules(), s.draft(), s.mechanics()));
        var forged = new GameSnapshot(s.version(), s.mode(), s.field(), s.pieces(), s.turn(), s.active(), s.castlingRights(),
                s.enPassantTarget(), s.halfmoveClock(), s.repetitions(), s.clock(), s.outcome(), s.moves(), events,
                s.whitePoints(), s.blackPoints(), s.whiteCaptures(), s.blackCaptures(), s.drawOffer(), s.heroRules(), s.draft(),
                s.mechanics().with(TurnFlowMechanic.KEY, pending));
        assertEquals(pending, TurnFlowMechanic.INSTANCE.restore(forged));
        assertThrows(IllegalArgumentException.class, () -> TurnFlowMechanic.INSTANCE.validate(forged));
    }
}
