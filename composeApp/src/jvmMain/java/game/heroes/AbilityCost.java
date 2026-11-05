package game.heroes;

import game.state.PieceState;
import java.util.Objects;

/** Стоимость применения; null у cooldown означает сохранить прежний кулдаун. */
public record AbilityCost(Integer cooldown, int ultimateCharge) {
    public static final AbilityCost NONE = new AbilityCost(null, 0);

    public AbilityCost {
        if ((cooldown != null && cooldown < 0) || ultimateCharge < 0)
            throw new IllegalArgumentException("Negative ability cost");
    }

    public static AbilityCost cooldown(int turns) { return new AbilityCost(turns, 0); }
    public static AbilityCost ultimate(int charge) { return new AbilityCost(null, charge); }

    public PieceState apply(PieceState actor, AbilityUse use) {
        Objects.requireNonNull(use, "use");
        if (use.borrowed() || equals(NONE)) return actor;
        if (actor.ultimateCharge() < ultimateCharge)
            throw new IllegalArgumentException("Ability cost exceeds available charge");
        return actor.withAbilityState(cooldown == null ? actor.cooldown() : cooldown,
                actor.ultimateCharge() - ultimateCharge, actor.storedPieceId());
    }
}
