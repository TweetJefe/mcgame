package game.heroes.bolotnik;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.TurnRejection;
import game.core.Game;
import game.economy.GoldRules;
import game.heroes.*;
import game.heroes.lumeshard.LumeshardRules;
import game.heroes.pudge.PudgeRules;
import game.moves.MoveResult;
import game.persistence.GameSaveFile;
import game.scenarios.BolotnikScenario;
import game.state.*;
import game.terrain.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class BolotnikTest {
    private final Field factory=new Field(8);
    private Map<Position,Figure> board(String whiteKing,String blackKing) {
        var b=new HashMap<Position,Figure>();
        for(int r=0;r<8;r++) for(int c=0;c<8;c++) { Position p=new Position(r,c);b.put(p,factory.createFigure(Empty.class,p,NONE)); }
        put(b,King.class,WHITE,whiteKing);put(b,King.class,BLACK,blackKing);return b;
    }
    private void put(Map<Position,Figure> b,Class<? extends Figure> type,FigureColor color,String p) { b.put(pos(p),factory.createFigure(type,pos(p),color)); }
    private Game create(Map<Position,Figure> b,Map<Position,HeroType> heroes,HeroRules rules) {
        return Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),heroes,rules,()->0L);
    }
    private Game create(Map<Position,Figure> b,String hero) { return create(b,Map.of(pos(hero),HeroType.BOLOTNIK_KNIGHT),HeroRules.prototype()); }
    private Game scenario(BolotnikScenario s) { return s.createGame(()->0L); }
    private Game charged(Game g,String square) {
        var s=g.getSnapshot();var pieces=new HashMap<>(s.pieces());PieceState p=s.pieceAt(pos(square));pieces.put(p.id(),p.withAbilityState(p.cooldown(),2,null));
        return Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),pieces,s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain()),()->0L);
    }
    private MoveResult cast(Game g,String from,String target,Ability a) { return g.useAbility(g.getCurrentPlayer(),pos(from),target==null?null:pos(target),a); }
    private void mud(Game g,String from,String target) { assertEquals(Success.SUCCESS,cast(g,from,target,Ability.MUD)); }
    private void ambush(Game g,String from) { assertEquals(Success.SUCCESS,cast(g,from,null,Ability.AMBUSH)); }
    private void move(Game g,String from,String to) { assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos(from),pos(to))); }
    private void end(Game g) { assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer())); }
    private void reject(Game g,MoveResult expected,java.util.function.Supplier<MoveResult> command) { var before=g.getSnapshot();assertEquals(expected,command.get());assertEquals(before,g.getSnapshot()); }
    @Test public void standardFactoryAssignsTheTwoKnights() {
        Game g=Game.mobaPrototype(HeroRules.prototype(),600);
        assertEquals(HeroType.BOLOTNIK_KNIGHT,g.getPieceState(pos("g1")).hero());
        assertEquals(HeroType.BOLOTNIK_KNIGHT,g.getPieceState(pos("g8")).hero());assertTrue(g.getTerrain().cells().isEmpty());
    }
    @Test public void radiusIsTheWholeFourSquareAreaAndFriendlyTargetsAreAllowed() {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"b2");put(b,Pawn.class,WHITE,"b3");put(b,Pawn.class,WHITE,"f6");put(b,Pawn.class,BLACK,"e5");
        Game g=create(b,"b2");assertTrue(g.getAbilityTargets(pos("b2"),Ability.MUD).contains(pos("f6")));
        reject(g,AbilityRejection.ABILITY_OUT_OF_RANGE,()->cast(g,"b2","h7",Ability.MUD));
        reject(g,AbilityRejection.INVALID_ABILITY_TARGET,()->cast(g,"b2","e5",Ability.MUD));
        reject(g,StatusRejection.KING_IMMUNE,()->cast(g,"b2","a1",Ability.MUD));
        var before=g.getSnapshot();g.getAbilityTargets(pos("b2"),Ability.MUD);assertEquals(before,g.getSnapshot());
        mud(g,"b2","f6");assertEquals(Pawn.class,g.getPieceState(pos("f6")).type());assertEquals(4,g.getPieceState(pos("b2")).cooldown());
        assertFalse(g.getTurnState().moveUsed());reject(g,TurnRejection.MOVE_REQUIRED,()->g.endTurn(g.getCurrentPlayer()));
    }
    @Test public void mudMayBePlacedUnderItsCaster() {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"d4");Game g=create(b,"d4");mud(g,"d4","d4");
        assertEquals(HeroType.BOLOTNIK_KNIGHT,g.getPieceState(pos("d4")).hero());assertEquals(1,g.getTerrain().cells().size());
        move(g,"d4","f5");assertTrue(g.getTerrain().cells().containsKey(pos("d4")));
    }
    @Test public void friendlyFiguresCanEnterAndCrossTheirOwnMud() {
        for(String target:List.of("d3","d4")) {
            var b=board("a1","h8");put(b,Knight.class,WHITE,"b2");put(b,Rook.class,WHITE,"d2");
            Game g=create(b,"b2");long id=g.getPieceState(pos("b2")).id();mud(g,"b2","d3");move(g,"d2",target);
            assertEquals(pos("b2"),g.getPieceState(id).position());assertEquals(1,g.getTerrain().cells().size());assertEquals(0,g.getEconomy().whiteHalves());
        }
    }
    @Test public void enemyCannotCrossMudButLandingTriggersTheSameHero() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"e2");put(b,Rook.class,BLACK,"d8");
        Game g=create(b,"e2");long id=g.getPieceState(pos("e2")).id();mud(g,"e2","d4");move(g,"a1","b1");end(g);
        reject(g,AbilityRejection.MUD_BLOCKED,()->g.moveFigure(g.getCurrentPlayer(),pos("d8"),pos("d2")));
        move(g,"d8","d4");assertEquals(id,g.getPieceState(pos("d4")).id());assertEquals(1,g.getPieceState(id).ultimateCharge());
        assertEquals(10,g.getEconomy().whiteHalves());assertTrue(g.getTerrain().cells().isEmpty());
        assertNull(g.getMoves().getLast().getCapturedFigure());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void bothLongSideSquaresBlockKnightJumps() {
        for(String cell:List.of("b2","b3")) {
            Game g=scenario(BolotnikScenario.KNIGHT);mud(g,"d4",cell);move(g,"a1","a2");end(g);
            assertFalse(g.getLegalMoves(pos("b1")).getCanMove().contains(pos("c3")));
            reject(g,AbilityRejection.MUD_BLOCKED,()->g.moveFigure(g.getCurrentPlayer(),pos("b1"),pos("c3")));
            move(g,"b1","d2");
        }
    }
    @Test public void aKnightLandingOnMudIsCapturedRatherThanBlocked() {
        Game g=scenario(BolotnikScenario.KNIGHT);mud(g,"d4","c3");move(g,"a1","a2");end(g);move(g,"b1","c3");
        assertEquals(HeroType.BOLOTNIK_KNIGHT,g.getPieceState(pos("c3")).hero());assertEquals(6,g.getEconomy().whiteHalves());
    }
    @Test public void mudCanBlockARookCheckWithoutDirectlyAttackingTheKing() {
        var b=board("d1","h8");put(b,Knight.class,WHITE,"e2");put(b,Rook.class,BLACK,"d8");
        Game g=create(b,"e2");assertTrue(g.isInCheck());mud(g,"e2","d4");assertFalse(g.isInCheck());move(g,"e2","g3");
    }
    @Test public void mudBlocksKnightAttacksUsingTheSameJumpRule() {
        var b=board("c3","h8");put(b,Knight.class,WHITE,"d4");put(b,Knight.class,BLACK,"b1");
        Game g=create(b,"d4");assertTrue(g.isInCheck());mud(g,"d4","b2");assertFalse(g.isInCheck());
    }
    @Test public void friendlyAttacksPassThroughFriendlyMud() {
        var b=board("a1","d8");put(b,Knight.class,WHITE,"e2");put(b,Rook.class,WHITE,"e1");
        Game g=create(b,"e2");mud(g,"e2","d4");move(g,"e1","d1");
        assertTrue(rules.moba.AttackRules.isInCheck(g.getGameField().getGameField(),g.getSnapshot().pieces(),BLACK,g.getTerrain()));
    }
    @Test public void cornerUltimateProducesOnlyTwoCells() throws Exception {
        var b=board("a1","a8");put(b,Knight.class,WHITE,"h8");Game g=charged(create(b,"h8"),"h8");long id=g.getPieceState(pos("h8")).id();
        ambush(g,"h8");assertEquals(Set.of(pos("g8"),pos("h8")),g.getTerrain().cells().keySet());
        assertEquals(PieceState.Status.HIDDEN_BOG,g.getPieceState(id).status());assertEquals(0,g.getPieceState(id).ultimateCharge());
        assertNull(g.getPieceState(pos("h8")));GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void hiddenHeroCannotLeaveVoluntarilyAndTheUltimateDoesNotExpire() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"d4");Game g=charged(create(b,"d4"),"d4");long id=g.getPieceState(pos("d4")).id();
        ambush(g,"d4");
        for(int i=0;i<4;i++) { move(g,i%2==0?"a1":"b1",i%2==0?"b1":"a1");end(g);move(g,i%2==0?"h8":"g8",i%2==0?"g8":"h8");end(g); }
        reject(g,MovementRejection.EMPTY_SOURCE,()->cast(g,"d4","f4",Ability.MUD));
        assertEquals(PieceState.Status.HIDDEN_BOG,g.getPieceState(id).status());assertEquals(3,g.getTerrain().cells().size());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void ultimateImmediatelyTakesTheEnemyClosestToAAndClearsAllItsCells() throws Exception {
        Game g=scenario(BolotnikScenario.LEFTMOST);long id=g.getPieceState(pos("f5")).id();ambush(g,"f5");
        assertEquals(id,g.getPieceState(pos("e5")).id());assertEquals(Pawn.class,g.getPieceState(pos("g5")).type());
        assertEquals(1,g.getPieceState(id).ultimateCharge());assertEquals(6,g.getEconomy().whiteHalves());
        assertTrue(g.getTerrain().cells().isEmpty());reject(g,TurnRejection.MOVE_REQUIRED,()->g.endTurn(g.getCurrentPlayer()));
        move(g,"a1","b1");GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void capturingAnAllyOnUltimateTerrainReleasesTheHiddenHero() throws Exception {
        Game g=scenario(BolotnikScenario.AMBUSH);long id=g.getPieceState(pos("f5")).id();ambush(g,"f5");
        assertEquals(Pawn.class,g.getPieceState(pos("e5")).type());move(g,"a1","b1");end(g);move(g,"e8","e5");
        assertEquals(id,g.getPieceState(pos("e5")).id());assertEquals(14,g.getEconomy().whiteHalves());assertEquals(2,g.getEconomy().blackHalves());
        assertTrue(g.getTerrain().cells().isEmpty());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void anyKingInTheUltimateFootprintRejectsTheEntireCast() {
        for(boolean friendly:List.of(true,false)) {
            var b=board(friendly?"e4":"a1",friendly?"h8":"e4");put(b,Knight.class,WHITE,"d4");
            Game g=charged(create(b,"d4"),"d4");reject(g,StatusRejection.KING_IMMUNE,()->cast(g,"d4",null,Ability.AMBUSH));
        }
    }
    @Test public void mudExpiresAfterTwoFollowingOwnerTurnsAndCooldownAfterFourStarts() {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"d4");Game g=create(b,"d4");mud(g,"d4","f4");
        move(g,"a1","b1");end(g);move(g,"h8","g8");end(g);assertEquals(3,g.getPieceState(pos("d4")).cooldown());
        move(g,"b1","a1");end(g);assertEquals(1,g.getTerrain().cells().size());move(g,"g8","h8");end(g);
        move(g,"a1","b1");assertEquals(1,g.getTerrain().cells().size());end(g);assertTrue(g.getTerrain().cells().isEmpty());
        move(g,"h8","g8");end(g);assertEquals(1,g.getPieceState(pos("d4")).cooldown());
        move(g,"b1","a1");end(g);move(g,"g8","h8");end(g);assertEquals(0,g.getPieceState(pos("d4")).cooldown());
    }
    private Game hookSetup(String mudSquare,String king,String bolotnik) {
        var b=board(king,"h8");put(b,Knight.class,WHITE,bolotnik);put(b,Rook.class,WHITE,"d2");put(b,Pawn.class,WHITE,"a2");put(b,Pawn.class,BLACK,"d5");
        Game g=create(b,Map.of(pos(bolotnik),HeroType.BOLOTNIK_KNIGHT,pos("d2"),HeroType.PUDGE_ROOK),HeroRules.prototype());
        mud(g,bolotnik,mudSquare);move(g,"a2","a3");end(g);move(g,"h8","g8");end(g);return g;
    }
    @Test public void hookLandingTriggersMudAndRetainsTheMandatoryMoveBudget() throws Exception {
        Game g=hookSetup("d3","a1","f2");assertEquals(Success.SUCCESS,cast(g,"d2","d5",Ability.HOOK));
        assertEquals(HeroType.BOLOTNIK_KNIGHT,g.getPieceState(pos("d3")).hero());assertEquals(2,g.getEconomy().whiteHalves());
        assertFalse(g.getTurnState().moveUsed());assertTrue(g.getTurnState().abilityUsed());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void hookCannotPullAnEnemyThroughIntermediateMud() {
        Game g=hookSetup("d4","a1","e2");reject(g,AbilityRejection.MUD_BLOCKED,()->cast(g,"d2","d5",Ability.HOOK));
    }
    @Test public void teleportLandingTriggersMudAndTheCapturedCasterStillSpendsTheAbility() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"b2");put(b,Bishop.class,BLACK,"f6");
        Game g=charged(create(b,Map.of(pos("b2"),HeroType.BOLOTNIK_KNIGHT,pos("f6"),HeroType.LUMESHARD_BISHOP),HeroRules.prototype()),"f6");
        mud(g,"b2","d4");move(g,"a1","b1");end(g);assertEquals(Success.SUCCESS,cast(g,"f6","d4",Ability.TELEPORT));
        assertEquals(HeroType.BOLOTNIK_KNIGHT,g.getPieceState(pos("d4")).hero());assertEquals(6,g.getEconomy().whiteHalves());
        assertTrue(g.getTurnState().abilityUsed());assertFalse(g.getTurnState().moveUsed());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void aTrapReactionThatExposesTheActingKingsCheckIsRejectedAtomically() {
        var b=board("d1","h8");put(b,Knight.class,WHITE,"d4");put(b,Rook.class,WHITE,"f3");put(b,Pawn.class,WHITE,"a2");put(b,Rook.class,BLACK,"d8");put(b,Pawn.class,BLACK,"f6");
        Game g=create(b,Map.of(pos("d4"),HeroType.BOLOTNIK_KNIGHT,pos("f3"),HeroType.PUDGE_ROOK),HeroRules.prototype());
        mud(g,"d4","f4");move(g,"a2","a3");end(g);move(g,"h8","g8");end(g);
        reject(g,MovementRejection.KING_IN_CHECK,()->cast(g,"f3","f6",Ability.HOOK));
    }
    @Test public void steppingOnMudCannotGiveTheActingKingAnIndirectKnightCheck() {
        var b=board("a1","h5");put(b,Knight.class,WHITE,"d4");put(b,Rook.class,BLACK,"f8");Game g=create(b,"d4");
        mud(g,"d4","f4");move(g,"a1","b1");end(g);
        reject(g,MovementRejection.KING_IN_CHECK,()->g.moveFigure(g.getCurrentPlayer(),pos("f8"),pos("f4")));
    }
    @Test public void aLoadedPudgeCountersTheBolotnikThatCapturedIt() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"f2");put(b,Pawn.class,WHITE,"a2");put(b,Rook.class,BLACK,"d8");put(b,Pawn.class,BLACK,"e8");
        var rules=new HeroRules(PudgeRules.sandbox(),LumeshardRules.prototype());
        Game g=create(b,Map.of(pos("f2"),HeroType.BOLOTNIK_KNIGHT,pos("d8"),HeroType.PUDGE_ROOK),rules);
        mud(g,"f2","d4");move(g,"a2","a3");end(g);assertEquals(Success.SUCCESS,cast(g,"d8","e8",Ability.SWALLOW));move(g,"d8","d4");
        assertEquals(Pawn.class,g.getPieceState(pos("d4")).type());assertEquals(BLACK,g.getPieceState(pos("d4")).color());
        assertEquals(10,g.getEconomy().whiteHalves());assertEquals(6,g.getEconomy().blackHalves());assertTrue(g.getTerrain().cells().isEmpty());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void aMarkedEnemyPaysItsBountyToTheTrapOwner() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"d4");put(b,Pawn.class,WHITE,"b2");put(b,Rook.class,BLACK,"f8");
        Game g=create(b,Map.of(pos("d4"),HeroType.BOLOTNIK_KNIGHT,pos("b2"),HeroType.BOUNTY_CROW_PAWN),HeroRules.prototype());
        mud(g,"d4","f4");move(g,"a1","b1");end(g);move(g,"h8","g8");end(g);
        assertEquals(Success.SUCCESS,cast(g,"b2","f8",Ability.BOUNTY));move(g,"b1","a1");end(g);move(g,"f8","f4");
        assertEquals(13,g.getEconomy().whiteHalves());assertTrue(g.getEconomy().marks().isEmpty());assertEquals(1,g.getPieceState(pos("f4")).ultimateCharge());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void killingTheOwnerRemovesItsUntriggeredMud() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"d4");put(b,Rook.class,BLACK,"d8");Game g=create(b,"d4");
        mud(g,"d4","f4");move(g,"a1","b1");end(g);move(g,"d8","d4");
        assertTrue(g.getTerrain().cells().isEmpty());assertEquals(6,g.getEconomy().blackHalves());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void enemyKingCannotEnterMudEvenWhenNoOrdinaryPieceProtectsIt() {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"d4");Game g=create(b,"d4");
        mud(g,"d4","g7");move(g,"a1","b1");end(g);
        reject(g,StatusRejection.KING_IMMUNE,()->g.moveFigure(g.getCurrentPlayer(),pos("h8"),pos("g7")));
        assertFalse(g.getLegalMoves(pos("h8")).getCanMove().contains(pos("g7")));
    }
    @Test public void hostileMudBlocksBothKingAndRookCastlingPaths() {
        for(String cell:List.of("f1","g1","b1")) {
            boolean queenSide=cell.equals("b1");var b=board("e1","g8");put(b,Rook.class,WHITE,queenSide?"a1":"h1");put(b,Pawn.class,WHITE,"a2");put(b,Knight.class,BLACK,"e5");
            Game g=Game.fromMobaPosition(new GamePosition(b,WHITE,new CastlingRights(!queenSide,queenSide,false,false),null,0),Map.of(pos("e5"),HeroType.BOLOTNIK_KNIGHT),HeroRules.prototype(),()->0L);
            move(g,"a2","a3");end(g);mud(g,"e5",cell);move(g,"g8","f8");end(g);
            reject(g,cell.equals("g1")?StatusRejection.KING_IMMUNE:AbilityRejection.MUD_BLOCKED,()->g.moveFigure(g.getCurrentPlayer(),pos("e1"),pos(queenSide?"c1":"g1")));
        }
    }
    private Game fadingCover(boolean skillReady) {
        var b=board("d1","h8");put(b,Knight.class,WHITE,"e2");put(b,Rook.class,BLACK,"d8");
        HeroRules rules=new HeroRules(PudgeRules.prototype(),LumeshardRules.prototype(),game.heroes.bountycrow.BountyCrowRules.prototype(),GoldRules.prototype(),skillReady?BolotnikRules.sandbox():BolotnikRules.prototype());
        Game g=create(b,Map.of(pos("e2"),HeroType.BOLOTNIK_KNIGHT),rules);
        mud(g,"e2","d4");move(g,"e2","g3");end(g);move(g,"h8","g8");end(g);move(g,"g3","h5");end(g);move(g,"g8","h8");end(g);return g;
    }
    @Test public void movesCannotStrandTheKingBehindCoverThatExpiresOnThisTurnEnd() throws Exception {
        Game g=fadingCover(false);reject(g,TurnRejection.NO_TURN_CONTINUATION,()->g.moveFigure(g.getCurrentPlayer(),pos("h5"),pos("f4")));
        move(g,"d1","e1");end(g);assertTrue(g.getTerrain().cells().isEmpty());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void movingBeforeRefreshingExpiringCoverRemainsPossible() throws Exception {
        Game g=fadingCover(true);move(g,"h5","f4");reject(g,MovementRejection.KING_IN_CHECK,()->g.endTurn(g.getCurrentPlayer()));
        mud(g,"f4","d4");end(g);assertEquals(1,g.getTerrain().cells().size());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void aLivingSwallowedBolotnikEmergesAndDetachesFromItsPudge() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"d4");put(b,Rook.class,WHITE,"c4");put(b,Rook.class,BLACK,"f8");
        Game g=create(b,Map.of(pos("d4"),HeroType.BOLOTNIK_KNIGHT,pos("c4"),HeroType.PUDGE_ROOK),new HeroRules(PudgeRules.sandbox(),LumeshardRules.prototype()));
        long id=g.getPieceState(pos("d4")).id();mud(g,"d4","f4");move(g,"a1","b1");end(g);move(g,"h8","g8");end(g);
        assertEquals(Success.SUCCESS,cast(g,"c4","d4",Ability.SWALLOW));move(g,"b1","a1");end(g);move(g,"f8","f4");
        assertEquals(id,g.getPieceState(pos("f4")).id());assertNull(g.getPieceState(pos("c4")).storedPieceId());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void immediateUltimateCaptureAlsoHonorsLoadedPudgesReaction() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,WHITE,"f5");put(b,Rook.class,BLACK,"e5");put(b,Pawn.class,BLACK,"d5");
        Game g=charged(create(b,Map.of(pos("f5"),HeroType.BOLOTNIK_KNIGHT,pos("e5"),HeroType.PUDGE_ROOK),new HeroRules(PudgeRules.sandbox(),LumeshardRules.prototype())),"f5");
        move(g,"a1","b1");end(g);assertEquals(Success.SUCCESS,cast(g,"e5","d5",Ability.SWALLOW));move(g,"h8","g8");end(g);
        ambush(g,"f5");assertEquals(Pawn.class,g.getPieceState(pos("e5")).type());assertEquals(BLACK,g.getPieceState(pos("e5")).color());
        assertEquals(10,g.getEconomy().whiteHalves());assertEquals(6,g.getEconomy().blackHalves());assertTrue(g.getTerrain().cells().isEmpty());GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void blackUltimateAlsoPrioritizesTheCoordinateClosestToA() throws Exception {
        var b=board("a1","h8");put(b,Knight.class,BLACK,"d4");put(b,Pawn.class,WHITE,"c4");put(b,Pawn.class,WHITE,"e4");
        Game g=charged(create(b,Map.of(pos("d4"),HeroType.BOLOTNIK_KNIGHT),HeroRules.prototype()),"d4");
        move(g,"a1","b1");end(g);ambush(g,"d4");assertEquals(BLACK,g.getPieceState(pos("c4")).color());
        assertEquals(Pawn.class,g.getPieceState(pos("e4")).type());assertTrue(g.getTerrain().cells().isEmpty());assertEquals(2,g.getEconomy().blackHalves());GameSaveFile.encode(g.getSnapshot());
    }
}
