package game;

import model.FigureColor;
import model.Position;

/** Presentation-independent action history, including reactions and explicit turn boundaries. */
public record GameEvent(long sequence, long turn, Kind kind, FigureColor color, Long actorId, Long targetId,
                        Position from, Position to, String message) {
    public enum Kind { MOVE, HOOK, SWALLOW, COUNTER_CAPTURE, END_TURN }
}
