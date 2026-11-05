package game.history;

import game.heroes.Abilities;
import game.mechanics.MechanicEvent;
import game.mechanics.MechanicEventType;
import game.mechanics.compat.HistoricalEvents;
import java.util.Objects;
import model.FigureColor;
import model.Position;

/** Action category and canonical ability identity are independent of each other and presentation. */
public record GameEvent(long sequence, long turn, Kind kind, FigureColor color, Long actorId, Long targetId,
                        Position from, Position to, String message, Abilities ability,
                        Abilities copiedAbility, boolean borrowed, MechanicEvent<?> effect) {
    public GameEvent {
        Objects.requireNonNull(kind, "kind");
        if (HistoricalEvents.isLegacy(kind)) {
            if (effect != null) throw new IllegalArgumentException("Two effect identities");
            effect = HistoricalEvents.effect(kind, actorId, targetId, from, to);
            kind = Kind.MECHANIC_EFFECT;
        }
        if ((kind == Kind.MECHANIC_EFFECT) != (effect != null))
            throw new IllegalArgumentException("Mechanic payload differs from event category");
        if ((kind == Kind.ABILITY_USED) != (ability != null))
            throw new IllegalArgumentException("Ability identity differs from event category");
        if (ability != null && (ability.id() == null || ability.id().isBlank()))
            throw new IllegalArgumentException("Blank event ability ID");
        if (kind != Kind.ABILITY_USED && (copiedAbility != null || borrowed))
            throw new IllegalArgumentException("Spell metadata on non-active event");
    }

    public GameEvent(long sequence, long turn, Kind kind, FigureColor color, Long actorId, Long targetId,
                     Position from, Position to, String message, Abilities ability,
                     Abilities copiedAbility, boolean borrowed) {
        this(sequence, turn, kind, color, actorId, targetId, from, to, message, ability, copiedAbility, borrowed, null);
    }

    public static GameEvent mechanicEffect(long sequence, long turn, FigureColor color, Long actorId,
            Long targetId, Position from, Position to, String message, MechanicEvent<?> effect) {
        return new GameEvent(sequence, turn, Kind.MECHANIC_EFFECT, color, actorId, targetId,
                from, to, message, null, null, false, effect);
    }

    public boolean isEffect(MechanicEventType<?> type) {
        return effect != null && effect.type() == type;
    }

    /** Только адаптеры прежних форматов и исторических реконструкторов. */
    public Kind legacyKind() {
        Kind legacy = HistoricalEvents.kind(effect);
        return legacy == null ? kind : legacy;
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

    /** Общие категории; исторические имена реакций служат только входными псевдонимами конструктора. */
    public enum Kind {
        ABILITY_USED,
        MECHANIC_EFFECT,
        CAPTURE,
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
