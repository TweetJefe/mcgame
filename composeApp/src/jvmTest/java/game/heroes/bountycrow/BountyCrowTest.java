package game.heroes.bountycrow;

import game.core.Game;
import game.economy.*;
import game.heroes.*;
import game.heroes.pudge.PudgeRules;
import game.moves.*;
import game.persistence.GameSaveFile;
import game.scenarios.BountyCrowScenario;
import game.state.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class BountyCrowTest {
    private final Field factory = new Field(8);
    private Map<Position, Figure> board(String whiteKing, String blackKing) {
        var board = new HashMap<Position, Figure>();
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Position p = new Position(r, c); board.put(p, factory.createFigure(Empty.class, p, NONE));
        }
        put(board, King.class, WHITE, whiteKing); put(board, King.class, BLACK, blackKing); return board;
    }
    private void put(Map<Position, Figure> board, Class<? extends Figure> type, FigureColor side, String square) {
        board.put(pos(square), factory.createFigure(type, pos(square), side));
    }
    private Game create(Map<Position, Figure> board, Map<Position, HeroType> heroes, HeroRules rules, FigureColor active) {
        return Game.fromMobaPosition(new GamePosition(board, active, CastlingRights.none(), null, 0), heroes, rules, () -> 0L);
    }
    private Game create(Map<Position, Figure> board, String crow) {
        return create(board, Map.of(pos(crow), HeroType.BOUNTY_CROW_PAWN), HeroRules.prototype(), WHITE);
    }
    private Game scenario(BountyCrowScenario scenario) { return scenario.createGame(() -> 0L); }
    private void play(Game g, String from, String to) { assertEquals(MoveResult.SUCCESS, g.moveFigure(g.getCurrentPlayer(), pos(from), pos(to))); }
    private void end(Game g) { assertEquals(MoveResult.SUCCESS, g.endTurn(g.getCurrentPlayer())); }
    private MoveResult cast(Game g, String from, String to, Ability a) { return g.useAbility(g.getCurrentPlayer(), pos(from), to == null ? null : pos(to), a); }
    private void mark(Game g, String from, String to) { assertEquals(MoveResult.SUCCESS, cast(g, from, to, Ability.BOUNTY)); }
    private long gold(Game g, FigureColor side) { return g.getEconomy().balanceHalves(side); }
    private void reject(Game g, MoveResult result, java.util.function.Supplier<MoveResult> action) {
        var before = g.getSnapshot(); assertEquals(result, action.get()); assertEquals(before, g.getSnapshot());
    }
    @Test public void initialAssignmentAndWallets() {
        Game g = Game.mobaPrototype(HeroRules.prototype(), 600);
        assertEquals(HeroType.BOUNTY_CROW_PAWN, g.getPieceState(pos("b2")).hero());
        assertEquals(HeroType.BOUNTY_CROW_PAWN, g.getPieceState(pos("b7")).hero());
        assertEquals(Pawn.class, g.getPieceState(pos("b2")).type()); assertEquals(0, gold(g, WHITE)); assertEquals(0, gold(g, BLACK));
    }
    @Test public void bountyIsGlobalIgnoresBlockersAndPreviewsDoNotChangeState() {
        Game g = Game.fromSnapshot(Game.mobaPrototype(HeroRules.prototype(), 600).getSnapshot(), () -> 0L); var before = g.getSnapshot();
        assertTrue(g.getAbilityTargets(pos("b2"), Ability.BOUNTY).contains(pos("h8")));
        assertTrue(g.getAbilityTargets(pos("b2"), Ability.BOUNTY).contains(pos("a7")));
        assertEquals(before, g.getSnapshot()); mark(g, "b2", "h8");
        assertEquals(3, g.getPieceState(pos("b2")).cooldown()); assertEquals(1, g.getEconomy().marks().size());
        assertEquals(0, gold(g, WHITE)); assertFalse(g.getTurnState().moveUsed()); assertTrue(g.getTurnState().abilityUsed());
        reject(g, MoveResult.MOVE_REQUIRED, () -> g.endTurn(g.getCurrentPlayer()));
    }
    @Test public void invalidTargetsAndWrongAbilitiesPreserveEverything() {
        Game g = scenario(BountyCrowScenario.BOUNTY);
        reject(g, MoveResult.INVALID_ABILITY_TARGET, () -> cast(g, "b2", "e1", Ability.BOUNTY));
        reject(g, MoveResult.INVALID_ABILITY_TARGET, () -> cast(g, "b2", "e3", Ability.BOUNTY));
        reject(g, MoveResult.KING_IMMUNE, () -> cast(g, "b2", "h8", Ability.BOUNTY));
        reject(g, MoveResult.NO_ABILITY, () -> cast(g, "b2", "e4", Ability.HOOK));
        reject(g, MoveResult.ULTIMATE_NOT_READY, () -> cast(g, "b2", null, Ability.STEAL));
    }
    @Test public void otherAllyCollectsBaseAndBountyOnce() {
        Game g = scenario(BountyCrowScenario.BOUNTY); mark(g, "b2", "e4"); play(g, "e1", "e4");
        assertEquals(9, gold(g, WHITE)); assertTrue(g.getEconomy().marks().isEmpty());
        assertEquals(1, g.getEconomy().history().size()); assertEquals(3, g.getWhitePlayer().getPoints());
        assertEquals(0, g.getPieceState(pos("b2")).ultimateCharge());
        reject(g, MoveResult.MOVE_ALREADY_USED, () -> g.moveFigure(g.getCurrentPlayer(), pos("e4"), pos("e5")));
    }
    @Test public void crowCollectsBothBonusesAndPawnChargesUltimate() {
        Game g = scenario(BountyCrowScenario.PASSIVE); mark(g, "d4", "e5"); play(g, "d4", "e5");
        assertEquals(6, gold(g, WHITE)); assertEquals(1, g.getPieceState(pos("e5")).ultimateCharge());
        var entry = g.getEconomy().history().getFirst();
        assertEquals(2, entry.baseHalves()); assertEquals(3, entry.bountyHalves()); assertEquals(1, entry.passiveHalves());
    }
    @Test public void passiveDoesNotSpendAbilityAndDoesNotRequireMark() {
        Game g = scenario(BountyCrowScenario.PASSIVE); play(g, "d4", "e5");
        assertEquals(3, gold(g, WHITE)); assertFalse(g.getTurnState().abilityUsed());
    }
    @Test public void bountyLastsThroughTwoFollowingOwnerTurnsAndExpiresExactlyOnTheirSecondEnd() throws Exception {
        Game g = scenario(BountyCrowScenario.BOUNTY); mark(g, "b2", "e4");
        assertEquals(5, g.getEconomy().marks().getFirst().expiresAfterTurn());
        play(g, "e1", "d1"); end(g); assertEquals(1, g.getEconomy().marks().size());
        play(g, "h8", "g8"); end(g); play(g, "a1", "b1"); end(g);
        assertEquals(1, g.getEconomy().marks().size()); play(g, "g8", "h8"); end(g);
        g = Game.fromSnapshot(GameSaveFile.decode(GameSaveFile.encode(g.getSnapshot())), () -> 0L);
        assertEquals(1, g.getEconomy().marks().size()); play(g, "b1", "a1");
        assertEquals(1, g.getEconomy().marks().size()); end(g); assertTrue(g.getEconomy().marks().isEmpty());
    }
    @Test public void ordinaryCapturePricesAreSeparateFromClassicPoints() {
        var b = board("a1", "h8"); put(b, Rook.class, WHITE, "e1"); put(b, Queen.class, BLACK, "e4"); put(b, Pawn.class, WHITE, "b2");
        Game g = create(b, "b2"); play(g, "e1", "e4"); assertEquals(12, gold(g, WHITE));
        assertEquals(9, g.getWhitePlayer().getPoints());
    }
    @Test public void captureAfterExpiryPaysOnlyTheOrdinaryReward() throws Exception {
        Game g = scenario(BountyCrowScenario.BOUNTY); mark(g, "b2", "e4");
        play(g, "e1", "d1"); end(g); play(g, "h8", "g8"); end(g);
        play(g, "d1", "e1"); end(g); play(g, "g8", "h8"); end(g);
        play(g, "a1", "b1"); end(g); play(g, "h8", "g8"); end(g);
        assertTrue(g.getEconomy().marks().isEmpty()); play(g, "e1", "e4");
        assertEquals(6, gold(g, WHITE)); GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void markSurvivesTheCastersDeathUntilItsExpiry() throws Exception {
        var b = board("a1", "h8"); put(b, Pawn.class, WHITE, "b2"); put(b, Rook.class, BLACK, "b8"); put(b, Pawn.class, BLACK, "g6");
        Game g = create(b, "b2"); long source = g.getPieceState(pos("b2")).id(), target = g.getPieceState(pos("g6")).id();
        mark(g, "b2", "g6"); play(g, "a1", "a2"); end(g); play(g, "b8", "b2"); end(g);
        assertEquals(PieceState.Status.CAPTURED, g.getPieceState(source).status());
        assertNotNull(g.getEconomy().mark(target)); assertEquals(4, gold(g, BLACK)); GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void aHeroPawnCostsTwoGold() {
        var b = board("a1", "h8"); put(b, Rook.class, WHITE, "e1"); put(b, Pawn.class, BLACK, "e4");
        Game g = create(b, Map.of(pos("e4"), HeroType.BOUNTY_CROW_PAWN), HeroRules.prototype(), WHITE);
        play(g, "e1", "e4"); assertEquals(4, gold(g, WHITE));
    }
    @Test public void ultimateTransfersTwoGoldAndNeedsOrdinaryMovementToFinish() {
        Game g = scenario(BountyCrowScenario.STEAL); assertEquals(6, gold(g, WHITE)); assertEquals(6, gold(g, BLACK));
        assertEquals(2, g.getPieceState(pos("f6")).ultimateCharge());
        assertEquals(MoveResult.SUCCESS, cast(g, "f6", null, Ability.STEAL));
        assertEquals(10, gold(g, WHITE)); assertEquals(2, gold(g, BLACK)); assertEquals(0, g.getPieceState(pos("f6")).ultimateCharge());
        reject(g, MoveResult.MOVE_REQUIRED, () -> g.endTurn(g.getCurrentPlayer()));
        reject(g, MoveResult.ABILITY_ALREADY_USED, () -> cast(g, "f6", "h3", Ability.BOUNTY));
        play(g, "f6", "f7"); end(g);
    }
    @Test public void theftCanFollowOrdinaryMove() {
        Game g = scenario(BountyCrowScenario.STEAL); play(g, "f6", "f7");
        assertEquals(MoveResult.SUCCESS, cast(g, "f7", null, Ability.STEAL)); end(g); assertEquals(10, gold(g, WHITE));
    }
    @Test public void theftTakesOnlyTheAvailableFractionalBalance() {
        var b = board("a1", "h8"); put(b, Pawn.class, WHITE, "d4"); put(b, Pawn.class, BLACK, "e5"); put(b, Pawn.class, BLACK, "f6");
        put(b, Pawn.class, BLACK, "g4"); put(b, Pawn.class, WHITE, "h3");
        Game g = create(b, Map.of(pos("d4"), HeroType.BOUNTY_CROW_PAWN, pos("g4"), HeroType.BOUNTY_CROW_PAWN), HeroRules.prototype(), WHITE);
        play(g, "d4", "e5"); end(g); play(g, "g4", "h3"); end(g); play(g, "e5", "f6"); end(g); play(g, "h8", "g8"); end(g);
        assertEquals(3, gold(g, BLACK)); assertEquals(MoveResult.SUCCESS, cast(g, "f6", null, Ability.STEAL));
        assertEquals(0, gold(g, BLACK)); assertEquals(9, gold(g, WHITE));
    }
    @Test public void emptyWalletRejectsTheftWithoutConsumingCharge() {
        var b = board("a1", "h8"); put(b, Pawn.class, WHITE, "d4"); put(b, Pawn.class, BLACK, "e5"); put(b, Pawn.class, BLACK, "f6");
        Game g = create(b, "d4"); play(g, "d4", "e5"); end(g); play(g, "h8", "g8"); end(g); play(g, "e5", "f6"); end(g); play(g, "g8", "h8"); end(g);
        reject(g, MoveResult.NO_GOLD_TO_STEAL, () -> cast(g, "f6", null, Ability.STEAL)); assertEquals(2, g.getPieceState(pos("f6")).ultimateCharge());
    }
    @Test public void cooldownCountsOwnTurnsAndDoesNotBlockUltimate() {
        Game g = scenario(BountyCrowScenario.STEAL); mark(g, "f6", "h3"); play(g, "a1", "b1"); end(g);
        play(g, "g8", "h8"); end(g); assertEquals(2, g.getPieceState(pos("f6")).cooldown());
        reject(g, MoveResult.ABILITY_COOLDOWN, () -> cast(g, "f6", "h3", Ability.BOUNTY));
        assertEquals(MoveResult.SUCCESS, cast(g, "f6", null, Ability.STEAL));
    }
    @Test public void purelyFinancialAbilitiesCannotLeaveOwnKingInCheck() {
        var b = board("a1", "h8"); put(b, Pawn.class, WHITE, "b2"); put(b, Rook.class, BLACK, "a8"); put(b, Pawn.class, BLACK, "g7");
        Game g = create(b, "b2"); assertTrue(g.isInCheck());
        reject(g, MoveResult.KING_IN_CHECK, () -> cast(g, "b2", "g7", Ability.BOUNTY));
    }
    @Test public void promotionKeepsIdentitySkillsAndPassive() throws Exception {
        var b = board("a1", "h6"); put(b, Pawn.class, WHITE, "b7"); put(b, Pawn.class, BLACK, "c7");
        Game g = create(b, "b7"); long id = g.getPieceState(pos("b7")).id();
        assertEquals(MoveResult.SUCCESS, g.moveFigure(g.getCurrentPlayer(), pos("b7"), pos("b8"), Promotion.QUEEN));
        assertEquals(id, g.getPieceState(pos("b8")).id()); assertEquals(HeroType.BOUNTY_CROW_PAWN, g.getPieceState(id).hero());
        g = Game.fromSnapshot(GameSaveFile.decode(GameSaveFile.encode(g.getSnapshot())), () -> 0L);
        mark(g, "b8", "c7"); end(g); play(g, "h6", "h5"); end(g); play(g, "b8", "c7");
        assertEquals(6, gold(g, WHITE)); assertEquals(1, g.getPieceState(id).ultimateCharge());
    }
    @Test public void promotionCaptureStillTriggersPassiveAndCharge() {
        var b = board("a1", "h6"); put(b, Pawn.class, WHITE, "b7"); put(b, Rook.class, BLACK, "c8");
        Game g = create(b, "b7");
        assertEquals(MoveResult.SUCCESS, g.moveFigure(g.getCurrentPlayer(), pos("b7"), pos("c8"), Promotion.KNIGHT));
        assertEquals(11, gold(g, WHITE)); assertEquals(1, g.getPieceState(pos("c8")).ultimateCharge());
    }
    @Test public void enPassantIsAPaidPawnCaptureWithPassive() {
        var b = board("a1", "h8"); put(b, Pawn.class, WHITE, "e5"); put(b, Pawn.class, BLACK, "d7");
        Game g = create(b, Map.of(pos("e5"), HeroType.BOUNTY_CROW_PAWN), HeroRules.prototype(), BLACK);
        play(g, "d7", "d5"); end(g); mark(g, "e5", "d5"); play(g, "e5", "d6");
        assertEquals(6, gold(g, WHITE)); assertTrue(g.getEconomy().marks().isEmpty());
    }
    @Test public void crowReleasedFromPudgeEarnsPassiveAndChargeForCounterCapture() throws Exception {
        var b = board("a1", "h8"); put(b, Rook.class, WHITE, "d4"); put(b, Pawn.class, WHITE, "e4"); put(b, Rook.class, BLACK, "d8");
        HeroRules rules = new HeroRules(PudgeRules.sandbox(), game.heroes.lumeshard.LumeshardRules.prototype());
        Game g = create(b, Map.of(pos("d4"), HeroType.PUDGE_ROOK, pos("e4"), HeroType.BOUNTY_CROW_PAWN), rules, WHITE);
        assertEquals(MoveResult.SUCCESS, cast(g, "d4", "e4", Ability.SWALLOW)); play(g, "a1", "b1"); end(g); play(g, "d8", "d4");
        assertEquals(HeroType.BOUNTY_CROW_PAWN, g.getPieceState(pos("d4")).hero());
        assertEquals(11, gold(g, WHITE)); assertEquals(10, gold(g, BLACK)); assertEquals(1, g.getPieceState(pos("d4")).ultimateCharge());
        GameSaveFile.encode(g.getSnapshot());
    }
    @Test public void bountyFollowsSwallowedTargetAndStillExpires() throws Exception {
        var b = board("a1", "h8"); put(b, Pawn.class, WHITE, "b2"); put(b, Pawn.class, BLACK, "e4"); put(b, Rook.class, BLACK, "e5");
        Game g = create(b, Map.of(pos("b2"), HeroType.BOUNTY_CROW_PAWN, pos("e5"), HeroType.PUDGE_ROOK),
                new HeroRules(PudgeRules.sandbox(), game.heroes.lumeshard.LumeshardRules.prototype()), WHITE);
        long target = g.getPieceState(pos("e4")).id(); mark(g, "b2", "e4"); play(g, "a1", "b1"); end(g);
        assertEquals(MoveResult.SUCCESS, cast(g, "e5", "e4", Ability.SWALLOW)); play(g, "h8", "g8"); end(g);
        assertNotNull(g.getEconomy().mark(target)); assertEquals(PieceState.Status.SWALLOWED, g.getPieceState(target).status());
        g = Game.fromSnapshot(GameSaveFile.decode(GameSaveFile.encode(g.getSnapshot())), () -> 0L);
        play(g, "b1", "a1"); end(g); play(g, "g8", "h8"); end(g); play(g, "a1", "b1"); end(g);
        assertNull(g.getEconomy().mark(target)); assertEquals(0, gold(g, BLACK));
    }
    @Test public void reapplyingRefreshesOneMarkWithoutStackingRewards() {
        var b = board("a1", "h8"); put(b, Pawn.class, WHITE, "b2"); put(b, Knight.class, BLACK, "e4");
        var rules = new HeroRules(PudgeRules.prototype(), game.heroes.lumeshard.LumeshardRules.prototype(), new BountyCrowRules(0, 2, 2), GoldRules.prototype());
        Game g = create(b, Map.of(pos("b2"), HeroType.BOUNTY_CROW_PAWN), rules, WHITE);
        mark(g, "b2", "e4"); play(g, "a1", "b1"); end(g); play(g, "h8", "g8"); end(g); mark(g, "b2", "e4");
        assertEquals(1, g.getEconomy().marks().size()); assertEquals(7, g.getEconomy().marks().getFirst().expiresAfterTurn());
    }
    @Test public void classicModeHasNoWalletAndUsesOriginalChessPoints() {
        var b = board("a1", "h8"); put(b, Rook.class, WHITE, "e1"); put(b, Queen.class, BLACK, "e4");
        Game g = Game.fromPosition(new GamePosition(b, WHITE, CastlingRights.none(), null, 0), () -> 0L);
        play(g, "e1", "e4"); assertFalse(g.getEconomy().enabled()); assertEquals(0, gold(g, WHITE)); assertEquals(9, g.getWhitePlayer().getPoints());
    }
}
