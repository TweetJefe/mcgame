package game.core;

import game.moves.MovementRejection;
import game.moves.Success;
import game.moves.TurnRejection;
import game.history.MadeMove;
import game.moves.AvailableMoves;
import game.moves.MoveResult;

import model.Figure;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static model.FigureColor.BLACK;
import static model.FigureColor.WHITE;
import static org.junit.Assert.*;

public class GameTest {
    @Test
    public void normalMoveUpdatesBoardHistoryAndTurn() {
        Game game = new Game(8);
        Figure pawn = game.getFigureByPosition(pos("e2"));
        Figure empty = game.getFigureByPosition(pos("e4"));
        assertEquals(Success.SUCCESS, move(game, "e2", "e4"));
        assertNotSame(pawn, game.getFigureByPosition(pos("e4")));
        assertTrue(game.getFigureByPosition(pos("e4")) instanceof Pawn);
        assertNotSame(empty, game.getFigureByPosition(pos("e2")));
        assertTrue(game.getFigureByPosition(pos("e2")) instanceof Empty);
        assertEquals(pos("e2"), pawn.getPosition());
        assertEquals(pos("e4"), empty.getPosition());
        assertSame(game.getBlackPlayer(), game.getCurrentPlayer());
        assertEquals("Pawn: (2, e) -> (4, e)", game.getLastMoveAsString());
        assertEquals(1, game.getWhitePlayer().getMadeMoves().size());
        assertTrue(game.getBlackPlayer().getMadeMoves().isEmpty());
        assertTrue(game.getWhitePlayer().getAttackedFigures().isEmpty());
        assertEquals(0L, game.getWhitePlayer().getPoints());
    }

