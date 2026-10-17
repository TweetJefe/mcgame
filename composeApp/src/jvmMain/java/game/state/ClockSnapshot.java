package game.state;


import model.FigureColor;

/** Immutable clock state; nanos retain subsecond accuracy for restoration and tests. */
public record ClockSnapshot(boolean enabled, boolean running, long whiteNanos, long blackNanos,
                            FigureColor activeColor) {
    public boolean isEnabled() { return enabled; }
    public boolean isRunning() { return running; }
    public long getWhiteNanos() { return whiteNanos; }
    public long getBlackNanos() { return blackNanos; }
    public long getWhiteMillis() { return (whiteNanos + 999_999L) / 1_000_000L; }
    public long getBlackMillis() { return (blackNanos + 999_999L) / 1_000_000L; }
    public FigureColor getActiveColor() { return activeColor; }
}
