package game;

import model.Figure;
import model.figures.*;
import org.junit.Test;

import static game.Positions.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class SpecialMovesTest {
    @Test public void promotionRequiresChoiceWithoutChangingState() {
        Game game = game("h1", "h8");
        put(game, Pawn.class, WHITE, "b7");
        assertTrue(game.getLegalMoves(pos("b7")).getCanMove().contains(pos("b8")));
        rejected(game, MoveResult.PROMOTION_REQUIRED, "b7", "b8");
    }
    @Test public void bothColorsCanChooseEveryPromotion() {
        for (var color : new model.FigureColor[]{WHITE, BLACK}) for (Promotion promotion : Promotion.values()) {
            Game game = game("h1", "h8");
            String from = color == WHITE ? "b7" : "b2", to = color == WHITE ? "b8" : "b1";
            put(game, Pawn.class, color, from);
            if (color == BLACK) play(game, "h1g1");
            assertEquals(MoveResult.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to), promotion));
            Figure replacement = game.getFigureByPosition(pos(to));
            assertEquals(promotion.getFigureType(), replacement.getClass());
            assertEquals(color, replacement.getColor());
            assertEquals(pos(to), replacement.getPosition());
            assertTrue(game.getFigureByPosition(pos(from)) instanceof Empty);
            MadeMove record = game.getMoves().getLast();
            assertEquals(MoveKind.PROMOTION, record.getKind()); assertEquals(promotion, record.getPromotion());
            assertEquals("Pawn", record.getFigure()); assertTrue(record.toString().contains("= " + replacement.getName()));
            assertEquals(0, game.getHalfmoveClock());
        }
    }
    @Test public void promotionWithCaptureWorksForBothColors() {
        for (var color : new model.FigureColor[]{WHITE, BLACK}) {
            Game game = game("h1", "h8");
            String from = color == WHITE ? "b7" : "b2", to = color == WHITE ? "a8" : "a1";
            put(game, Pawn.class, color, from);
            put(game, Rook.class, color == WHITE ? BLACK : WHITE, to);
            if (color == BLACK) play(game, "h1g1");
            Player mover = game.getCurrentPlayer();
            assertEquals(MoveResult.SUCCESS, game.moveFigure(mover, pos(from), pos(to), Promotion.KNIGHT));
            assertTrue(game.getFigureByPosition(pos(to)) instanceof Knight);
            assertEquals(3L, mover.getPoints()); assertEquals(1, mover.getAttackedFigures().size());
            assertEquals("Rook", game.getMoves().getLast().getCapturedFigure());
            assertEquals(pos(to), game.getMoves().getLast().getCaptureSquare());
        }
    }
    @Test public void promotionCannotBeRequestedOnOrdinaryMove() {
        Game game = new Game(8);
        rejected(game, MoveResult.INVALID_PROMOTION, "e2", "e4", Promotion.QUEEN);
        rejected(game, MoveResult.INVALID_PROMOTION, "g1", "f3", Promotion.KNIGHT);
    }
    @Test public void promotionMustResolveOwnCheck() {
        Game game = game("e1", "h8");
        put(game, Rook.class, BLACK, "e8"); put(game, Pawn.class, WHITE, "b7");
        rejected(game, MoveResult.KING_IN_CHECK, "b7", "b8", Promotion.QUEEN);
    }
    @Test public void bothColorsCanCastleOnBothSides() {
        for (var color : new model.FigureColor[]{WHITE, BLACK}) for (boolean kingSide : new boolean[]{true, false}) {
            Game game = game("e1", "e8");
            String from = color == WHITE ? "e1" : "e8";
            String to = color == WHITE ? (kingSide ? "g1" : "c1") : (kingSide ? "g8" : "c8");
            String rookFrom = color == WHITE ? (kingSide ? "h1" : "a1") : (kingSide ? "h8" : "a8");
            String rookTo = color == WHITE ? (kingSide ? "f1" : "d1") : (kingSide ? "f8" : "d8");
            put(game, Rook.class, color, rookFrom);
            if (color == BLACK) { put(game, Pawn.class, WHITE, "a2"); play(game, "a2a3"); }
            assertTrue(game.getLegalMoves(pos(from)).getCanMove().contains(pos(to)));
            int plies = game.getMoves().size();
            assertEquals(MoveResult.SUCCESS, move(game, from, to));
            assertTrue(game.getFigureByPosition(pos(to)) instanceof King);
            assertTrue(game.getFigureByPosition(pos(rookTo)) instanceof Rook);
            assertTrue(game.getFigureByPosition(pos(from)) instanceof Empty);
            assertTrue(game.getFigureByPosition(pos(rookFrom)) instanceof Empty);
            assertEquals(pos(rookTo), game.getFigureByPosition(pos(rookTo)).getPosition());
            assertEquals(plies + 1, game.getMoves().size());
            assertEquals(MoveKind.CASTLING, game.getMoves().getLast().getKind());
            assertFalse(game.getCastlingRights().allows(color, true));
            assertFalse(game.getCastlingRights().allows(color, false));
        }
    }
    @Test public void movingKingAwayAndBackPermanentlyLosesCastlingRights() {
        Game game = game("e1", "e8"); put(game, Rook.class, WHITE, "h1");
        play(game, "e1e2", "e8d8", "e2e1", "d8e8");
        rejected(game, MoveResult.ILLEGAL_MOVEMENT, "e1", "g1");
    }
    @Test public void movingRookAwayAndBackLosesOnlyItsOwnRight() {
        Game game = game("e1", "e8"); put(game, Rook.class, WHITE, "h1"); put(game, Rook.class, WHITE, "a1");
        play(game, "h1h2", "e8d8", "h2h1", "d8e8");
        assertFalse(game.getCastlingRights().allows(WHITE, true)); assertTrue(game.getCastlingRights().allows(WHITE, false));
        rejected(game, MoveResult.ILLEGAL_MOVEMENT, "e1", "g1");
        assertEquals(MoveResult.SUCCESS, move(game, "e1", "c1"));
    }
    @Test public void capturedHomeRookRightIsNotRestoredByReplacementRook() {
        Game game = game("e1", "e8"); put(game, Rook.class, WHITE, "h1");
        put(game, Bishop.class, BLACK, "g2"); put(game, Pawn.class, WHITE, "a2");
        play(game, "a2a3", "g2h1");
        assertFalse(game.getCastlingRights().allows(WHITE, true));
        put(game, Rook.class, WHITE, "h1");
        rejected(game, MoveResult.ILLEGAL_MOVEMENT, "e1", "g1");
    }
    @Test public void castlingRequiresSameColorRookAndEmptyPathIncludingBFile() {
        Game missing = game("e1", "e8"); put(missing, Pawn.class, WHITE, "a2");
        rejected(missing, MoveResult.ILLEGAL_MOVEMENT, "e1", "g1");
        put(missing, Rook.class, BLACK, "h1"); rejected(missing, MoveResult.ILLEGAL_MOVEMENT, "e1", "g1");
        Game blocked = game("e1", "e8"); put(blocked, Rook.class, WHITE, "a1"); put(blocked, Knight.class, WHITE, "b1");
        rejected(blocked, MoveResult.ILLEGAL_MOVEMENT, "e1", "c1");
    }
    @Test public void cannotCastleOutOfThroughOrIntoCheck() {
        for (String attacker : new String[]{"e8", "f8", "g8"}) {
            Game game = game("e1", "a8"); put(game, Rook.class, WHITE, "h1"); put(game, Rook.class, BLACK, attacker);
            rejected(game, MoveResult.KING_IN_CHECK, "e1", "g1");
            assertFalse(game.getLegalMoves(pos("e1")).getCanMove().contains(pos("g1")));
        }
    }
    @Test public void emptyPawnDiagonalPreventsCastlingThroughCheck() {
        Game game = game("e1", "a8"); put(game, Rook.class, WHITE, "h1"); put(game, Pawn.class, BLACK, "e2");
        rejected(game, MoveResult.KING_IN_CHECK, "e1", "g1");
    }
    @Test public void attackedRookDoesNotPreventCastling() {
        Game game = game("e1", "a8"); put(game, Rook.class, WHITE, "h1"); put(game, Rook.class, BLACK, "h8");
        assertEquals(MoveResult.SUCCESS, move(game, "e1", "g1"));
    }
    @Test public void attackedBFileDoesNotPreventQueenSideCastling() {
        Game game = game("e1", "e8"); put(game, Rook.class, WHITE, "a1"); put(game, Rook.class, BLACK, "b8");
        assertEquals(MoveResult.SUCCESS, move(game, "e1", "c1"));
    }
    @Test public void whiteEnPassantRemovesCorrectPawnAndRecordsCapture() {
        Game game = new Game(8); play(game, "e2e4", "a7a6", "e4e5", "d7d5");
        Figure victim = game.getFigureByPosition(pos("d5"));
        assertEquals(pos("d6"), game.getEnPassantTarget());
        assertTrue(game.getLegalMoves(pos("e5")).getCanAttack().contains(pos("d6")));
        assertEquals(MoveResult.SUCCESS, move(game, "e5", "d6"));
        assertTrue(game.getFigureByPosition(pos("e5")) instanceof Empty);
        assertTrue(game.getFigureByPosition(pos("d5")) instanceof Empty);
        assertTrue(game.getFigureByPosition(pos("d6")) instanceof Pawn);
        assertSame(victim, game.getWhitePlayer().getAttackedFigures().getLast());
        assertEquals(1L, game.getWhitePlayer().getPoints());
        assertEquals(MoveKind.EN_PASSANT, game.getMoves().getLast().getKind());
        assertEquals(pos("d5"), game.getMoves().getLast().getCaptureSquare());
        assertNull(game.getEnPassantTarget()); assertEquals(0, game.getHalfmoveClock());
    }
    @Test public void blackEnPassantWorks() {
        Game game = new Game(8); play(game, "a2a3", "e7e5", "a3a4", "e5e4", "d2d4");
        assertEquals(MoveResult.SUCCESS, move(game, "e4", "d3"));
        assertTrue(game.getFigureByPosition(pos("d4")) instanceof Empty);
        assertEquals(BLACK, game.getFigureByPosition(pos("d3")).getColor());
        assertEquals(1L, game.getBlackPlayer().getPoints());
    }
    @Test public void enPassantExpiresAfterAnotherMove() {
        Game game = new Game(8); play(game, "e2e4", "a7a6", "e4e5", "d7d5", "g1f3", "g8f6");
        assertNull(game.getEnPassantTarget()); rejected(game, MoveResult.ILLEGAL_MOVEMENT, "e5", "d6");
    }
    @Test public void rejectedMoveDoesNotConsumeEnPassantRight() {
        Game game = new Game(8); play(game, "e2e4", "a7a6", "e4e5", "d7d5");
        rejected(game, MoveResult.ILLEGAL_MOVEMENT, "e5", "e7");
        assertEquals(MoveResult.SUCCESS, move(game, "e5", "d6"));
    }
    @Test public void singleStepDoesNotCreateEnPassantRight() {
        Game game = new Game(8); play(game, "e2e4", "d7d6", "e4e5", "d6d5");
        rejected(game, MoveResult.ILLEGAL_MOVEMENT, "e5", "d6");
    }
    @Test public void enPassantCannotExposeHorizontalRookAttack() {
        Game game = game("h4", "h8"); put(game, Pawn.class, WHITE, "g5");
        put(game, Pawn.class, BLACK, "f7"); put(game, Rook.class, BLACK, "a5");
        play(game, "h4h5", "f7f5");
        rejected(game, MoveResult.KING_IN_CHECK, "g5", "f6");
        assertFalse(game.getLegalMoves(pos("g5")).getCanAttack().contains(pos("f6")));
    }
    @Test public void enPassantCanRemoveCheckingPawn() {
        Game game = game("f4", "h8"); put(game, Pawn.class, WHITE, "e5"); put(game, Pawn.class, BLACK, "d7");
        play(game, "f4e4", "d7d5"); assertTrue(game.isInCheck());
        assertEquals(MoveResult.SUCCESS, move(game, "e5", "d6"));
    }
    @Test public void repetitionKeyUsesOnlyLegalEnPassantAndIncludesCastlingRights() {
        Game ordinary = new Game(8); play(ordinary, "e2e4");
        String ineffective = ordinary.getPositionKey(); set(ordinary, "enPassantTarget", null);
        assertEquals(ineffective, ordinary.getPositionKey());
        Game legal = new Game(8); play(legal, "e2e4", "a7a6", "e4e5", "d7d5");
        String effective = legal.getPositionKey(); set(legal, "enPassantTarget", null);
        assertNotEquals(effective, legal.getPositionKey());
        Game pinned = game("h4", "h8"); put(pinned, Pawn.class, WHITE, "g5");
        put(pinned, Pawn.class, BLACK, "f7"); put(pinned, Rook.class, BLACK, "a5");
        play(pinned, "h4h5", "f7f5"); String pinnedKey = pinned.getPositionKey(); set(pinned, "enPassantTarget", null);
        assertEquals(pinnedKey, pinned.getPositionKey());
        Game rights = game("e1", "e8"); put(rights, Rook.class, WHITE, "h1");
        String before = rights.getPositionKey(); play(rights, "h1h2", "e8d8", "h2h1", "d8e8");
        assertNotEquals(before, rights.getPositionKey()); assertEquals(1, rights.getRepetitionCount());
    }
}