    @Test
    public void captureRecordsVictimAndAwardsPointsOnce() {
        Game game = new Game(8);
        assertEquals(Success.SUCCESS, move(game, "e2", "e4"));
        assertEquals(Success.SUCCESS, move(game, "d7", "d5"));
        Figure pawn = game.getFigureByPosition(pos("e4"));
        Figure victim = game.getFigureByPosition(pos("d5"));
        assertEquals(Success.SUCCESS, move(game, "e4", "d5"));
        assertNotSame(pawn, game.getFigureByPosition(pos("d5")));
        assertTrue(game.getFigureByPosition(pos("d5")) instanceof Pawn);
        assertTrue(game.getFigureByPosition(pos("e4")) instanceof Empty);
        assertEquals(List.of(victim), game.getWhitePlayer().getAttackedFigures());
        assertEquals(1L, game.getWhitePlayer().getPoints());
        assertEquals(2, game.getWhitePlayer().getMadeMoves().size());
        reject(game, TurnRejection.WRONG_PLAYER, game.getWhitePlayer(), pos("d5"), pos("c6"));
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, game.getBlackPlayer(), pos("a8"), pos("a5"));
    }

    @Test
    public void wrongNullAndForeignPlayersCannotMove() {
        Game game = new Game(8);
        reject(game, TurnRejection.WRONG_PLAYER, game.getBlackPlayer(), pos("e7"), pos("e5"));
        reject(game, TurnRejection.WRONG_PLAYER, null, pos("e2"), pos("e4"));
        reject(game, TurnRejection.WRONG_PLAYER, new Player(true), pos("e2"), pos("e4"));
    }

    @Test
    public void invalidCoordinatesAreRejectedWithoutExceptions() {
        Game game = new Game(8);
        for (Position invalid : new Position[]{null, new Position(-1, 0), new Position(8, 0),
                new Position(0, -1), new Position(0, 8), new Position(Integer.MAX_VALUE, 4)}) {
            reject(game, MovementRejection.OUT_OF_BOUNDS, game.getCurrentPlayer(), invalid, pos("e4"));
            reject(game, MovementRejection.OUT_OF_BOUNDS, game.getCurrentPlayer(), pos("e2"), invalid);
        }
    }

    @Test
    public void emptyEnemyOwnAndUnchangedSquaresAreRejected() {
        Game game = new Game(8);
        reject(game, MovementRejection.EMPTY_SOURCE, "e3", "e4");
        reject(game, MovementRejection.WRONG_FIGURE_COLOR, "e7", "e5");
        reject(game, MovementRejection.OWN_PIECE_AT_TARGET, "e1", "d1");
        reject(game, MovementRejection.SAME_SQUARE, "e2", "e2");
    }

    @Test
    public void movementGeometryAndBlockersAreValidated() {
        Game game = new Game(8);
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, "a1", "a3");
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, "c1", "d3");
        assertEquals(Success.SUCCESS, move(game, "g1", "f3"));
        assertEquals(Success.SUCCESS, move(game, "b8", "c6"));
    }

    @Test
    public void pawnDoubleStepIsAvailableOnlyFromStartingRankForBothColors() {
        Game game = new Game(8);
        assertEquals(Success.SUCCESS, move(game, "e2", "e4"));
        assertEquals(Success.SUCCESS, move(game, "e7", "e5"));
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, "e4", "e6");
        assertEquals(Success.SUCCESS, move(game, "a2", "a3"));
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, "e5", "e3");
    }

    @Test
    public void pawnCannotJumpOverEitherColorForBothSides() {
        for (FigureColor pawnColor : new FigureColor[]{WHITE, BLACK}) {
            for (FigureColor blockerColor : new FigureColor[]{WHITE, BLACK}) {
                Game game = emptyGame("a1", "h8");
                String from = pawnColor == WHITE ? "e2" : "e7";
                String blocker = pawnColor == WHITE ? "e3" : "e6";
                String to = pawnColor == WHITE ? "e4" : "e5";
                put(game, Pawn.class, pawnColor, from);
                put(game, Knight.class, blockerColor, blocker);
                if (pawnColor == BLACK) assertEquals(Success.SUCCESS, move(game, "a1", "a2"));
                reject(game, MovementRejection.ILLEGAL_MOVEMENT, from, to);
                assertTrue(game.getLegalMoves(pos(from)).getCanMove().isEmpty());
            }
        }
    }

    @Test
    public void pawnCannotTakeForwardOrStepToEmptyDiagonal() {
        Game game = emptyGame("a1", "h8");
        put(game, Pawn.class, WHITE, "e2");
        put(game, Knight.class, BLACK, "e3");
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, "e2", "e3");
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, "e2", "d3");
    }

    @Test
    public void pawnDoubleStepCannotLandOnOccupiedSquare() {
        Game game = emptyGame("a1", "h8");
        put(game, Pawn.class, WHITE, "e2");
        put(game, Knight.class, BLACK, "e4");
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, "e2", "e4");
        assertEquals(Success.SUCCESS, move(game, "e2", "e3"));
    }

    @Test
    public void rookHistoryUsesRookName() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, WHITE, "a1");
        assertEquals(Success.SUCCESS, move(game, "a1", "a4"));
        assertEquals("Rook: (1, a) -> (4, a)", game.getLastMoveAsString());
    }

    @Test
    public void kingCannotMoveOntoEmptyPawnAttackSquare() {
        Game game = emptyGame("e1", "h8");
        put(game, Pawn.class, BLACK, "d3");
        reject(game, MovementRejection.KING_IN_CHECK, "e1", "e2");
        assertFalse(game.getLegalMoves(pos("e1")).getCanMove().contains(pos("e2")));
    }

    @Test
    public void pawnForwardSquareDoesNotCountAsAttack() {
        Game game = emptyGame("e1", "h8");
        put(game, Pawn.class, BLACK, "e3");
        assertEquals(Success.SUCCESS, move(game, "e1", "e2"));
    }

    @Test
    public void whitePawnAlsoProtectsEmptyDiagonal() {
        Game game = emptyGame("a1", "e8");
        put(game, Pawn.class, WHITE, "d6");
        assertEquals(Success.SUCCESS, move(game, "a1", "a2"));
        reject(game, MovementRejection.KING_IN_CHECK, "e8", "e7");
    }

    @Test
    public void kingCannotMoveIntoKnightAttack() {
        Game game = emptyGame("e1", "h8");
        put(game, Knight.class, BLACK, "f4");
        reject(game, MovementRejection.KING_IN_CHECK, "e1", "e2");
    }

    @Test
    public void kingCannotMoveIntoAnySlidingPieceAttack() {
        for (Class<? extends Figure> type : List.of(Bishop.class, Rook.class, Queen.class)) {
            Game game = emptyGame("e1", "h8");
            put(game, type, BLACK, type == Bishop.class ? "a6" : "e8");
            reject(game, MovementRejection.KING_IN_CHECK, "e1", "e2");
        }
    }

    @Test
    public void blockersStopSlidingAttacks() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, BLACK, "e8");
        put(game, Pawn.class, BLACK, "e4");
        assertEquals(Success.SUCCESS, move(game, "e1", "e2"));
    }

    @Test
    public void kingsCannotBecomeAdjacent() {
        Game game = emptyGame("e1", "e3");
        reject(game, MovementRejection.KING_IN_CHECK, "e1", "e2");
    }

    @Test
    public void kingOriginDoesNotShieldItsDestinationDuringValidation() {
        Game game = emptyGame("e2", "h8");
        put(game, Rook.class, BLACK, "e8");
        reject(game, MovementRejection.KING_IN_CHECK, "e2", "e1");
    }

    @Test
    public void pinnedPieceCannotExposeKingButCanKeepBlockingRay() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, WHITE, "e2");
        put(game, Rook.class, BLACK, "e8");
        reject(game, MovementRejection.KING_IN_CHECK, "e2", "d2");
        AvailableMoves legal = game.getLegalMoves(pos("e2"));
        assertFalse(legal.getCanMove().contains(pos("d2")));
        assertTrue(legal.getCanMove().contains(pos("e3")));
        assertEquals(Success.SUCCESS, move(game, "e2", "e3"));
    }

    @Test
    public void rejectedCaptureDoesNotRemoveVictimOrAwardPoints() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, WHITE, "e2");
        put(game, Rook.class, BLACK, "e8");
        put(game, Bishop.class, BLACK, "d2");
        reject(game, MovementRejection.KING_IN_CHECK, "e2", "d2");
        assertFalse(game.getLegalMoves(pos("e2")).getCanAttack().contains(pos("d2")));
    }

    @Test
    public void playerInCheckCannotMakeUnrelatedMove() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, BLACK, "e8");
        put(game, Rook.class, WHITE, "a1");
        reject(game, MovementRejection.KING_IN_CHECK, "a1", "a2");
    }

    @Test
    public void checkCanBeResolvedByBlocking() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, BLACK, "e8");
        put(game, Rook.class, WHITE, "a2");
        assertEquals(Success.SUCCESS, move(game, "a2", "e2"));
    }

    @Test
    public void checkCanBeResolvedByCapturingAttacker() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, BLACK, "e8");
        put(game, Bishop.class, WHITE, "d7");
        assertEquals(Success.SUCCESS, move(game, "d7", "e8"));
        assertEquals(3L, game.getWhitePlayer().getPoints());
    }

    @Test
    public void checkCanBeResolvedByKingEscape() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, BLACK, "e8");
        assertEquals(Success.SUCCESS, move(game, "e1", "d1"));
    }

    @Test
    public void blockingOnlyOneOfTwoChecksIsRejected() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, BLACK, "e8");
        put(game, Bishop.class, BLACK, "b4");
        put(game, Rook.class, WHITE, "a2");
        reject(game, MovementRejection.KING_IN_CHECK, "a2", "e2");
    }

    @Test
    public void kingCannotCaptureProtectedPiece() {
        Game game = emptyGame("e1", "h8");
        put(game, Knight.class, BLACK, "e2");
        put(game, Rook.class, BLACK, "e8");
        reject(game, MovementRejection.KING_IN_CHECK, "e1", "e2");
    }

    @Test
    public void kingCanCaptureUnprotectedPiece() {
        Game game = emptyGame("e1", "h8");
        put(game, Rook.class, BLACK, "e2");
        assertEquals(Success.SUCCESS, move(game, "e1", "e2"));
        assertTrue(game.getFigureByPosition(pos("e2")) instanceof King);
    }

    @Test
    public void kingCaptureIsExplicitlyRejectedAndNotHighlighted() {
        Game game = emptyGame("a1", "e8");
        put(game, Rook.class, WHITE, "e2");
        reject(game, MovementRejection.KING_CAPTURE_NOT_ALLOWED, "e2", "e8");
        assertFalse(game.getLegalMoves(pos("e2")).getCanAttack().contains(pos("e8")));
    }

    @Test
    public void pinnedOpponentStillAttacksSquaresForKingSafety() {
        Game game = emptyGame("d4", "f8");
        put(game, Rook.class, WHITE, "f1");
        put(game, Knight.class, BLACK, "f7");
        reject(game, MovementRejection.KING_IN_CHECK, "d4", "e5");
    }

    @Test
    public void legalTargetQueriesLeaveAllStateAndCachesUnchanged() {
        Game game = new Game(8);
        game.getGameField().compute(game.getFigureByPosition(pos("e2")));
        Snapshot before = new Snapshot(game);
        AvailableMoves legal = game.getLegalMoves(pos("e2"));
        assertEquals(List.of(pos("e3"), pos("e4")), legal.getCanMove());
        assertTrue(legal.getCanAttack().isEmpty());
        before.assertUnchanged(game);
        assertThrows(UnsupportedOperationException.class, () -> legal.getCanMove().add(pos("e5")));
    }

    @Test
    public void staleOrTamperedFigureCacheCannotAuthorizeMove() {
        Game game = new Game(8);
        Figure pawn = game.getFigureByPosition(pos("e2"));
        game.getGameField().compute(pawn);
        assertThrows(UnsupportedOperationException.class, () -> pawn.getCanMove().add(pos("e5")));
        reject(game, MovementRejection.ILLEGAL_MOVEMENT, "e2", "e5");
    }

    @Test
    public void callersCannotChangeStoredBoardOrHistoryThroughRequestCoordinates() {
        Game game = new Game(8);
        Position from = pos("e2");
        Position to = pos("e4");
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), from, to));
        assertEquals(pos("e2"), from);
        assertEquals(pos("e4"), to);
        assertTrue(game.getFigureByPosition(pos("e2")) instanceof Empty);
        assertEquals(pos("e4"), game.getFigureByPosition(pos("e4")).getPosition());
        assertEquals("Pawn: (2, e) -> (4, e)", game.getLastMoveAsString());
    }

    @Test
    public void madeMoveCopiesItsCoordinates() {
        Position from = pos("a1");
        Position to = pos("a4");
        MadeMove move = new MadeMove("Rook", from, to);
        assertEquals(pos("a1"), move.getFrom());
        assertEquals(pos("a4"), move.getTo());
        assertEquals("Rook: (1, a) -> (4, a)", move.toString());
    }

    @Test
    public void invalidOrEnemySelectionHasNoLegalTargets() {
        Game game = new Game(8);
        for (Position from : new Position[]{null, new Position(9, 4), pos("e3"), pos("e7")}) {
            assertTrue(game.getLegalMoves(from).getCanMove().isEmpty());
            assertTrue(game.getLegalMoves(from).getCanAttack().isEmpty());
        }
    }

    @Test
    public void missingKingOrCellIsRejectedBeforeGameCreation() {
        Game game = new Game(8);
        var board = new HashMap<>(game.getGameField().getGameField());
        board.put(pos("e1"), new Empty(pos("e1"), FigureColor.NONE, game.getGameField()));
        assertThrows(IllegalArgumentException.class, () -> Field.fromPosition(board));
        board.remove(pos("a2"));
        assertThrows(IllegalArgumentException.class, () -> Field.fromPosition(board));
        assertTrue(game.getFigureByPosition(pos("e1")) instanceof King);
    }

    private static Game emptyGame(String whiteKing, String blackKing) { return Positions.game(whiteKing, blackKing); }
    private static void put(Game game, Class<? extends Figure> type, FigureColor color, String cell) {
        Positions.put(game, type, color, cell);
    }

    private static Position pos(String cell) {
        return new Position(cell.charAt(1) - '1', cell.charAt(0) - 'a');
    }

    private static MoveResult move(Game game, String from, String to) {
        return game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to));
    }

    private static void reject(Game game, MoveResult expected, String from, String to) {
        reject(game, expected, game.getCurrentPlayer(), pos(from), pos(to));
    }

    private static void reject(Game game, MoveResult expected, Player player, Position from, Position to) {
        Snapshot before = new Snapshot(game);
        assertEquals(expected, game.moveFigure(player, from, to));
        before.assertUnchanged(game);
    }

    /** Rejection must preserve identities, positions, caches, turn, both histories and scores. */
    private static final class Snapshot {
        private final Map<Position, Figure> board;
        private final Map<Figure, Position> positions = new IdentityHashMap<>();
        private final Map<Figure, List<Position>> moves = new IdentityHashMap<>();
        private final Map<Figure, List<Position>> attacks = new IdentityHashMap<>();
        private final Player current;
        private final List<List<MadeMove>> histories = new ArrayList<>();
        private final List<List<Figure>> captures = new ArrayList<>();
        private final List<Long> points = new ArrayList<>();

        Snapshot(Game game) {
            board = new HashMap<>(game.getGameField().getGameField());
            for (Figure figure : board.values()) {
                Position position = figure.getPosition();
                positions.put(figure, new Position(position.getRow(), position.getColumn()));
                moves.put(figure, figure.getCanMove());
                attacks.put(figure, figure.getCanAttack());
            }
            current = game.getCurrentPlayer();
            for (Player player : List.of(game.getWhitePlayer(), game.getBlackPlayer())) {
                histories.add(List.copyOf(player.getMadeMoves()));
                captures.add(List.copyOf(player.getAttackedFigures()));
                points.add(player.getPoints());
            }
        }

        void assertUnchanged(Game game) {
            assertEquals(board, game.getGameField().getGameField());
            assertSame(current, game.getCurrentPlayer());
            for (Figure figure : board.values()) {
                assertEquals(positions.get(figure), figure.getPosition());
                assertEquals(moves.get(figure), figure.getCanMove());
                assertEquals(attacks.get(figure), figure.getCanAttack());
            }
            List<Player> players = List.of(game.getWhitePlayer(), game.getBlackPlayer());
            for (int i = 0; i < players.size(); i++) {
                assertEquals(histories.get(i), players.get(i).getMadeMoves());
                assertEquals(captures.get(i), players.get(i).getAttackedFigures());
                assertEquals(points.get(i).longValue(), players.get(i).getPoints());
            }
        }
    }
}
