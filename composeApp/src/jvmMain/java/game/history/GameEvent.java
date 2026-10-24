package game.history;

import game.heroes.Abilities;
import model.FigureColor;
import model.Position;

import java.util.Objects;

/** Action category and canonical ability identity are independent of each other and presentation. */
public record GameEvent(long sequence, long turn, Kind kind, FigureColor color, Long actorId, Long targetId,
                        Position from, Position to, String message, Abilities ability,
                        Abilities copiedAbility, boolean borrowed) {
    public GameEvent {
        Objects.requireNonNull(kind, "kind");
        if ((kind == Kind.ABILITY_USED) != (ability != null))
            throw new IllegalArgumentException("Ability identity differs from event category");
        if (ability != null && (ability.id() == null || ability.id().isBlank()))
            throw new IllegalArgumentException("Blank event ability ID");
        if (kind != Kind.ABILITY_USED && (copiedAbility != null || borrowed))
            throw new IllegalArgumentException("Spell metadata on non-active event");
    }

    /** Non-ability action or effect. */
    public GameEvent(long sequence, long turn, Kind kind, FigureColor color, Long actorId, Long targetId,
                     Position from, Position to, String message) {
        this(sequence, turn, kind, color, actorId, targetId, from, to, message, null, null, false);
    }

    public static GameEvent abilityUsed(long sequence, long turn, Abilities ability, FigureColor color,
                                        Long actorId, Long targetId, Position from, Position to, String message,
                                        Abilities copiedAbility, boolean borrowed) {
        return new GameEvent(sequence, turn, Kind.ABILITY_USED, color, actorId, targetId, from, to, message,
                Objects.requireNonNull(ability, "ability"), copiedAbility, borrowed);
    }

    public static GameEvent abilityUsed(long sequence, long turn, Abilities ability, FigureColor color,
                                        Long actorId, Long targetId, Position from, Position to, String message) {
        return abilityUsed(sequence, turn, ability, color, actorId, targetId, from, to, message, null, false);
    }

    public String abilityId() { return ability == null ? null : ability.id(); }
    public boolean isAbility(Abilities expected) { return ability != null && ability == expected; }

    /** Active abilities have one category; remaining entries describe existing actions and effects. */
    public enum Kind {
        ABILITY_USED,
        YARN_STUN,
        LIGHTNING_HIT,
        LIGHTNING_RESET,
        LIGHTNING_DISCHARGE,
        LIGHTNING_CAPTURE,
        MINE_TRIGGER,
        MINE_CAPTURE,
        BLAST_AREA,
        BLAST_CAPTURE,
        BLAST_DEATH,
        ILLUSION_HIT,
        MOVE,
        COUNTER_CAPTURE,
        END_TURN,
        BOG_CAPTURE,
        YAGA_BIRTH,
        YAGA_MATURE,
        REPLACE,
        STUN,
        ARES_REVENGE
    }
}
