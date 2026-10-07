package game;

import model.figures.*;
import org.junit.Test;
import rules.standard.MaterialRules;

import static game.Positions.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class TerminationTest {
    @Test public void concurrentRequestsCannotApplyTwoMovesForSamePlayer() throws Exception {
        Game game = new Game(8);
        Player white = game.getWhitePlayer();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        var start = new java.util.concurrent.CountDownLatch(1);
        var requests = new java.util.ArrayList<java.util.concurrent.Future<MoveResult>>();
        try {
            for (int i = 0; i < 8; i++) requests.add(pool.submit(() -> {
                start.await();
                return game.moveFigure(white, pos("e2"), pos("e4"));
            }));
            start.countDown();
            int successes = 0;
            for (var request : requests) {
                MoveResult result = request.get(5, java.util.concurrent.TimeUnit.SECONDS);
                if (result.isSuccess()) successes++;
                else assertEquals(MoveResult.WRONG_PLAYER, result);
            }
            assertEquals(1, successes); assertEquals(1, game.getMoves().size());
            assertSame(game.getBlackPlayer(), game.getCurrentPlayer());
        } finally { pool.shutdownNow(); }
    }
    @Test public void foolsMateEndsGameWithBlackWinner() {
        Game game = new Game(8); play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(Termination.CHECKMATE, game.getOutcome().getReason());
        assertEquals(BLACK, game.getOutcome().getWinner()); assertTrue(game.isInCheck());
        assertTrue(game.getLegalMoves(pos("e1")).getCanMove().isEmpty());
        rejected(game, MoveResult.GAME_OVER, "a2", "a3");
    }
    @Test public void whiteCheckmateAndStalemateAreDistinct() {
        Game mate = game("f6", "h8"); put(mate, Queen.class, WHITE, "g6");
        play(mate, "g6g7"); assertEquals(Termination.CHECKMATE, mate.getOutcome().getReason());
        assertEquals(WHITE, mate.getOutcome().getWinner());
        Game stale = game("f7", "h8"); put(stale, Queen.class, WHITE, "g5");
        play(stale, "g5g6"); assertEquals(Termination.STALEMATE, stale.getOutcome().getReason());
        assertEquals(NONE, stale.getOutcome().getWinner()); assertFalse(stale.isInCheck());
    }
    @Test public void checkWithAnEscapeDoesNotEndGame() {
        Game game = game("a1", "h8"); put(game, Rook.class, WHITE, "h1");
        play(game, "h1h7"); assertTrue(game.isInCheck()); assertFalse(game.getOutcome().isFinished());
        assertTrue(game.getLegalMoves(pos("h8")).getCanMove().contains(pos("g8")));
    }
    @Test public void automaticMaterialDrawRecognizesKingsAndSingleMinor() {
        Game kings = game("a1", "h8"); play(kings, "a1a2");
        assertEquals(Termination.INSUFFICIENT_MATERIAL, kings.getOutcome().getReason());
        Game bishop = game("a1", "h8"); put(bishop, Bishop.class, WHITE, "c1"); play(bishop, "c1d2");
        assertEquals(Termination.INSUFFICIENT_MATERIAL, bishop.getOutcome().getReason());
        Game knight = game("a1", "h8"); put(knight, Knight.class, WHITE, "b1"); play(knight, "b1c3");
        assertEquals(Termination.INSUFFICIENT_MATERIAL, knight.getOutcome().getReason());
    }
    @Test public void bishopSquareColorsMatterForMaterialDraw() {
        Game same = game("a1", "h8"); put(same, Bishop.class, WHITE, "c1"); put(same, Bishop.class, BLACK, "f8");
        play(same, "c1d2"); assertEquals(Termination.INSUFFICIENT_MATERIAL, same.getOutcome().getReason());
        Game opposite = game("a1", "h8"); put(opposite, Bishop.class, WHITE, "c1"); put(opposite, Bishop.class, BLACK, "e8");
        play(opposite, "c1d2"); assertFalse(opposite.getOutcome().isFinished());
    }
    @Test public void twoKnightsAndBishopKnightDoNotTriggerMaterialDraw() {
        Game knights = game("a1", "h8"); put(knights, Knight.class, WHITE, "b1"); put(knights, Knight.class, WHITE, "g1");
        play(knights, "b1c3"); assertFalse(knights.getOutcome().isFinished());
        Game mixed = game("a1", "h8"); put(mixed, Knight.class, WHITE, "b1"); put(mixed, Bishop.class, WHITE, "c1");
        play(mixed, "b1c3"); assertFalse(mixed.getOutcome().isFinished());
    }
    @Test public void multipleSameColorBishopsStillCannotMateBareKing() {
        Game game = game("a1", "h8"); put(game, Bishop.class, WHITE, "c1"); put(game, Bishop.class, WHITE, "e3");
        assertFalse(MaterialRules.canPossiblyMate(game.getGameField().getGameField(), WHITE));
        play(game, "c1d2"); assertEquals(Termination.INSUFFICIENT_MATERIAL, game.getOutcome().getReason());
    }
    @Test public void threefoldIsClaimableButNotAutomatic() {
        Game game = new Game(8); repeatKnights(game, 2);
        assertEquals(3, game.getRepetitionCount()); assertFalse(game.getOutcome().isFinished());
        assertTrue(game.canClaimDraw(game.getCurrentPlayer()));
        assertEquals(MoveResult.WRONG_PLAYER, game.claimDraw(game.getBlackPlayer()));
        assertEquals(MoveResult.SUCCESS, game.claimDraw(game.getWhitePlayer()));
        assertEquals(Termination.THREEFOLD_REPETITION, game.getOutcome().getReason());
        rejected(game, MoveResult.GAME_OVER, "g1", "f3");
    }
    @Test public void fivefoldIsAutomatic() {
        Game game = new Game(8); repeatKnights(game, 4);
        assertEquals(5, game.getRepetitionCount());
        assertEquals(Termination.FIVEFOLD_REPETITION, game.getOutcome().getReason());
        assertEquals(NONE, game.getOutcome().getWinner());
    }
    @Test public void intendedThirdRepetitionCanBeClaimedWithoutPlayingMove() {
        Game game = new Game(8); repeatKnights(game, 1); play(game, "g1f3", "g8f6", "f3g1");
        Player mover = game.getCurrentPlayer(); String key = game.getPositionKey(); int plies = game.getMoves().size();
        assertFalse(game.canClaimDraw(mover));
        assertTrue(game.canClaimDrawAfterMove(mover, pos("f6"), pos("g8"), null));
        assertEquals(MoveResult.SUCCESS, game.claimDraw(mover, pos("f6"), pos("g8"), null));
        assertEquals(Termination.THREEFOLD_REPETITION, game.getOutcome().getReason());
        assertEquals(plies, game.getMoves().size()); assertEquals(key, game.getPositionKey());
        assertTrue(game.getFigureByPosition(pos("f6")) instanceof Knight);
    }
    @Test public void invalidClaimsAndProspectiveClaimsDoNotChangeChessState() {
        Game game = new Game(8); State before = new State(game);
        assertEquals(MoveResult.INVALID_DRAW_CLAIM, game.claimDraw(game.getCurrentPlayer())); before.assertUnchanged(game);
        assertEquals(MoveResult.INVALID_DRAW_CLAIM, game.claimDraw(game.getCurrentPlayer(), pos("g1"), pos("f3"), null));
        before.assertUnchanged(game);
        assertEquals(MoveResult.ILLEGAL_MOVEMENT, game.claimDraw(game.getCurrentPlayer(), pos("a1"), pos("a3"), null));
        before.assertUnchanged(game);
        assertFalse(game.canClaimDrawAfterMove(game.getBlackPlayer(), pos("g1"), pos("f3"), null)); before.assertUnchanged(game);
    }
    @Test public void fiftyMoveRuleRequiresClaimAndCountsBothSides() {
        Game game = new Game(8); set(game, "halfmoveClock", 99); play(game, "g1f3");
        assertEquals(100, game.getHalfmoveClock()); assertFalse(game.getOutcome().isFinished());
        assertEquals(MoveResult.SUCCESS, game.claimDraw(game.getCurrentPlayer()));
        assertEquals(Termination.FIFTY_MOVE_RULE, game.getOutcome().getReason());
    }
    @Test public void intendedFiftiethMoveCanBeClaimedWithoutApplyingIt() {
        Game game = new Game(8); set(game, "halfmoveClock", 99);
        assertTrue(game.canClaimDrawAfterMove(game.getCurrentPlayer(), pos("g1"), pos("f3"), null));
        assertEquals(MoveResult.SUCCESS, game.claimDraw(game.getCurrentPlayer(), pos("g1"), pos("f3"), null));
        assertTrue(game.getMoves().isEmpty()); assertEquals(99, game.getHalfmoveClock());
        assertEquals(Termination.FIFTY_MOVE_RULE, game.getOutcome().getReason());
    }
    @Test public void pawnMoveAndCaptureResetQuietMoveCounter() {
        Game pawn = new Game(8); set(pawn, "halfmoveClock", 99); play(pawn, "e2e4");
        assertEquals(0, pawn.getHalfmoveClock()); assertFalse(pawn.canClaimDraw(pawn.getCurrentPlayer()));
        Game capture = new Game(8); play(capture, "e2e4", "d7d5"); set(capture, "halfmoveClock", 99); play(capture, "e4d5");
        assertEquals(0, capture.getHalfmoveClock());
        Game planned = new Game(8); set(planned, "halfmoveClock", 99);
        assertFalse(planned.canClaimDrawAfterMove(planned.getCurrentPlayer(), pos("e2"), pos("e4"), null));
    }
    @Test public void seventyFiveMovesAutomaticallyEndGame() {
        Game game = new Game(8); set(game, "halfmoveClock", 149); play(game, "g1f3");
        assertEquals(150, game.getHalfmoveClock()); assertEquals(Termination.SEVENTY_FIVE_MOVE_RULE, game.getOutcome().getReason());
    }
    @Test public void checkmateTakesPriorityOverSeventyFiveMoveDraw() {
        Game game = new Game(8); play(game, "f2f3", "e7e5", "g2g4"); set(game, "halfmoveClock", 149); play(game, "d8h4");
        assertEquals(150, game.getHalfmoveClock()); assertEquals(Termination.CHECKMATE, game.getOutcome().getReason());
    }
    @Test public void resignationEndsGameAndCannotBeOverwritten() {
        Game game = new Game(8); assertEquals(MoveResult.SUCCESS, game.resign(game.getWhitePlayer()));
        assertEquals(Termination.RESIGNATION, game.getOutcome().getReason()); assertEquals(BLACK, game.getOutcome().getWinner());
        State ended = new State(game);
        assertEquals(MoveResult.GAME_OVER, game.resign(game.getBlackPlayer()));
        assertEquals(MoveResult.GAME_OVER, game.offerDraw(game.getWhitePlayer()));
        assertEquals(MoveResult.GAME_OVER, game.acceptDraw(game.getBlackPlayer()));
        assertEquals(MoveResult.GAME_OVER, game.declineDraw(game.getBlackPlayer()));
        assertEquals(MoveResult.GAME_OVER, game.claimDraw(game.getWhitePlayer()));
        assertEquals(MoveResult.GAME_OVER, game.claimDraw(game.getWhitePlayer(), pos("g1"), pos("f3"), null));
        ended.assertUnchanged(game);
    }
    @Test public void resigningAgainstBareKingIsDraw() {
        Game game = game("a1", "h8"); put(game, Queen.class, WHITE, "d1");
        assertEquals(MoveResult.SUCCESS, game.resign(game.getWhitePlayer()));
        assertEquals(Termination.RESIGNATION_DRAW, game.getOutcome().getReason()); assertEquals(NONE, game.getOutcome().getWinner());
    }
    @Test public void offerAcceptAndDeclineValidateParticipantsAndAgreementTiming() {
        Game game = new Game(8); State before = new State(game);
        assertEquals(MoveResult.WRONG_PLAYER, game.offerDraw(new Player(true)));
        assertEquals(MoveResult.WRONG_PLAYER, game.resign(null));
        assertEquals(MoveResult.NO_DRAW_OFFER, game.acceptDraw(game.getBlackPlayer())); before.assertUnchanged(game);
        assertEquals(MoveResult.SUCCESS, game.offerDraw(game.getWhitePlayer()));
        assertEquals(MoveResult.DRAW_OFFER_PENDING, game.offerDraw(game.getBlackPlayer()));
        assertEquals(MoveResult.NO_DRAW_OFFER, game.acceptDraw(game.getWhitePlayer()));
        assertEquals(MoveResult.DRAW_AGREEMENT_TOO_EARLY, game.acceptDraw(game.getBlackPlayer()));
        assertEquals(MoveResult.SUCCESS, game.declineDraw(game.getBlackPlayer())); assertNull(game.getDrawOffer());
        play(game, "e2e4", "e7e5");
        assertEquals(MoveResult.SUCCESS, game.offerDraw(game.getWhitePlayer()));
        assertEquals(MoveResult.SUCCESS, game.acceptDraw(game.getBlackPlayer()));
        assertEquals(Termination.DRAW_AGREEMENT, game.getOutcome().getReason());
    }
    @Test public void opponentsMoveDeclinesDrawButOfferSurvivesOfferersOwnMove() {
        Game game = new Game(8); assertEquals(MoveResult.SUCCESS, game.offerDraw(game.getWhitePlayer()));
        play(game, "e2e4"); assertSame(game.getWhitePlayer(), game.getDrawOffer());
        play(game, "e7e5"); assertNull(game.getDrawOffer());
    }
    @Test public void emptyHistoryIsSafeAndHistoryGettersCopyCoordinates() {
        Game game = new Game(8); assertEquals("", game.getLastMoveAsString()); play(game, "e2e4");
        MadeMove move = game.getMoves().getLast(); assertEquals(pos("e2"), move.getFrom()); assertEquals(pos("e4"), move.getTo());
        assertEquals(pos("e2"), move.getFrom()); assertEquals(pos("e4"), move.getTo());
        assertEquals(1, move.getPly()); assertEquals(WHITE, move.getColor()); assertNotNull(move.getClock());
        assertThrows(UnsupportedOperationException.class, () -> game.getMoves().clear());
    }
    private static void repeatKnights(Game game, int repeats) {
        for (int i = 0; i < repeats; i++) play(game, "g1f3", "g8f6", "f3g1", "f6g8");
    }
}
