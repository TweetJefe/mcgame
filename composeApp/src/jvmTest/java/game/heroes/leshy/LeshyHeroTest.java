package game.heroes.leshy;

import game.core.Game;
import game.draft.Draft;
import game.draft.DraftRules;
import game.draft.DraftActionResult;
import game.heroes.AbilityRequest;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.GinsengFixtures;
import game.heroes.faun.FaunAbility;
import game.heroes.pudge.PudgeAbility;
import game.heroes.moleminer.MoleminerAbility;
import game.heroes.bolotnik.BolotnikAbility;
import game.heroes.phoenix.PhoenixHeroTest;
import game.mechanics.PieceRestriction;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.StatusRejection;
import game.moves.Success;
import game.persistence.GameSaveFile;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Figure;
import model.Position;
import model.figures.Archbishop;
import model.figures.Bishop;
import model.figures.King;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static game.heroes.phoenix.PhoenixHeroTest.charge;
import static game.heroes.phoenix.PhoenixHeroTest.play;
import static game.heroes.phoenix.PhoenixHeroTest.restored;
import static game.heroes.phoenix.PhoenixHeroTest.ok;
import static model.FigureColor.WHITE;
import static model.FigureColor.BLACK;
import static org.junit.Assert.*;

public class LeshyHeroTest {
    public static Map<Position, Figure> board() {
        var board = PhoenixHeroTest.board();
        GinsengFixtures.put(board, Pawn.class, WHITE,"c2");
        return board;
    }
    public static Game create(Map<Position, Figure> board, Map<Position, HeroType> other) {
        var assignments = new HashMap<>(other);
        assignments.put(pos("c2"), HeroType.LESHY);
        return Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0), assignments, HeroRules.defaults(), () -> 0L);
    }
    public static Game basic() { return create(board(), Map.of()); }
    public static Game forestReady() {
        var board = board();
        GinsengFixtures.put(board, Rook.class, WHITE,"b2");
        GinsengFixtures.put(board, Pawn.class, BLACK,"d2");
        return charge(create(board, Map.of()),"c2", 1);
    }
    private static void vine(Game game, String actor) {
        ok(game.useAbility(game.getCurrentPlayer(), pos(actor), null, LeshyAbility.VINE));
    }
    private static void forest(Game game, String actor) {
        ok(game.useAbility(game.getCurrentPlayer(), pos(actor), null, LeshyAbility.FOREST));
    }
    private static LeshyMechanic.State state(Game game) { return game.getSnapshot().mechanics().get(LeshyMechanic.KEY); }

    @Test public void vineHasThreeForwardCellsFourTurnCooldownAndDoesNotUseTheMove() throws Exception {
        Game game = basic();
        vine(game,"c2");
        assertEquals(Set.of(pos("c3"), pos("c4"), pos("c5")), state(game).cells());
        assertEquals(4, game.getPieceState(pos("c2")).cooldown());
        assertTrue(game.getTurnState().abilityUsed());
        assertFalse(game.getTurnState().moveUsed());
        assertFalse(game.endTurn(game.getCurrentPlayer()).isSuccess());
        restored(game);
    }
    @Test public void rootingPreventsMovesButKeepsAbilitiesAndExistingIdentity() throws Exception {
        var board = board(); GinsengFixtures.put(board, Knight.class, WHITE,"c3");
        Game game = create(board, Map.of(pos("c3"), HeroType.FAUN));
        long id = game.getPieceState(pos("c3")).id();
        vine(game,"c2");
        assertEquals(StatusRejection.ROOTED, game.moveFigure(game.getCurrentPlayer(), pos("c3"), pos("b5")));
        assertTrue(game.getLegalMoves(pos("c3")).getCanMove().isEmpty());
        play(game,"h2","h3");
        play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(), pos("c3"), pos("e3"), FaunAbility.CLOUD));
        assertEquals(id, game.getPieceState(pos("c3")).id());
        assertEquals(HeroType.FAUN, game.getPieceState(pos("c3")).hero());
        restored(game);
    }
    @Test public void rootingAnEnemyRemovesItsCheck() throws Exception {
        var board = board(); GinsengFixtures.empty(board,"h1");
        GinsengFixtures.put(board, King.class, WHITE,"e3"); GinsengFixtures.put(board, Knight.class, BLACK,"c4");
        Game game = create(board, Map.of());
        assertTrue(game.isInCheck());
        vine(game,"c2");
        assertFalse(game.isInCheck());
        restored(game);
    }
    @Test public void emptyVineCellsBlockSlidingAndDoublePawnMoves() throws Exception {
        var board = board(); GinsengFixtures.put(board, Rook.class, WHITE,"b4");
        Game game = create(board, Map.of());
        vine(game,"c2");
        assertEquals(StatusRejection.PASSAGE_BLOCKED, game.moveFigure(game.getCurrentPlayer(), pos("b4"), pos("f4")));
        assertEquals(StatusRejection.PASSAGE_BLOCKED, game.moveFigure(game.getCurrentPlayer(), pos("c2"), pos("c4")));
        ok(game.moveFigure(game.getCurrentPlayer(), pos("c2"), pos("c3")));
        restored(game);
    }
    @Test public void knightsAndCompositeKnightJumpsCrossTheVine() throws Exception {
        for (var body : List.of(Knight.class, Archbishop.class)) {
            var board = board(); GinsengFixtures.put(board, body, WHITE,"b3");
            Game game = create(board, Map.of());
            vine(game,"c2");
            ok(game.moveFigure(game.getCurrentPlayer(), pos("b3"), pos("d4")));
            restored(game);
        }
    }
    @Test public void vineBlocksAttackRaysButDoesNotStopTheHook() throws Exception {
        var board = board(); GinsengFixtures.empty(board,"h1"); GinsengFixtures.put(board, King.class, WHITE,"b4");
        GinsengFixtures.put(board, Rook.class, BLACK,"h4");
        Game game = create(board, Map.of());
        assertTrue(game.isInCheck());
        vine(game,"c2");
        assertFalse(game.isInCheck());
        restored(game);

        board = board(); GinsengFixtures.put(board, Rook.class, WHITE,"b4"); GinsengFixtures.put(board, Pawn.class, BLACK,"d4");
        game = create(board, Map.of(pos("b4"), HeroType.PUDGE));
        vine(game,"c2");
        play(game,"h2","h3");
        play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(), pos("b4"), pos("d4"), PudgeAbility.HOOK));
        assertNotNull(game.getPieceState(pos("c4")));
        restored(game);
    }
    @Test public void forcedDisplacementOffVineImmediatelyRemovesRoot() throws Exception {
        var board = board(); GinsengFixtures.put(board, Rook.class, WHITE,"c3"); GinsengFixtures.put(board, Bishop.class, WHITE,"f2");
        Game game = create(board, Map.of(pos("f2"), HeroType.GINSENG));
        vine(game,"c2");
        play(game,"h2","h3");
        play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("f2"), GinsengAbility.DISPLACE, pos("c3"), pos("d3"), false)));
        ok(game.moveFigure(game.getCurrentPlayer(), pos("d3"), pos("d4")));
        restored(game);
    }
    @Test public void vineExpiresAtEndOfThirdFollowingOwnTurn() throws Exception {
        Game game = basic();
        vine(game,"c2");
        play(game,"h2","h3");
        play(game,"g8","f6");
        play(game,"h3","h4");
        play(game,"f6","g8");
        play(game,"h4","h5");
        play(game,"g8","f6");
        assertFalse(state(game).vines().isEmpty());
        assertEquals(1, game.getPieceState(pos("c2")).cooldown());
        play(game,"h5","h6");
        assertTrue(state(game).vines().isEmpty());
        restored(game);
    }
    @Test public void kingOnVineLaneRejectsCastAtomically() throws Exception {
        var board = board(); GinsengFixtures.empty(board,"h1"); GinsengFixtures.put(board, King.class, WHITE,"c4");
        Game game = create(board, Map.of()); byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertEquals(StatusRejection.KING_IMMUNE, game.useAbility(game.getCurrentPlayer(), pos("c2"), null, LeshyAbility.VINE));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
    }
    @Test public void friendlyTreesReturnButEnemyTreesRemainPermanently() throws Exception {
        Game game = forestReady();
        long ally = game.getPieceState(pos("b2")).id(), enemy = game.getPieceState(pos("d2")).id();
        forest(game,"c2");
        assertEquals(2, state(game).trees().size());
        assertEquals(0, game.getPieceState(pos("c2")).ultimateCharge());
        assertEquals(0, state(game).trees().get(enemy).until());
        assertEquals(StatusRejection.PROTECTED_OBSTACLE, game.moveFigure(game.getCurrentPlayer(), pos("b2"), pos("b3")));
        play(game,"h2","h3");
        play(game,"g8","f6");
        play(game,"h3","h4");
        play(game,"f6","g8");
        assertTrue(state(game).trees().containsKey(ally));
        game = restored(game);
        play(game,"h4","h5");
        play(game,"g8","f6");
        assertFalse(state(game).trees().containsKey(ally));
        assertTrue(state(game).trees().containsKey(enemy));
        assertEquals(ally, game.getPieceState(pos("b2")).id());
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b2"), pos("b3")));
        restored(game);
    }
    @Test public void treeCannotBeCapturedOrHookedAndFailedActionsPreserveResources() throws Exception {
        var board = board(); GinsengFixtures.put(board, Rook.class, WHITE,"b4");
        GinsengFixtures.put(board, Pawn.class, WHITE,"b2"); GinsengFixtures.put(board, Pawn.class, BLACK,"d2");
        GinsengFixtures.put(board, Rook.class, WHITE,"d4");
        Game game = charge(create(board, Map.of(pos("b4"), HeroType.PUDGE)),"c2", 1);
        forest(game,"c2");
        play(game,"h2","h3");
        play(game,"g8","f6");
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertEquals(StatusRejection.PROTECTED_OBSTACLE, game.moveFigure(game.getCurrentPlayer(), pos("d4"), pos("d2")));
        assertEquals(StatusRejection.PROTECTED_OBSTACLE, game.useAbility(game.getCurrentPlayer(), pos("b4"), pos("b2"), PudgeAbility.HOOK));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
        restored(game);
    }
    @Test public void transformedHeroCannotUseAbilitiesOrItems() throws Exception {
        var board = board(); GinsengFixtures.put(board, Rook.class, WHITE,"b2");
        Game game = charge(create(board, Map.of(pos("b2"), HeroType.PUDGE)),"c2", 1);
        forest(game,"c2");
        play(game,"h2","h3");
        play(game,"g8","f6");
        assertEquals(StatusRejection.PROTECTED_OBSTACLE, game.getAbilityAvailability(pos("b2"), PudgeAbility.HOOK));
        assertEquals(StatusRejection.PROTECTED_OBSTACLE, game.useItem(game.getCurrentPlayer(), game.getPieceState(pos("b2")).id(), 0));
        restored(game);
    }
    @Test public void ginsengCopiesVineWithoutCooldownAndKeepsTheLongEffectAfterConsumption() throws Exception {
        var board = board(); GinsengFixtures.put(board, Pawn.class, BLACK, "c2"); GinsengFixtures.put(board, Bishop.class, WHITE,"f2");
        Game game = charge(create(board, Map.of(pos("f2"), HeroType.GINSENG)),"f2", 2);
        play(game,"h2","h3");
        vine(game,"c2");
        play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(), pos("f2"), pos("c2"), GinsengAbility.COPY_SPELL));
        play(game,"h3","h4");
        play(game,"f6","g8");
        int cd = game.getPieceState(pos("c2")).cooldown();
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("f2"), LeshyAbility.VINE, null, null, true)));
        assertTrue(state(game).cells().containsAll(List.of(pos("f3"), pos("f4"), pos("f5"))));
        assertEquals(0, game.getPieceState(pos("f2")).cooldown());
        assertEquals(cd, game.getPieceState(pos("c2")).cooldown());
        assertEquals(HeroType.GINSENG, game.getPieceState(pos("f2")).hero());
        assertTrue(game.getSpells().borrowed().isEmpty());
        restored(game);
    }
    @Test public void ginsengCopiesForestWithoutAdditionalCaptureCharge() throws Exception {
        var board = board(); GinsengFixtures.put(board, Pawn.class, BLACK, "c2"); GinsengFixtures.put(board, Bishop.class, WHITE,"f2");
        for (String cell:new String[]{"b2","g2"}) GinsengFixtures.put(board, Pawn.class, WHITE, cell);
        for (String cell:new String[]{"d2","e2"}) GinsengFixtures.put(board, Pawn.class, BLACK, cell);
        Game game = charge(charge(create(board, Map.of(pos("f2"), HeroType.GINSENG)),"f2", 2),"c2", 1);
        play(game,"h2","h3");
        forest(game,"c2");
        play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(), pos("f2"), pos("c2"), GinsengAbility.COPY_SPELL));
        play(game,"h3","h4");
        play(game,"f6","g8");
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("f2"), LeshyAbility.FOREST, null, null, true)));
        assertTrue(state(game).trees().containsKey(game.getPieceState(pos("e2")).id()));
        assertEquals(0, game.getPieceState(pos("f2")).ultimateCharge());
        assertEquals(HeroType.GINSENG, game.getPieceState(pos("f2")).hero());
        assertTrue(game.getSpells().borrowed().isEmpty());
        restored(game);
    }
    @Test public void draftAndXmlRegisterTheNewPawnHero() throws Exception {
        var draft = new Draft(new DraftRules(0, 1));
        assertEquals(8, draft.getPlacementTargets(WHITE, HeroType.LESHY).size());
        assertEquals(DraftActionResult.SUCCESS, draft.pick(WHITE, HeroType.LESHY, pos("h2"))); draft.finishPicks(BLACK);
        Game game = Game.mobaFromDraft(draft.getSnapshot(), HeroRules.defaults(), 600, () -> 0L);
        String xml = new String(GameSaveFile.encode(game.getSnapshot()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("formatVersion=\"21\""));
        assertEquals(draft.getSnapshot(), restored(game).getDraft());
    }

    @Test public void blackVinesPointDownAndEdgesTruncateTheLane() throws Exception {
        var board = board(); GinsengFixtures.put(board, Pawn.class, BLACK,"c7");
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                Map.of(pos("c7"), HeroType.LESHY), HeroRules.defaults(), () -> 0L);
        play(game,"h2","h3");
        vine(game,"c7");
        assertEquals(Set.of(pos("c6"), pos("c5"), pos("c4")), state(game).cells());
        restored(game);
        board = board(); GinsengFixtures.put(board, Pawn.class, WHITE,"a6");
        game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                Map.of(pos("a6"), HeroType.LESHY), HeroRules.defaults(), () -> 0L);
        vine(game,"a6");
        assertEquals(Set.of(pos("a7"), pos("a8")), state(game).cells());
        restored(game);
    }
    @Test public void emptyForestDoesNotConsumeChargeAndKingIsSkipped() throws Exception {
        Game empty = charge(basic(),"c2", 1); byte[] before = GameSaveFile.encode(empty.getSnapshot());
        assertFalse(empty.useAbility(empty.getCurrentPlayer(), pos("c2"), null, LeshyAbility.FOREST).isSuccess());
        assertArrayEquals(before, GameSaveFile.encode(empty.getSnapshot()));
        var board = board(); GinsengFixtures.empty(board,"h1"); GinsengFixtures.put(board, King.class, WHITE,"b2");
        GinsengFixtures.put(board, Pawn.class, BLACK,"d2");
        Game game = charge(create(board, Map.of()),"c2", 1);
        forest(game,"c2");
        assertEquals(1, state(game).trees().size());
        assertFalse(state(game).trees().containsKey(game.getPieceState(pos("b2")).id()));
        restored(game);
    }
    @Test public void vineExpiryCannotLeaveTheKingInCheck() throws Exception {
        var board = board(); GinsengFixtures.empty(board,"h1"); GinsengFixtures.put(board, King.class, WHITE,"e3");
        GinsengFixtures.put(board, Knight.class, BLACK,"c4");
        Game game = create(board, Map.of());
        vine(game,"c2");
        play(game,"h2","h3");
        play(game,"g8","f6");
        play(game,"h3","h4");
        play(game,"f6","g8");
        play(game,"h4","h5");
        play(game,"g8","f6");
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertFalse(game.moveFigure(game.getCurrentPlayer(), pos("h5"), pos("h6")).isSuccess());
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
        play(game,"e3","f2");
        assertTrue(state(game).vines().isEmpty());
        restored(game);
    }
    @Test public void effectsOutliveTheCasterAndDoNotTriggerHisDeathPassive() throws Exception {
        var board = board(); GinsengFixtures.put(board, Pawn.class, WHITE,"b2"); GinsengFixtures.put(board, Pawn.class, BLACK,"d2");
        GinsengFixtures.put(board, Rook.class, BLACK,"c8");
        Game game = charge(create(board, Map.of()),"c2", 1);
        long enemy = game.getPieceState(pos("d2")).id();
        forest(game,"c2");
        play(game,"h2","h3");
        play(game,"c8","c2");
        assertTrue(state(game).trees().containsKey(enemy));
        play(game,"h3","h4");
        play(game,"g8","f6");
        play(game,"h4","h5");
        assertEquals(1, state(game).trees().size());
        assertTrue(state(game).trees().containsKey(enemy));
        restored(game);
    }
    @Test public void anExplosionSkipsTreesWhileStillKillingTheBomber() throws Exception {
        var board = board(); GinsengFixtures.put(board, Pawn.class, WHITE,"b2"); GinsengFixtures.put(board, Pawn.class, BLACK,"e4");
        Game game = charge(charge(create(board, Map.of(pos("e4"), HeroType.MOLEMINER)),"c2", 1),"e4", 2);
        long tree = game.getPieceState(pos("b2")).id(), bomber = game.getPieceState(pos("e4")).id();
        forest(game,"c2");
        play(game,"h2","h3");
        ok(game.useAbility(game.getCurrentPlayer(), pos("e4"), pos("b2"), MoleminerAbility.DETONATE));
        assertEquals(tree, game.getPieceState(pos("b2")).id());
        assertTrue(game.getPieceState(bomber).isRemoved());
        restored(game);
    }
    @Test public void changedDurationsAndCooldownUseTheGameRules() throws Exception {
        var rules = HeroRules.defaults().with(LeshyDefinition.INSTANCE, new LeshyRules(2, 1, 2, 1, 3));
        Game game = Game.fromMobaPosition(new GamePosition(board(), WHITE, CastlingRights.none(), null, 0), Map.of(pos("c2"), HeroType.LESHY), rules, () -> 0L);
        vine(game,"c2");
        assertEquals(2, state(game).cells().size());
        assertEquals(2, game.getPieceState(pos("c2")).cooldown());
        play(game,"h2","h3");
        play(game,"g8","f6");
        assertFalse(state(game).vines().isEmpty());
        play(game,"h3","h4");
        assertTrue(state(game).vines().isEmpty());
        restored(game);
    }
    @Test public void forgedLifetimeAndOldFormatAreRejected() throws Exception {
        Game game = basic();
        vine(game,"c2");
        var s = game.getSnapshot();
        var state = state(game);
        var vine = state.vines().getFirst();
        var forged = new LeshyMechanic.State(List.of(new LeshyMechanic.Vine(vine.source(), vine.castTurn(), vine.until()+2, vine.cells())), state.trees());
        assertThrows(IllegalArgumentException.class, () -> Game.fromSnapshot(s.withMechanics(s.mechanics().with(LeshyMechanic.KEY, forged)), () -> 0L));
        String xml = new String(GameSaveFile.encode(s), StandardCharsets.UTF_8);
        String old = xml.replace("formatVersion=\"21\"","formatVersion=\"20\"").replaceAll("(?s)<leshyRules [^>]*/>","");
        assertThrows(java.io.IOException.class, () -> GameSaveFile.decode(old.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void aTreeCannotEmergeFromItsOldMudAndDormantMudCanBeSaved() throws Exception {
        var board = board(); GinsengFixtures.put(board, Knight.class, BLACK,"d2"); GinsengFixtures.put(board, Rook.class, WHITE,"f2");
        Game game = charge(create(board, Map.of(pos("d2"), HeroType.BOLOTNIK)),"c2", 1);
        play(game,"h2","h3");
        ok(game.useAbility(game.getCurrentPlayer(), pos("d2"), pos("f3"), BolotnikAbility.MUD));
        play(game,"g8","f6");
        forest(game,"c2");
        play(game,"h3","h4");
        play(game,"f6","g8");
        ok(game.moveFigure(game.getCurrentPlayer(), pos("f2"), pos("f3")));
        assertEquals(Rook.class, game.getPieceState(pos("f3")).type());
        assertEquals(HeroType.BOLOTNIK, game.getPieceState(pos("d2")).hero());
        restored(game);
    }
}
