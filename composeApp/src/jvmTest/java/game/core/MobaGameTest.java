package game.core;

import game.moves.AbilityRejection;
import game.moves.ItemRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.TurnRejection;
import game.heroes.Ability;
import game.heroes.HeroType;
import game.heroes.pudge.PudgeRules;
import game.history.GameEvent;
import game.moves.MoveResult;
import game.moves.Promotion;
import game.state.CastlingRights;
import game.state.GameMode;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.ItemSlot;
import game.state.PieceState;
import game.state.Termination;

import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static game.core.Positions.pos;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class MobaGameTest {
    private final Field factory = new Field(8);
    private final PudgeRules free = new PudgeRules(0, 0, true, false);

    private Map<Position, Figure> board(String whiteKing, String blackKing) {
        var board = new HashMap<Position, Figure>();
        for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++) {
            Position square = new Position(row, column);
            board.put(square, factory.createFigure(Empty.class, square, NONE));
        }
        put(board, King.class, WHITE, whiteKing); put(board, King.class, BLACK, blackKing);
        return board;
    }
    private void put(Map<Position, Figure> board, Class<? extends Figure> type, FigureColor color, String square) {
        board.put(pos(square), factory.createFigure(type, pos(square), color));
    }
    private Game create(Map<Position, Figure> board, Map<Position, HeroType> heroes) {
        return create(board, heroes, free, () -> 0L);
    }
    private Game create(Map<Position, Figure> board, Map<Position, HeroType> heroes, PudgeRules rules, java.util.function.LongSupplier time) {
        return Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0), heroes, rules, time);
    }
    private Game game() {
        var board = board("a1", "h8");
        put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, BLACK, "e5");
        put(board, Pawn.class, WHITE, "d2");
        return create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
    }
    private MoveResult move(Game game, String from, String to) {
        return game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to));
    }
    private MoveResult hook(Game game, String from, String to) {
        return game.useAbility(game.getCurrentPlayer(), pos(from), pos(to), Ability.HOOK);
    }
    private MoveResult swallow(Game game, String from, String to) {
        return game.useAbility(game.getCurrentPlayer(), pos(from), pos(to), Ability.SWALLOW);
    }
    private void reject(Game game, MoveResult result, java.util.function.Supplier<MoveResult> command) {
        GameSnapshot before = game.getSnapshot();
        assertEquals(result, command.get());
        assertEquals(before, game.getSnapshot());
    }
    private void assertConsistent(Game game) {
        var snapshot = game.getSnapshot();
        for (var entry : snapshot.field().getGameField().entrySet()) {
            var piece = snapshot.pieceAt(entry.getKey()); Figure figure = entry.getValue();
            if (figure instanceof Empty) assertNull(piece);
            else {
                assertNotNull(piece); assertEquals(figure.getClass(), piece.type());
                assertEquals(figure.getColor(), piece.color()); assertEquals(entry.getKey(), piece.position());
            }
        }
        for (PieceState piece : snapshot.pieces().values()) if (piece.storedPieceId() != null) {
            PieceState stored = snapshot.pieces().get(piece.storedPieceId());
            assertNotNull(stored); assertEquals(PieceState.Status.SWALLOWED, stored.status()); assertEquals(piece.color(), stored.color());
        }
    }

    @Test public void onlyNormalMoveIsMandatoryAndPlayerControlsEndTurn() {
        Game game = game();
        reject(game, TurnRejection.MOVE_REQUIRED, () -> game.endTurn(game.getCurrentPlayer()));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2"));
        assertSame(game.getWhitePlayer(), game.getCurrentPlayer()); assertTrue(game.getTurnState().moveUsed());
        assertFalse(game.getTurnState().abilityUsed()); assertTrue(game.getTurnState().canEndTurn());
        reject(game, TurnRejection.MOVE_ALREADY_USED, () -> move(game, "d2", "d3"));
        assertTrue(game.getLegalMoves(pos("d2")).getCanMove().isEmpty());
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer()));
        assertSame(game.getBlackPlayer(), game.getCurrentPlayer());
        assertFalse(game.getTurnState().moveUsed()); assertFalse(game.getTurnState().abilityUsed());
        assertEquals(2, game.getTurnState().number()); assertConsistent(game);
    }
    @Test public void skillThenMoveSamePudgePreservesIds() {
        Game game = game(); long pudge = game.getPieceState(pos("e2")).id(), victim = game.getPieceState(pos("e5")).id();
        var before = game.getSnapshot();
        assertEquals(Success.SUCCESS, hook(game, "e2", "e5"));
        assertEquals(victim, game.getPieceState(pos("e3")).id());
        reject(game, TurnRejection.MOVE_REQUIRED, () -> game.endTurn(game.getCurrentPlayer()));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2"));
        assertEquals(pudge, game.getPieceState(pos("f2")).id());
        assertTrue(game.getTurnState().abilityUsed()); assertTrue(game.getTurnState().moveUsed());
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer()));
        assertEquals(pos("e2"), before.pieces().get(pudge).position());
        assertEquals(pos("e5"), before.pieces().get(victim).position()); assertConsistent(game);
    }
    @Test public void moveThenSkillSamePudgeUsesNewPosition() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, BLACK, "f5");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2"));
        assertTrue(game.getAbilityTargets(pos("f2"), Ability.HOOK).contains(pos("f5")));
        assertEquals(Success.SUCCESS, hook(game, "f2", "f5"));
        assertTrue(game.getFigureByPosition(pos("f3")) instanceof Knight);
        assertSame(game.getWhitePlayer(), game.getCurrentPlayer()); assertConsistent(game);
    }
    @Test public void abilityBudgetIsSharedBetweenHeroesSkillAndUltimate() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Rook.class, WHITE, "g2");
        put(board, Pawn.class, WHITE, "e3"); put(board, Pawn.class, WHITE, "g3");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK, pos("g2"), HeroType.PUDGE_ROOK));
        assertEquals(Success.SUCCESS, hook(game, "e2", "e3"));
        reject(game, TurnRejection.ABILITY_ALREADY_USED, () -> swallow(game, "g2", "g3"));
        reject(game, TurnRejection.ABILITY_ALREADY_USED, () -> hook(game, "g2", "g3"));
        assertTrue(game.getAbilityTargets(pos("g2"), Ability.SWALLOW).isEmpty());
    }
    @Test public void hookWorksInAllEightDirectionsUpToThreeStepsAndStopsAdjacent() {
        for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
            if (dr == 0 && dc == 0) continue;
            var board = board("h1", "h8"); put(board, Rook.class, WHITE, "d4");
            Position target = new Position(3 + dr * 3, 3 + dc * 3), landing = new Position(3 + dr, 3 + dc);
            board.put(target, factory.createFigure(Knight.class, target, BLACK));
            Game game = create(board, Map.of(pos("d4"), HeroType.PUDGE_ROOK));
            long id = game.getPieceState(target).id();
            assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("d4"), target, Ability.HOOK));
            assertEquals(id, game.getPieceState(landing).id()); assertEquals(0, game.getWhitePlayer().getPoints());
            assertTrue(game.getTurnState().abilityUsed()); assertFalse(game.getTurnState().moveUsed()); assertConsistent(game);
        }
    }
    @Test public void adjacentHookIsAcceptedWithoutDuplicatingOrCapturingTarget() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Pawn.class, WHITE, "e3");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        var target = game.getPieceState(pos("e3"));
        assertEquals(Success.SUCCESS, hook(game, "e2", "e3")); assertEquals(target, game.getPieceState(pos("e3")));
        assertEquals(0, game.getWhitePlayer().getPoints()); assertEquals(3, game.getSnapshot().pieces().size() - 1);
        assertTrue(game.getTurnState().abilityUsed()); assertConsistent(game);
    }
    @Test public void ordinaryFiguresAndNullAbilitiesCannotCast() {
        Game game = game();
        reject(game, AbilityRejection.NO_ABILITY, () -> hook(game, "d2", "e5"));
        reject(game, AbilityRejection.INVALID_ABILITY, () -> game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("e5"), null));
        assertTrue(game.getAbilityTargets(pos("d2"), Ability.HOOK).isEmpty());
    }
    @Test public void hookNeverIgnoresBlockersOrPullsKings() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, BLACK, "e5");
        put(board, Pawn.class, WHITE, "e3"); put(board, Knight.class, BLACK, "g3"); put(board, Knight.class, BLACK, "e6");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        reject(game, AbilityRejection.ABILITY_BLOCKED, () -> hook(game, "e2", "e5"));
        reject(game, AbilityRejection.ABILITY_OUT_OF_RANGE, () -> hook(game, "e2", "g3"));
        reject(game, AbilityRejection.ABILITY_OUT_OF_RANGE, () -> hook(game, "e2", "e6"));
        reject(game, AbilityRejection.INVALID_ABILITY_TARGET, () -> hook(game, "e2", "f2"));
        reject(game, StatusRejection.KING_IMMUNE, () -> hook(game, "e2", "h8"));
        reject(game, MovementRejection.OUT_OF_BOUNDS, () -> game.useAbility(game.getCurrentPlayer(), pos("e2"), null, Ability.HOOK));
    }
    @Test public void hookMayOpenIndirectCheckButCannotExposeOwnKing() {
        var board = board("a1", "e8"); put(board, Rook.class, WHITE, "e1"); put(board, Rook.class, WHITE, "d7");
        put(board, Bishop.class, BLACK, "e7");
        Game game = create(board, Map.of(pos("d7"), HeroType.PUDGE_ROOK));
        assertEquals(Success.SUCCESS, hook(game, "d7", "e7")); // Adjacent, remains blocking.
        assertFalse(rules.standard.MoveValidator.isInCheck(game.getGameField().getGameField(), BLACK));
        var opening = board("a1", "e8"); put(opening, Rook.class, WHITE, "e1"); put(opening, Rook.class, WHITE, "b7");
        put(opening, Bishop.class, BLACK, "e7");
        game = create(opening, Map.of(pos("b7"), HeroType.PUDGE_ROOK));
        assertEquals(Success.SUCCESS, hook(game, "b7", "e7"));
        assertTrue(rules.standard.MoveValidator.isInCheck(game.getGameField().getGameField(), BLACK));
        var pinned = board("e1", "h8"); put(pinned, Rook.class, WHITE, "b2"); put(pinned, Pawn.class, WHITE, "e2");
        put(pinned, Rook.class, BLACK, "e8");
        Game protectedGame = create(pinned, Map.of(pos("b2"), HeroType.PUDGE_ROOK));
        reject(protectedGame, MovementRejection.KING_IN_CHECK, () -> hook(protectedGame, "b2", "e2"));
    }
    @Test public void swallowStoresAllyWithoutAwardingCaptureAndDoesNotSatisfyMove() {
        Game game = game(); long pudge = game.getPieceState(pos("e2")).id(), ally = game.getPieceState(pos("d2")).id();
        assertEquals(Success.SUCCESS, swallow(game, "e2", "d2"));
        assertEquals(Long.valueOf(ally), game.getPieceState(pudge).storedPieceId());
        assertEquals(PieceState.Status.SWALLOWED, game.getPieceState(ally).status()); assertNull(game.getPieceState(ally).position());
        assertTrue(game.getFigureByPosition(pos("d2")) instanceof Empty);
        assertTrue(game.getWhitePlayer().getAttackedFigures().isEmpty()); assertEquals(0, game.getWhitePlayer().getPoints());
        reject(game, TurnRejection.MOVE_REQUIRED, () -> game.endTurn(game.getCurrentPlayer()));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2")); assertEquals(pudge, game.getPieceState(pos("f2")).id());
        assertEquals(Long.valueOf(ally), game.getPieceState(pos("f2")).storedPieceId()); assertConsistent(game);
    }
    @Test public void swallowRangeAndFriendlyRestrictionAreCheckedWithoutMutation() {
        Game game = game();
        reject(game, AbilityRejection.ABILITY_OUT_OF_RANGE, () -> swallow(game, "e2", "e5"));
        reject(game, StatusRejection.KING_IMMUNE, () -> swallow(game, "e2", "a1"));
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, BLACK, "e3");
        Game targetGame = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        reject(targetGame, AbilityRejection.INVALID_ABILITY_TARGET, () -> swallow(targetGame, "e2", "e3"));
    }
    @Test public void capturedPudgeReleasesSameAllyAndAllyCapturesAttacker() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, WHITE, "d2");
        put(board, Rook.class, BLACK, "f8");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        long pudge = game.getPieceState(pos("e2")).id(), ally = game.getPieceState(pos("d2")).id(), attacker = game.getPieceState(pos("f8")).id();
        assertEquals(Success.SUCCESS, swallow(game, "e2", "d2"));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2"));
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer()));
        assertEquals(Success.SUCCESS, move(game, "f8", "f2"));
        assertEquals(ally, game.getPieceState(pos("f2")).id()); assertTrue(game.getFigureByPosition(pos("f2")) instanceof Knight);
        assertEquals(PieceState.Status.CAPTURED, game.getPieceState(pudge).status());
        assertEquals(PieceState.Status.CAPTURED, game.getPieceState(attacker).status());
        assertEquals(1, game.getBlackPlayer().getAttackedFigures().size()); assertEquals(1, game.getWhitePlayer().getAttackedFigures().size());
        assertEquals(3, game.getBlackPlayer().getPoints()); assertEquals(3, game.getWhitePlayer().getPoints());
        assertEquals(GameEvent.Kind.COUNTER_CAPTURE, game.getEvents().getLast().kind());
        assertTrue(game.getTurnState().moveUsed()); assertConsistent(game);
    }
    @Test public void unloadedPudgeIsCapturedNormally() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Rook.class, BLACK, "f8");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2")); game.endTurn(game.getCurrentPlayer());
        assertEquals(Success.SUCCESS, move(game, "f8", "f2"));
        assertEquals(BLACK, game.getFigureByPosition(pos("f2")).getColor()); assertTrue(game.getWhitePlayer().getAttackedFigures().isEmpty());
        assertConsistent(game);
    }
    @Test public void threeEmptyItemSlotsAndRejectedItemCommandsDoNotSpendBudgets() {
        Game game = game(); PieceState pudge = game.getPieceState(pos("e2"));
        assertEquals(3, pudge.inventory().size()); assertTrue(pudge.inventory().stream().allMatch(ItemSlot::isEmpty));
        assertThrows(UnsupportedOperationException.class, () -> pudge.inventory().clear());
        reject(game, ItemRejection.EMPTY_ITEM_SLOT, () -> game.useItem(game.getCurrentPlayer(), pudge.id(), 0));
        reject(game, ItemRejection.INVALID_ITEM_SLOT, () -> game.useItem(game.getCurrentPlayer(), pudge.id(), 3));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2"));
        reject(game, ItemRejection.EMPTY_ITEM_SLOT, () -> game.useItem(game.getCurrentPlayer(), pudge.id(), 2));
        assertFalse(game.getTurnState().abilityUsed()); assertConsistent(game);
    }
    @Test public void snapshotsCollectionsAndQueryPreviewsAreImmutableAndPure() {
        Game game = game(); GameSnapshot before = game.getSnapshot();
        assertTrue(game.getAbilityTargets(pos("e2"), Ability.HOOK).contains(pos("e5")));
        game.getLegalMoves(pos("e2")); assertEquals(before, game.getSnapshot());
        assertThrows(UnsupportedOperationException.class, () -> before.pieces().clear());
        assertThrows(UnsupportedOperationException.class, () -> before.events().clear());
        assertEquals(Success.SUCCESS, hook(game, "e2", "e5"));
        assertEquals(WHITE, before.active()); assertTrue(before.events().isEmpty());
        assertTrue(before.field().getFigureByPosition(pos("e5")) instanceof Knight);
    }
    @Test public void clockRunsThroughSkillAndMoveAndSwitchesOnlyOnEnd() {
        AtomicLong time = new AtomicLong();
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Pawn.class, WHITE, "d2");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK), free, time::get); game.startClock(10);
        time.set(1_000_000_000L); assertEquals(Success.SUCCESS, swallow(game, "e2", "d2"));
        assertEquals(WHITE, game.getClockSnapshot().activeColor());
        time.set(2_000_000_000L); assertEquals(Success.SUCCESS, move(game, "e2", "f2"));
        assertEquals(8_000_000_000L, game.getClockSnapshot().whiteNanos());
        time.set(3_000_000_000L); assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer()));
        assertEquals(BLACK, game.getClockSnapshot().activeColor());
        time.set(4_000_000_000L); assertEquals(7_000_000_000L, game.getClockSnapshot().whiteNanos());
        assertEquals(9_000_000_000L, game.getClockSnapshot().blackNanos());
    }
    @Test public void wrongPlayersAndClassicModeCannotUseMobaCommands() {
        Game game = game();
        reject(game, TurnRejection.WRONG_PLAYER, () -> game.useAbility(game.getBlackPlayer(), pos("e2"), pos("e5"), Ability.HOOK));
        reject(game, TurnRejection.WRONG_PLAYER, () -> game.endTurn(new Player(true)));
        Game classic = new Game(8);
        assertEquals(TurnRejection.UNSUPPORTED_ACTION, classic.endTurn(classic.getCurrentPlayer()));
        assertEquals(TurnRejection.UNSUPPORTED_ACTION, classic.useAbility(classic.getCurrentPlayer(), pos("a1"), pos("a2"), Ability.HOOK));
        assertEquals(GameMode.CLASSIC, classic.getMode());
    }

    @Test public void loadedPudgeCannotBeCapturedByKing() {
        var board = board("a1", "g3"); put(board, Rook.class, WHITE, "e2"); put(board, Pawn.class, WHITE, "d2");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        assertEquals(Success.SUCCESS, swallow(game, "e2", "d2"));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2")); game.endTurn(game.getCurrentPlayer());
        assertFalse(game.isInCheck());
        reject(game, MovementRejection.KING_IN_CHECK, () -> move(game, "g3", "f2"));
        assertFalse(game.getLegalMoves(pos("g3")).getCanMove().contains(pos("f2")));
        assertFalse(game.getLegalMoves(pos("g3")).getCanAttack().contains(pos("f2"))); assertConsistent(game);
    }
    @Test public void retaliationIsIncludedInAttackersKingSafetyBeforeCommit() {
        var board = board("a1", "f7"); put(board, Rook.class, WHITE, "e2"); put(board, Rook.class, WHITE, "d2");
        put(board, Knight.class, BLACK, "h3");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        assertEquals(Success.SUCCESS, swallow(game, "e2", "d2"));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2")); game.endTurn(game.getCurrentPlayer());
        assertTrue(game.isInCheck());
        reject(game, MovementRejection.KING_IN_CHECK, () -> move(game, "h3", "f2"));
        assertFalse(game.getLegalMoves(pos("h3")).getCanAttack().contains(pos("f2"))); assertConsistent(game);
    }
    @Test public void twoLoadedPudgesResolveBothCounterCapturesExactlyOnce() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, WHITE, "d2");
        put(board, Rook.class, BLACK, "f8"); put(board, Knight.class, BLACK, "g8");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK, pos("f8"), HeroType.PUDGE_ROOK));
        long whitePassenger = game.getPieceState(pos("d2")).id(), blackPassenger = game.getPieceState(pos("g8")).id();
        assertEquals(Success.SUCCESS, swallow(game, "e2", "d2"));
        assertEquals(Success.SUCCESS, move(game, "e2", "f2")); game.endTurn(game.getCurrentPlayer());
        assertEquals(Success.SUCCESS, swallow(game, "f8", "g8"));
        assertEquals(Success.SUCCESS, move(game, "f8", "f2"));
        assertEquals(blackPassenger, game.getPieceState(pos("f2")).id());
        assertEquals(PieceState.Status.CAPTURED, game.getPieceState(whitePassenger).status());
        assertEquals(2, game.getBlackPlayer().getAttackedFigures().size()); assertEquals(1, game.getWhitePlayer().getAttackedFigures().size());
        assertEquals(2, game.getEvents().stream().filter(event -> event.kind() == GameEvent.Kind.COUNTER_CAPTURE).count());
        assertConsistent(game);
    }
    @Test public void hookCooldownTicksOnlyAtOwnersTurnStart() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, BLACK, "e5");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK), new PudgeRules(2, 0, true, false), () -> 0L);
        assertEquals(Success.SUCCESS, hook(game, "e2", "e5")); assertEquals(2, game.getPieceState(pos("e2")).cooldown());
        move(game, "e2", "f2"); game.endTurn(game.getCurrentPlayer());
        assertEquals(2, game.getPieceState(pos("f2")).cooldown());
        move(game, "h8", "h7"); game.endTurn(game.getCurrentPlayer());
        assertEquals(1, game.getPieceState(pos("f2")).cooldown());
        reject(game, AbilityRejection.ABILITY_COOLDOWN, () -> hook(game, "f2", "e3"));
        move(game, "f2", "f3"); game.endTurn(game.getCurrentPlayer());
        move(game, "h7", "h8"); game.endTurn(game.getCurrentPlayer());
        assertEquals(0, game.getPieceState(pos("f3")).cooldown()); assertEquals(Success.SUCCESS, hook(game, "f3", "e3"));
        assertConsistent(game);
    }
    @Test public void ultimateChargesFromPudgesOwnCapturesAndIsSpentOnlyOnSuccess() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, WHITE, "d5");
        for (String square : List.of("e3", "e4", "e5")) put(board, Pawn.class, BLACK, square);
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK), new PudgeRules(0, 3, true, false), () -> 0L);
        reject(game, AbilityRejection.ULTIMATE_NOT_READY, () -> swallow(game, "e2", "d5"));
        assertEquals(Success.SUCCESS, move(game, "e2", "e3")); assertEquals(1, game.getPieceState(pos("e3")).ultimateCharge());
        game.endTurn(game.getCurrentPlayer()); move(game, "h8", "g8"); game.endTurn(game.getCurrentPlayer());
        move(game, "e3", "e4"); game.endTurn(game.getCurrentPlayer()); move(game, "g8", "h8"); game.endTurn(game.getCurrentPlayer());
        assertEquals(Success.SUCCESS, move(game, "e4", "e5")); assertEquals(3, game.getPieceState(pos("e5")).ultimateCharge());
        assertEquals(Success.SUCCESS, swallow(game, "e5", "d5")); assertEquals(0, game.getPieceState(pos("e5")).ultimateCharge());
        assertEquals(3, game.getWhitePlayer().getPoints()); assertTrue(game.getTurnState().moveUsed()); assertConsistent(game);
    }
    @Test public void swallowCannotRemoveOwnKingsShield() {
        var board = board("e1", "h8"); put(board, Rook.class, WHITE, "d2"); put(board, Knight.class, WHITE, "e2");
        put(board, Rook.class, BLACK, "e8");
        Game game = create(board, Map.of(pos("d2"), HeroType.PUDGE_ROOK));
        reject(game, MovementRejection.KING_IN_CHECK, () -> swallow(game, "d2", "e2"));
        assertConsistent(game);
    }
    @Test public void promotionPreservesPawnIdAndThreeSlots() {
        var board = board("a1", "h8"); put(board, Pawn.class, WHITE, "b7");
        Game game = create(board, Map.of()); long pawn = game.getPieceState(pos("b7")).id();
        reject(game, MovementRejection.PROMOTION_REQUIRED, () -> move(game, "b7", "b8"));
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("b7"), pos("b8"), Promotion.ROOK));
        assertEquals(pawn, game.getPieceState(pos("b8")).id()); assertEquals(Rook.class, game.getPieceState(pawn).type());
        assertEquals(3, game.getPieceState(pawn).inventory().size()); assertConsistent(game);
    }
    @Test public void hookDisplacementMayPutPawnOnBackRankWithoutBreakingClassicValidation() {
        var board = board("h1", "h8"); put(board, Rook.class, WHITE, "c1"); put(board, Pawn.class, WHITE, "c4");
        Game game = create(board, Map.of(pos("c1"), HeroType.PUDGE_ROOK));
        // Three-step vertical pull lands on c2. Horizontal pull by a Pudge on e1 can land a pawn on d1.
        assertEquals(Success.SUCCESS, hook(game, "c1", "c4")); assertConsistent(game);
        var edge = board("h1", "h8"); put(edge, Rook.class, WHITE, "e1"); put(edge, Pawn.class, WHITE, "b1");
        Game edgeGame = create(edge, Map.of(pos("e1"), HeroType.PUDGE_ROOK));
        assertEquals(Success.SUCCESS, hook(edgeGame, "e1", "b1"));
        assertEquals(Pawn.class, edgeGame.getPieceState(pos("d1")).type());
        assertThrows(IllegalArgumentException.class, () -> Field.fromPosition(edgeGame.getGameField().getGameField()));
        assertConsistent(edgeGame);
    }
    @Test public void flagFallingAfterMoveBeforeEndTurnStillLosesTheActivePlayersTime() {
        AtomicLong time = new AtomicLong();
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK), free, time::get); game.startClock(1);
        move(game, "e2", "f2"); time.set(1_000_000_000L);
        assertEquals(TurnRejection.GAME_OVER, game.endTurn(game.getCurrentPlayer()));
        assertEquals(Termination.TIMEOUT, game.getOutcome().reason()); assertEquals(BLACK, game.getOutcome().winner());
        assertFalse(game.getClockSnapshot().running());
    }
    @Test public void hookCanSaveAClassicMatingPositionBeforeMandatoryMove() {
        var board = board("h1", "a8"); put(board, Rook.class, WHITE, "f1");
        put(board, Rook.class, BLACK, "h3"); put(board, Rook.class, BLACK, "g3");
        var position = new GamePosition(board, WHITE, CastlingRights.none(), null, 0);
        assertEquals(Termination.CHECKMATE, Game.fromPosition(position).getOutcome().reason());
        Game game = Game.fromMobaPosition(position, Map.of(pos("f1"), HeroType.PUDGE_ROOK), free, () -> 0L);
        assertFalse(game.getOutcome().isFinished()); assertTrue(game.isInCheck());
        assertTrue(game.getLegalMoves(pos("f1")).getCanMove().isEmpty());
        assertEquals(Success.SUCCESS, hook(game, "f1", "h3")); assertFalse(game.isInCheck());
        assertEquals(Success.SUCCESS, move(game, "f1", "e1"));
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer())); assertConsistent(game);
    }
    @Test public void bothSwallowRadiusConfigurationsAreSupportedExplicitly() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, WHITE, "d3");
        Game four = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        reject(four, AbilityRejection.ABILITY_OUT_OF_RANGE, () -> swallow(four, "e2", "d3"));
        Game eight = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK), new PudgeRules(0, 0, true, true), () -> 0L);
        assertEquals(Success.SUCCESS, swallow(eight, "e2", "d3")); assertConsistent(eight);
    }
    @Test public void loadedPudgeKeepsOnlyOnePassengerAcrossSubsequentTurns() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Knight.class, WHITE, "d2");
        put(board, Pawn.class, WHITE, "g2");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK));
        swallow(game, "e2", "d2"); move(game, "e2", "f2"); game.endTurn(game.getCurrentPlayer());
        move(game, "h8", "h7"); game.endTurn(game.getCurrentPlayer());
        reject(game, AbilityRejection.ALREADY_SWALLOWED, () -> swallow(game, "f2", "g2")); assertConsistent(game);
    }
    @Test public void hookDisplacementOfHomeRookPermanentlyDropsItsCastlingRight() {
        var board = board("e1", "e8"); put(board, Rook.class, WHITE, "a1"); put(board, Rook.class, WHITE, "d1");
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, new CastlingRights(false, true, false, false), null, 0),
                Map.of(pos("d1"), HeroType.PUDGE_ROOK), free, () -> 0L);
        long home = game.getPieceState(pos("a1")).id();
        assertEquals(Success.SUCCESS, hook(game, "d1", "a1"));
        assertEquals(home, game.getPieceState(pos("c1")).id()); assertFalse(game.getCastlingRights().whiteQueenSide());
        assertConsistent(game);
    }
    @Test public void movingEitherEnPassantPawnByHookExpiresTheRight() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "b5"); put(board, Pawn.class, WHITE, "e5");
        put(board, Pawn.class, BLACK, "d5");
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), pos("d6"), 0),
                Map.of(pos("b5"), HeroType.PUDGE_ROOK), free, () -> 0L);
        assertEquals(Success.SUCCESS, hook(game, "b5", "d5")); assertNull(game.getEnPassantTarget());
        assertFalse(game.getLegalMoves(pos("e5")).getCanAttack().contains(pos("d6"))); assertConsistent(game);
    }
    @Test public void aReturnedPawnRetainsItsTypeEvenOnAnEdgeRank() {
        var board = board("e1", "h8"); put(board, Rook.class, WHITE, "a1"); put(board, Pawn.class, WHITE, "a2");
        put(board, Rook.class, BLACK, "c8");
        Game game = create(board, Map.of(pos("a1"), HeroType.PUDGE_ROOK)); long pawn = game.getPieceState(pos("a2")).id();
        swallow(game, "a1", "a2"); move(game, "a1", "b1"); game.endTurn(game.getCurrentPlayer());
        // Capture along c-file after the Pudge moves there on its next turn.
        move(game, "h8", "h7"); game.endTurn(game.getCurrentPlayer());
        move(game, "b1", "c1"); game.endTurn(game.getCurrentPlayer());
        assertEquals(Success.SUCCESS, move(game, "c8", "c1"));
        assertEquals(pawn, game.getPieceState(pos("c1")).id()); assertEquals(Pawn.class, game.getPieceState(pawn).type()); assertConsistent(game);
    }
    @Test public void onlyOneConcurrentNormalMoveIsAcceptedWithinTheSameMobaTurn() throws Exception {
        Game game = game(); Player player = game.getCurrentPlayer();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            var requests = new java.util.ArrayList<java.util.concurrent.Callable<MoveResult>>();
            for (int i = 0; i < 8; i++) requests.add(() -> game.moveFigure(player, pos("e2"), pos("f2")));
            var results = pool.invokeAll(requests); int success = 0;
            for (var result : results) {
                MoveResult value = result.get();
                if (value == Success.SUCCESS) success++; else assertEquals(TurnRejection.MOVE_ALREADY_USED, value);
            }
            assertEquals(1, success); assertEquals(1, game.getMoves().size()); assertSame(player, game.getCurrentPlayer());
        } finally { pool.shutdownNow(); }
    }
    @Test public void castlingPreservesBothIdsAndPudgesPassenger() {
        var board = board("e1", "e8"); put(board, Rook.class, WHITE, "a1"); put(board, Pawn.class, WHITE, "a2");
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, new CastlingRights(false, true, false, false), null, 0),
                Map.of(pos("a1"), HeroType.PUDGE_ROOK), free, () -> 0L);
        long king = game.getPieceState(pos("e1")).id(), pudge = game.getPieceState(pos("a1")).id(), ally = game.getPieceState(pos("a2")).id();
        assertEquals(Success.SUCCESS, swallow(game, "a1", "a2"));
        assertEquals(Success.SUCCESS, move(game, "e1", "c1"));
        assertEquals(king, game.getPieceState(pos("c1")).id()); assertEquals(pudge, game.getPieceState(pos("d1")).id());
        assertEquals(Long.valueOf(ally), game.getPieceState(pudge).storedPieceId());
        assertFalse(game.getCastlingRights().whiteQueenSide()); assertTrue(game.getTurnState().moveUsed()); assertConsistent(game);
    }
    @Test public void classicModeAlsoHasStableIdsAndCorrectActingTurnEvents() {
        Game game = new Game(8); long pawn = game.getPieceState(pos("e2")).id();
        assertEquals(Success.SUCCESS, move(game, "e2", "e4"));
        assertEquals(pawn, game.getPieceState(pos("e4")).id());
        assertEquals(1, game.getEvents().getFirst().turn()); assertEquals(2, game.getTurnState().number());
        assertConsistent(game);
    }
    @Test public void clockCannotBeConfiguredAfterAnAbilityEvenWithoutNormalMoves() {
        Game game = game(); assertEquals(Success.SUCCESS, swallow(game, "e2", "d2"));
        assertTrue(game.getMoves().isEmpty());
        assertThrows(IllegalStateException.class, () -> game.startClock(10));
    }
    @Test public void abilityBeforeMoveCannotLeaveTheTurnWithoutAnyLegalNormalMove() {
        var board = board("a1", "e8");
        put(board, Rook.class, WHITE, "a2"); put(board, Rook.class, BLACK, "a5");
        put(board, Rook.class, BLACK, "b5"); put(board, Queen.class, BLACK, "c2");
        Game game = Game.fromMobaPosition(new GamePosition(board, BLACK, CastlingRights.none(), null, 0),
                Map.of(pos("a2"), HeroType.PUDGE_ROOK, pos("a5"), HeroType.PUDGE_ROOK), free, () -> 0L);
        assertEquals(Success.SUCCESS, swallow(game, "a5", "b5"));
        assertEquals(Success.SUCCESS, move(game, "e8", "f8"));
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer()));
        assertFalse(game.getLegalMoves(pos("a2")).getCanMove().isEmpty());
        reject(game, TurnRejection.NO_MOVE_AFTER_ABILITY, () -> hook(game, "a2", "a5"));
        assertFalse(game.getAbilityTargets(pos("a2"), Ability.HOOK).contains(pos("a5")));
        assertFalse(game.getTurnState().abilityUsed()); assertConsistent(game);
    }
    @Test public void nestedAlliedPudgeIsReleasedWithItsOwnPassengerIntact() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Rook.class, WHITE, "g2");
        put(board, Pawn.class, WHITE, "g3"); put(board, Rook.class, BLACK, "f8");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK, pos("g2"), HeroType.PUDGE_ROOK));
        long inner = game.getPieceState(pos("g2")).id(), pawn = game.getPieceState(pos("g3")).id();
        assertEquals(Success.SUCCESS, swallow(game, "g2", "g3"));
        move(game, "e2", "f2"); game.endTurn(game.getCurrentPlayer()); move(game, "h8", "h7"); game.endTurn(game.getCurrentPlayer());
        assertEquals(Success.SUCCESS, swallow(game, "f2", "g2"));
        move(game, "f2", "f3"); game.endTurn(game.getCurrentPlayer());
        assertEquals(Success.SUCCESS, move(game, "f8", "f3"));
        assertEquals(inner, game.getPieceState(pos("f3")).id()); assertEquals(Long.valueOf(pawn), game.getPieceState(inner).storedPieceId());
        assertEquals(PieceState.Status.SWALLOWED, game.getPieceState(pawn).status()); assertConsistent(game);
    }
}
