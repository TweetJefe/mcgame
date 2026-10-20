package game.moves;

import model.Figure;
import model.FigureColor;
import model.Position;

/**
 * One prepared capture, credited only when Game commits the complete action.
 */
public record Capture(long actorId, long victimId, FigureColor creditedSide, Figure victim,
                      Position square, boolean reaction, Cause cause) {
    public enum Cause {MOVE, PUDGE, BOG, ARES, MINE, BLAST, LIGHTNING }
    public boolean rewarded(){return victim.getColor()!=creditedSide && actorId!=victimId;}

    public Capture(long actorId, long victimId, FigureColor creditedSide, Figure victim, Position square, boolean reaction) {
        this(actorId, victimId, creditedSide, victim, square, reaction, reaction ? Cause.PUDGE : Cause.MOVE);
    }
}
