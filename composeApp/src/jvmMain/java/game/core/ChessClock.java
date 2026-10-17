package game.core;

import game.state.ClockSnapshot;

import model.FigureColor;

/** Owned by Game. Timestamps are supplied by a monotonic source, never by the UI. */
final class ChessClock {
    private boolean enabled;
    private boolean running;
    private long whiteNanos;
    private long blackNanos;
    private long lastUpdate;
    private FigureColor active = FigureColor.WHITE;

    void start(int seconds, FigureColor color, long now) {
        if (seconds <= 0) throw new IllegalArgumentException("Time control must be positive");
        if (enabled) throw new IllegalStateException("Clock is already configured");
        whiteNanos = blackNanos = Math.multiplyExact((long) seconds, 1_000_000_000L);
        enabled = running = true;
        active = color;
        lastUpdate = now;
    }

    FigureColor update(long now) {
        if (!running) return FigureColor.NONE;
        long elapsed = Math.max(0L, now - lastUpdate);
        lastUpdate = now;
        if (active == FigureColor.WHITE) whiteNanos = Math.max(0L, whiteNanos - elapsed);
        else blackNanos = Math.max(0L, blackNanos - elapsed);
        return (active == FigureColor.WHITE ? whiteNanos : blackNanos) == 0 ? active : FigureColor.NONE;
    }

    void switchTo(FigureColor color) { active = color; }
    /** Resume from saved remaining time; never reuse the old monotonic timestamp. */
    void restore(ClockSnapshot saved, long now) {
        enabled = saved.enabled(); running = saved.running();
        whiteNanos = saved.whiteNanos(); blackNanos = saved.blackNanos();
        active = saved.activeColor(); lastUpdate = now;
    }
    void stop() { running = false; }
    ClockSnapshot snapshot() { return new ClockSnapshot(enabled, running, whiteNanos, blackNanos, active); }
}
