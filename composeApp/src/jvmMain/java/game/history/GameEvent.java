package game.history;


import model.FigureColor;
import game.heroes.Ability;
import model.Position;

/** Presentation-independent action history, including reactions and explicit turn boundaries. */
public record GameEvent(long sequence, long turn, Kind kind, FigureColor color, Long actorId, Long targetId,
                        Position from, Position to, String message, Ability copiedAbility, boolean borrowed) {
    public GameEvent(long sequence,long turn,Kind kind,FigureColor color,Long actorId,Long targetId,Position from,Position to,String message) {
        this(sequence,turn,kind,color,actorId,targetId,from,to,message,null,false);
    }
    public enum Kind { MOVE, HOOK, SWALLOW, COUNTER_CAPTURE, END_TURN, LASER, TELEPORT, BOUNTY, STEAL, MUD, AMBUSH, BOG_CAPTURE, SUMMON_YAGA, YAGA_BIRTH, YAGA_MATURE, REPLACE, DISPLACE, COPY_SPELL }
}
