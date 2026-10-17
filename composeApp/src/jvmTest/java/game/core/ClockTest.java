package game.core;

import game.moves.MoveResult;
import game.moves.Promotion;
import game.state.ClockSnapshot;
import game.state.Termination;

import model.figures.*;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static game.core.Positions.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class ClockTest {
    @Test public void untimedGameDoesNotExpire() {
        AtomicLong time = new AtomicLong(); Game game = new Game(8, time::get);
        time.set(1_000_000_000_000L); assertFalse(game.getClockSnapshot().isEnabled());
        assertFalse(game.getOutcome().isFinished()); play(game, "e2e4");
    }
    @Test public void clockIsConfiguredOnlyOnceBeforeMovesAndRequiresPositiveTime() {
        Game game = new Game(8); assertThrows(IllegalArgumentException.class, () -> game.startClock(0));
        assertThrows(IllegalArgumentException.class, () -> game.startClock(-1));
        game.startClock(180); assertTrue(game.getClockSnapshot().isRunning());
        assertThrows(IllegalStateException.class, () -> game.startClock(600));
        Game played = new Game(8); play(played, "e2e4");
        assertThrows(IllegalStateException.class, () -> played.startClock(60));
    }
    @Test public void fractionsOfASecondAreChargedAcrossTurnSwitches() {
        AtomicLong time = new AtomicLong(); Game game = new Game(8, time::get); game.startClock(10);
        time.addAndGet(250_000_000L); play(game, "e2e4");
        assertEquals(9_750_000_000L, game.getClockSnapshot().getWhiteNanos());
        time.addAndGet(375_000_000L); play(game, "e7e5");
        time.addAndGet(125_000_000L);
        ClockSnapshot snapshot = game.getClockSnapshot();
        assertEquals(9_625_000_000L, snapshot.getWhiteNanos()); assertEquals(9_625_000_000L, snapshot.getBlackNanos());
        assertEquals(WHITE, snapshot.getActiveColor());
    }
    @Test public void latePollingChargesAllElapsedTime() {
        AtomicLong time = new AtomicLong(); Game game = new Game(8, time::get); game.startClock(10);
        time.set(4_321_000_000L);
        assertEquals(5_679_000_000L, game.getClockSnapshot().getWhiteNanos());
        assertEquals(10_000_000_000L, game.getClockSnapshot().getBlackNanos());
        assertEquals(5679L, game.getClockSnapshot().getWhiteMillis());
    }
    @Test public void pollingFrequencyDoesNotChangeClockResult() {
        AtomicLong time = new AtomicLong(); Game frequent = new Game(8, time::get), sparse = new Game(8, time::get);
        frequent.startClock(10); sparse.startClock(10);
        for (int i = 1; i <= 37; i++) { time.set(i * 100_000_000L); frequent.getClockSnapshot(); }
        assertEquals(frequent.getClockSnapshot(), sparse.getClockSnapshot());
    }
    @Test public void timeoutWithoutPollingRejectsMoveBeforeBoardChanges() {
        AtomicLong time = new AtomicLong(); Game game = new Game(8, time::get); game.startClock(1);
        time.set(1_000_000_000L);
        assertEquals(MoveResult.GAME_OVER, move(game, "e2", "e4"));
        assertEquals(Termination.TIMEOUT, game.getOutcome().getReason()); assertEquals(BLACK, game.getOutcome().getWinner());
        assertTrue(game.getMoves().isEmpty()); assertTrue(game.getFigureByPosition(pos("e2")) instanceof Pawn);
        assertEquals(0L, game.getClockSnapshot().getWhiteNanos()); assertFalse(game.getClockSnapshot().isRunning());
    }
    @Test public void blackTimeoutAwardsWhiteWinAndStopsBothClocks() {
        AtomicLong time = new AtomicLong(); Game game = new Game(8, time::get); game.startClock(1); play(game, "e2e4");
        time.set(2_000_000_000L); assertEquals(WHITE, game.getOutcome().getWinner());
        assertEquals(Termination.TIMEOUT, game.getOutcome().getReason());
        ClockSnapshot frozen = game.getClockSnapshot(); time.set(10_000_000_000L); assertEquals(frozen, game.getClockSnapshot());
    }
    @Test public void timeoutAgainstBareKingIsDrawEvenIfFlaggedPlayerHasRook() {
        AtomicLong time = new AtomicLong(); Game game = game("a1", "h8", time::get);
        put(game, Rook.class, WHITE, "c1"); game.startClock(1); time.set(1_000_000_000L);
        assertEquals(Termination.TIMEOUT_DRAW, game.getOutcome().getReason()); assertEquals(NONE, game.getOutcome().getWinner());
    }
    @Test public void timeoutAgainstSingleBishopWithBareKingIsDraw() {
        AtomicLong time = new AtomicLong(); Game game = game("a1", "h8", time::get);
        put(game, Bishop.class, BLACK, "c8"); game.startClock(1); time.set(1_000_000_000L);
        assertEquals(Termination.TIMEOUT_DRAW, game.getOutcome().getReason());
    }
    @Test public void opponentPawnCanMakeLoneBishopMatePossibleOnTimeout() {
        AtomicLong time = new AtomicLong(); Game game = game("a1", "h8", time::get);
        put(game, Pawn.class, WHITE, "h2"); put(game, Bishop.class, BLACK, "c8"); game.startClock(1); time.set(1_000_000_000L);
        assertEquals(Termination.TIMEOUT, game.getOutcome().getReason()); assertEquals(BLACK, game.getOutcome().getWinner());
    }
    @Test public void loneKnightCannotWinOnTimeAgainstOnlyEnemyKingAndQueen() {
        AtomicLong time = new AtomicLong(); Game game = game("a1", "h8", time::get);
        put(game, Queen.class, WHITE, "d1"); put(game, Knight.class, BLACK, "b8"); game.startClock(1); time.set(1_000_000_000L);
        assertEquals(Termination.TIMEOUT_DRAW, game.getOutcome().getReason());
    }
    @Test public void rejectedMoveDoesNotRefundThinkingTime() {
        AtomicLong time = new AtomicLong(); Game game = new Game(8, time::get); game.startClock(10);
        time.set(250_000_000L); assertEquals(MoveResult.ILLEGAL_MOVEMENT, move(game, "a1", "a3"));
        assertEquals(9_750_000_000L, game.getClockSnapshot().getWhiteNanos());
        assertTrue(game.getMoves().isEmpty()); assertSame(game.getWhitePlayer(), game.getCurrentPlayer());
    }
    @Test public void promotionChoiceDoesNotPauseOrSwitchClock() {
        AtomicLong time = new AtomicLong(); Game game = game("h1", "h8", time::get); put(game, Pawn.class, WHITE, "b7");
        game.startClock(10); time.set(250_000_000L); assertEquals(MoveResult.PROMOTION_REQUIRED, move(game, "b7", "b8"));
        time.set(500_000_000L);
        assertEquals(MoveResult.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("b7"), pos("b8"), Promotion.QUEEN));
        assertEquals(9_500_000_000L, game.getClockSnapshot().getWhiteNanos()); assertEquals(BLACK, game.getClockSnapshot().getActiveColor());
    }
    @Test public void checkmateFreezesClocksAndIsNeverReplacedByTimeout() {
        AtomicLong time = new AtomicLong(); Game game = new Game(8, time::get); game.startClock(10);
        play(game, "f2f3", "e7e5", "g2g4", "d8h4"); ClockSnapshot ended = game.getClockSnapshot();
        assertFalse(ended.isRunning()); time.set(100_000_000_000L);
        assertEquals(Termination.CHECKMATE, game.getOutcome().getReason()); assertEquals(ended, game.getClockSnapshot());
    }
    @Test public void expirationDuringValidationDoesNotApplyMove() {
        AtomicLong reads = new AtomicLong();
        Game game = new Game(8, () -> reads.getAndIncrement() < 2 ? 0L : 1_000_000_000L);
        game.startClock(1);
        assertEquals(MoveResult.GAME_OVER, move(game, "e2", "e4"));
        assertTrue(game.getMoves().isEmpty()); assertTrue(game.getFigureByPosition(pos("e2")) instanceof Pawn);
    }
    @Test public void snapshotsAndMoveRecordsRetainTimeOutsideCompose() {
        AtomicLong time = new AtomicLong(); Game game = new Game(8, time::get); game.startClock(180);
        time.set(123_456_789L); play(game, "e2e4");
        ClockSnapshot saved = game.getMoves().getLast().getClock();
        assertEquals(179_876_543_211L, saved.getWhiteNanos());
        assertEquals(180_000_000_000L, saved.getBlackNanos());
        time.set(2_000_000_000L); game.getClockSnapshot(); assertEquals(180_000_000_000L, saved.getBlackNanos());
    }
}
