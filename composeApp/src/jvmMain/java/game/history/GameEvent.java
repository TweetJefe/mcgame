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
    public enum Kind { YARN, CLAY_POT, YARN_STUN, GRAPPLE, EVOLVE, CHAIN_LIGHTNING, THUNDERSTORM, LIGHTNING_HIT, LIGHTNING_RESET, LIGHTNING_DISCHARGE, LIGHTNING_CAPTURE, PLANT_MINE, DETONATE, MINE_TRIGGER, MINE_CAPTURE, BLAST_AREA, BLAST_CAPTURE, BLAST_DEATH, BOX, ILLUSION, ILLUSION_HIT, MOVE, HOOK, SWALLOW, COUNTER_CAPTURE, END_TURN, LASER, TELEPORT, BOUNTY, STEAL, MUD, AMBUSH, BOG_CAPTURE, SUMMON_YAGA, YAGA_BIRTH, YAGA_MATURE, REPLACE, DISPLACE, COPY_SPELL, CLOUD, INVISIBILITY, SPEAR, STUN, ARES_REVENGE }
}
