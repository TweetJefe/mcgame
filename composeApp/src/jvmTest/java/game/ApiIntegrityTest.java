package game;

import model.*;
import model.figures.*;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static game.Positions.pos;
import static game.Positions.play;
import static model.FigureColor.*;
import static org.junit.Assert.*;

/** Exercises production API without reflection-based fixture editing. */
public class ApiIntegrityTest {
    private final Field factory = new Field(8);

    private Map<Position, Figure> board(String whiteKing, String blackKing) {
        var board = new HashMap<Position, Figure>();
        for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++) {
            Position square = new Position(row, column);
            board.put(square, factory.createFigure(Empty.class, square, NONE));
        }
        put(board, King.class, WHITE, whiteKing);
        put(board, King.class, BLACK, blackKing);
        return board;
    }
    private void put(Map<Position, Figure> board, Class<? extends Figure> type, FigureColor color, String square) {
        board.put(pos(square), factory.createFigure(type, pos(square), color));
    }
    private Game load(Map<Position, Figure> board, FigureColor turn, CastlingRights rights, Position ep, int count) {
        return Game.fromPosition(new GamePosition(board, turn, rights, ep, count));
    }
    private void invalid(Map<Position, Figure> board) {
        assertThrows(IllegalArgumentException.class, () -> load(board, WHITE, CastlingRights.none(), null, 0));
    }

    @Test public void onlyStandardBoardIsEnabled() {
        for (int size : new int[]{-1, 0, 1, 7, 9, 10, 16, Integer.MAX_VALUE}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new Game(size));
            assertTrue(error.getMessage().contains("8x8"));
        }
        assertEquals(64, factory.getGameField().size());
        for (var entry : factory.getGameField().entrySet()) {
            assertEquals(entry.getKey(), entry.getValue().getPosition());
            assertNotNull(entry.getValue());
        }
    }
    @Test public void coordinatesAndPlacementHaveNoPublicMutationApi() throws Exception {
        assertTrue(Modifier.isFinal(Position.class.getModifiers()));
        for (String member : new String[]{"row", "column"})
            assertTrue(Modifier.isFinal(Position.class.getDeclaredField(member).getModifiers()));
        for (String member : new String[]{"position", "color", "field", "name", "points"})
            assertTrue(Modifier.isFinal(Figure.class.getDeclaredField(member).getModifiers()));
        assertThrows(NoSuchMethodException.class, () -> Position.class.getMethod("setRow", int.class));
        assertThrows(NoSuchMethodException.class, () -> Position.class.getMethod("setColumn", int.class));
        assertThrows(NoSuchMethodException.class, () -> Figure.class.getMethod("move", Position.class));
        assertThrows(NoSuchMethodException.class, () -> Figure.class.getMethod("setPosition", Position.class));
        assertThrows(NoSuchMethodException.class, () -> Player.class.getMethod("addPoints", int.class));
        Position key = pos("e2"); var map = Map.of(key, "pawn");
        assertEquals("pawn", map.get(pos("e2")));
        assertEquals(pos("f3"), key.addPosition(new Position(1, 1)));
        assertEquals(pos("e2"), key);
    }
    @Test public void boardAndAllHistoryCollectionsRejectExternalWrites() {
        Game game = new Game(8); Player player = game.getWhitePlayer();
        var oldHistory = player.getMadeMoves(); var oldCaptures = player.getAttackedFigures();
        var oldMoves = game.getMoves();
        var board = game.getGameField().getGameField();
        assertThrows(UnsupportedOperationException.class, board::clear);
        assertThrows(UnsupportedOperationException.class, () -> board.remove(pos("e1")));
        assertThrows(UnsupportedOperationException.class, () -> board.put(pos("e2"), board.get(pos("e4"))));
        assertThrows(UnsupportedOperationException.class, () -> board.entrySet().iterator().next().setValue(board.get(pos("e1"))));
        assertThrows(UnsupportedOperationException.class, oldHistory::clear);
        assertThrows(UnsupportedOperationException.class, oldCaptures::clear);
        assertThrows(UnsupportedOperationException.class, oldMoves::clear);
        play(game, "e2e4", "d7d5", "e4d5");
        assertTrue(oldHistory.isEmpty()); assertTrue(oldCaptures.isEmpty()); assertTrue(oldMoves.isEmpty());
        assertEquals(2, player.getMadeMoves().size()); assertEquals(1, player.getAttackedFigures().size());
        assertEquals(1L, player.getPoints());
        assertThrows(UnsupportedOperationException.class, () -> player.getAttackedFigures().removeFirst());
    }
    @Test public void retainedBoardAndPieceSnapshotsSurviveOrdinaryMovesAndCapture() {
        Game game = new Game(8);
        Field before = game.getGameField(); Figure pawn = before.getFigureByPosition(pos("e2"));
        var snapshot = game.getPosition();
        play(game, "e2e4", "d7d5", "e4d5");
        assertNotSame(before, game.getGameField());
        assertEquals(pos("e2"), pawn.getPosition());
        assertSame(pawn, before.getFigureByPosition(pos("e2")));
        assertTrue(before.getFigureByPosition(pos("e4")) instanceof Empty);
        assertTrue(before.getFigureByPosition(pos("d7")) instanceof Pawn);
        assertEquals(WHITE, snapshot.sideToMove()); assertEquals(CastlingRights.initial(), snapshot.castlingRights());
        assertSame(pawn, snapshot.board().get(pos("e2")));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.board().clear());
        assertTrue(game.getFigureByPosition(pos("d5")) instanceof Pawn);
    }
    @Test public void pseudoMoveCachesAreSafeAndBoundToTheirBoardSnapshot() {
        Game game = new Game(8); Field field = game.getGameField();
        Figure pawn = field.getFigureByPosition(pos("e2"));
        assertEquals(List.of(), pawn.getCanMove()); assertEquals(List.of(), pawn.getCanAttack());
        Figure empty = field.getFigureByPosition(pos("e4")); field.compute(empty);
        assertTrue(empty.getCanMove().isEmpty()); assertTrue(empty.getCanAttack().isEmpty());
        field.compute(pawn); var moves = pawn.getCanMove();
        assertEquals(List.of(pos("e3"), pos("e4")), moves);
        assertThrows(UnsupportedOperationException.class, () -> moves.add(pos("e5")));
        play(game, "e2e4"); field.compute(pawn);
        assertEquals(moves, pawn.getCanMove());
        assertThrows(IllegalArgumentException.class, () -> game.getGameField().compute(pawn));
        assertTrue(game.getLegalMoves(pos("e4")).getCanMove().isEmpty()); // Black's turn.
    }
    @Test public void successfulLoadCopiesInputAndRebindsPieces() {
        var source = new HashMap<>(factory.getGameField());
        var input = new GamePosition(source, WHITE, CastlingRights.initial(), null, 0);
        source.clear();
        Game game = Game.fromPosition(input);
        assertEquals(64, game.getGameField().getGameField().size());
        assertNotSame(factory.getFigureByPosition(pos("e2")), game.getFigureByPosition(pos("e2")));
        assertTrue(game.getMoves().isEmpty()); assertEquals(0L, game.getWhitePlayer().getPoints());
        assertFalse(game.getClockSnapshot().isEnabled()); assertEquals(1, game.getRepetitionCount());
        assertEquals(MoveResult.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("e2"), pos("e4")));
        assertTrue(input.board().get(pos("e2")) instanceof Pawn);
    }
    @Test public void nullBoardCellsAndIncompleteMapsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Game.fromPosition(null));
        assertThrows(IllegalArgumentException.class, () -> Field.fromPosition(null));
        assertThrows(IllegalArgumentException.class, () -> new GamePosition(null, WHITE, CastlingRights.none(), null, 0));
        for (int kind = 0; kind < 4; kind++) {
            var board = new HashMap<>(factory.getGameField());
            if (kind == 0) board.remove(pos("a2"));
            if (kind == 1) board.put(pos("a2"), null);
            if (kind == 2) board.put(null, board.remove(pos("a2")));
            if (kind == 3) board.put(new Position(8, 0), board.remove(pos("a2")));
            invalid(board);
        }
    }
    @Test public void mismatchedCoordinatesAndUnsupportedPiecesAreRejected() {
        var board = board("e1", "e8"); board.put(pos("a2"), factory.createFigure(Pawn.class, pos("b2"), WHITE));
        invalid(board);
        board.put(pos("a2"), new UnknownFigure(pos("a2"), factory)); invalid(board);
        assertThrows(IllegalArgumentException.class, () -> factory.createFigure(UnknownFigure.class, pos("a2"), WHITE));
        assertThrows(IllegalArgumentException.class, () -> factory.createFigure(null, pos("a2"), WHITE));
        assertThrows(IllegalArgumentException.class, () -> factory.createFigure(Pawn.class, null, WHITE));
        assertThrows(IllegalArgumentException.class, () -> factory.createFigure(Pawn.class, pos("a9"), WHITE));
        assertThrows(IllegalArgumentException.class, () -> factory.createFigure(Empty.class, pos("a2"), WHITE));
        assertThrows(IllegalArgumentException.class, () -> factory.createFigure(Pawn.class, pos("a2"), NONE));
    }
    @Test public void invalidPieceColorsAreRejectedEvenWithDirectConstructors() {
        var board = board("e1", "e8"); board.put(pos("a2"), new Pawn(pos("a2"), NONE, factory)); invalid(board);
        board.put(pos("a2"), new Empty(pos("a2"), BLACK, factory)); invalid(board);
        board.put(pos("a2"), new Pawn(WHITE)); invalid(board); // Placement prototype has no coordinate.
    }
    @Test public void bothKingsAndTheirSeparationAreRequired() {
        var board = board("e1", "e8"); put(board, Empty.class, NONE, "e1"); invalid(board);
        board = board("e1", "e8"); put(board, King.class, WHITE, "a1"); invalid(board);
        invalid(board("e1", "e2"));
    }
    @Test public void impossibleMaterialAndUnpromotedLastRankPawnsAreRejected() {
        for (String square : new String[]{"a1", "a8"}) for (FigureColor color : new FigureColor[]{WHITE, BLACK}) {
            var board = board("e1", "e8"); put(board, Pawn.class, color, square); invalid(board);
        }
        var tooMany = new HashMap<>(factory.getGameField()); put(tooMany, Knight.class, WHITE, "a3"); invalid(tooMany);
        var board = board("e1", "e8");
        for (char file = 'a'; file <= 'h'; file++) put(board, Pawn.class, WHITE, file + "2");
        put(board, Pawn.class, WHITE, "a3"); invalid(board);
        put(board, Empty.class, NONE, "a3");
        put(board, Queen.class, WHITE, "a3"); put(board, Queen.class, WHITE, "b3"); invalid(board);
        put(board, Empty.class, NONE, "a2"); // One missing pawn can account for an extra queen.
        assertNotNull(load(board, WHITE, CastlingRights.none(), null, 0));
        put(board, Bishop.class, WHITE, "b1"); put(board, Bishop.class, WHITE, "d1"); invalid(board);
    }
    @Test public void invalidTurnCounterAndRightsAreRejected() {
        var board = factory.getGameField();
        assertThrows(IllegalArgumentException.class, () -> load(board, NONE, CastlingRights.initial(), null, 0));
        assertThrows(IllegalArgumentException.class, () -> load(board, null, CastlingRights.initial(), null, 0));
        assertThrows(IllegalArgumentException.class, () -> load(board, WHITE, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> load(board, WHITE, CastlingRights.initial(), null, -1));
        var sparse = board("e1", "e8");
        assertThrows(IllegalArgumentException.class, () -> load(sparse, WHITE, CastlingRights.initial(), null, 0));
        put(sparse, Rook.class, BLACK, "h1");
        assertThrows(IllegalArgumentException.class, () -> load(sparse, WHITE, new CastlingRights(true, false, false, false), null, 0));
    }
    @Test public void checkAgainstNonMovingKingIsRejectedButMovingKingMayBeChecked() {
        var board = board("e1", "h8"); put(board, Rook.class, BLACK, "e8");
        Game checked = load(board, WHITE, CastlingRights.none(), null, 0);
        assertTrue(checked.isInCheck());
        assertEquals(MoveResult.SUCCESS, checked.moveFigure(checked.getCurrentPlayer(), pos("e1"), pos("d1")));
        assertThrows(IllegalArgumentException.class, () -> load(board, BLACK, CastlingRights.none(), null, 0));
        put(board, Rook.class, WHITE, "h1"); invalid(board); // Both kings in check.
    }
    @Test public void loadedTerminalPositionsAreEvaluatedImmediately() {
        Game bare = load(board("e1", "e8"), WHITE, CastlingRights.none(), null, 0);
        assertEquals(Termination.INSUFFICIENT_MATERIAL, bare.getOutcome().reason());
        assertEquals(MoveResult.GAME_OVER, bare.moveFigure(bare.getCurrentPlayer(), pos("e1"), pos("e2")));
        Game mate = new Game(8); play(mate, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(mate.getOutcome(), Game.fromPosition(mate.getPosition()).getOutcome());
        var board = board("e1", "e8"); put(board, Rook.class, WHITE, "a1");
        assertEquals(Termination.SEVENTY_FIVE_MOVE_RULE, load(board, WHITE, CastlingRights.none(), null, 150).getOutcome().reason());
    }
    @Test public void loadedCastlingWorksAndRetainedSnapshotsDoNotMove() {
        for (FigureColor color : new FigureColor[]{WHITE, BLACK}) for (boolean kingSide : new boolean[]{true, false}) {
            var board = board("e1", "e8"); String rank = color == WHITE ? "1" : "8";
            put(board, Rook.class, color, (kingSide ? "h" : "a") + rank);
            var rights = new CastlingRights(color == WHITE && kingSide, color == WHITE && !kingSide,
                    color == BLACK && kingSide, color == BLACK && !kingSide);
            Game game = load(board, color, rights, null, 3); Field before = game.getGameField();
            play(game, "e" + rank + (kingSide ? "g" : "c") + rank);
            assertTrue(before.getFigureByPosition(pos("e" + rank)) instanceof King);
            assertTrue(before.getFigureByPosition(pos((kingSide ? "h" : "a") + rank)) instanceof Rook);
            assertTrue(game.getFigureByPosition(pos((kingSide ? "f" : "d") + rank)) instanceof Rook);
            assertFalse(game.getCastlingRights().allows(color, kingSide));
            assertEquals(4, game.getHalfmoveClock());
        }
    }
    @Test public void positionRoundTripPreservesLegalEnPassantForBothColors() {
        for (boolean white : new boolean[]{true, false}) {
            Game source = new Game(8);
            if (white) play(source, "e2e4", "a7a6", "e4e5", "d7d5");
            else play(source, "a2a3", "e7e5", "a3a4", "e5e4", "d2d4");
            Game game = Game.fromPosition(source.getPosition()); Field before = game.getGameField();
            assertEquals(source.getPositionKey(), game.getPositionKey());
            play(game, white ? "e5d6" : "e4d3");
            assertTrue(before.getFigureByPosition(pos(white ? "d5" : "d4")) instanceof Pawn);
            assertTrue(game.getFigureByPosition(pos(white ? "d5" : "d4")) instanceof Empty);
            assertEquals(MoveKind.EN_PASSANT, game.getMoves().getFirst().getKind());
        }
    }
    @Test public void invalidEnPassantMetadataIsRejected() {
        Game source = new Game(8); play(source, "e2e4"); var position = source.getPosition();
        assertNotNull(Game.fromPosition(position)); // No adjacent capturing pawn is required.
        var board = new HashMap<>(position.board());
        assertThrows(IllegalArgumentException.class, () -> load(board, BLACK, position.castlingRights(), pos("e6"), 0));
        assertThrows(IllegalArgumentException.class, () -> load(board, BLACK, position.castlingRights(), pos("e3"), 1));
        put(board, Pawn.class, WHITE, "e2");
        assertThrows(IllegalArgumentException.class, () -> load(board, BLACK, position.castlingRights(), pos("e3"), 0));
        put(board, Empty.class, NONE, "e2"); put(board, Pawn.class, BLACK, "e4");
        assertThrows(IllegalArgumentException.class, () -> load(board, BLACK, position.castlingRights(), pos("e3"), 0));
        var illegalBefore = board("a1", "h4"); put(illegalBefore, Rook.class, WHITE, "a4"); put(illegalBefore, Pawn.class, WHITE, "e4");
        assertThrows(IllegalArgumentException.class, () -> load(illegalBefore, BLACK, CastlingRights.none(), pos("e3"), 0));
    }
    @Test public void loadedPromotionPreservesOriginalPawnSnapshot() {
        var board = board("h1", "h8"); put(board, Pawn.class, WHITE, "b7");
        Game game = load(board, WHITE, CastlingRights.none(), null, 0); Field before = game.getGameField();
        assertEquals(MoveResult.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("b7"), pos("b8"), Promotion.QUEEN));
        assertTrue(before.getFigureByPosition(pos("b7")) instanceof Pawn);
        assertTrue(before.getFigureByPosition(pos("b8")) instanceof Empty);
        assertTrue(game.getFigureByPosition(pos("b8")) instanceof Queen);
    }
    @Test public void failedLoadAndRejectedMovePreserveExistingGameExactly() {
        Game game = new Game(8); play(game, "e2e4", "d7d5");
        var before = new Positions.State(game); Field field = game.getGameField();
        var bad = new HashMap<>(field.getGameField()); bad.remove(pos("e1")); invalid(bad);
        before.assertUnchanged(game); assertSame(field, game.getGameField());
        assertEquals(MoveResult.ILLEGAL_MOVEMENT, game.moveFigure(game.getCurrentPlayer(), pos("e4"), pos("e6")));
        before.assertUnchanged(game); assertSame(field, game.getGameField());
    }

    @Test public void loadedBlackTurnUsesItsClockAndHalfmoveCounter() {
        var board = board("e1", "e8"); put(board, Rook.class, BLACK, "a8");
        var time = new java.util.concurrent.atomic.AtomicLong();
        Game game = Game.fromPosition(new GamePosition(board, BLACK, CastlingRights.none(), null, 100), time::get);
        assertSame(game.getBlackPlayer(), game.getCurrentPlayer());
        assertTrue(game.canClaimDraw(game.getCurrentPlayer()));
        game.startClock(10); time.set(1_500_000_000L);
        assertEquals(8_500_000_000L, game.getClockSnapshot().getBlackNanos());
        assertEquals(10_000_000_000L, game.getClockSnapshot().getWhiteNanos());
        play(game, "a8a7"); assertEquals(101, game.getHalfmoveClock());
        assertEquals(WHITE, game.getClockSnapshot().getActiveColor());
    }

    private static final class UnknownFigure extends Figure {
        UnknownFigure(Position square, Field field) { super(square, WHITE, field, "Unknown", 0); }
        @Override public void fillMoves() { throw new AssertionError("Invalid piece must never be computed"); }
        @Override public void computeFutureMovesAndAttacks() { throw new AssertionError("Invalid piece must never be computed"); }
    }
}
