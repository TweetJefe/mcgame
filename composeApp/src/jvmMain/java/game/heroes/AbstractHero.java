package game.heroes;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.MoveResult;
import game.state.PieceState;
import model.Position;

import java.util.List;
import java.util.Objects;

/**
 * Template methods for shared availability checks and previews, plus capture charge accounting.
 * Subclasses keep their rules immutable and implement only hero-specific checks and effects.
 */
public abstract class AbstractHero implements Hero {
    private final HeroType type;
    private final List<Abilities> abilities;
    private final Abilities ultimateAbility;
    private final int ultimateCaptures;

    /**
     * @param ultimateAbility the charge-based ability, or null when there is no active ultimate
     * @param ultimateCaptures charge threshold and cap; zero makes the ultimate free
     *                         and disables capture charging. May also cap a passive's charge
     *                         when ultimateAbility is null.
     */
    protected AbstractHero(HeroType type, List<? extends Abilities> abilities, Abilities ultimateAbility,
                           int ultimateCaptures) {
        this.type = Objects.requireNonNull(type, "type");
        this.abilities = List.copyOf(abilities);
        if (ultimateCaptures < 0) throw new IllegalArgumentException("Negative ultimate capture threshold");
        if (ultimateAbility != null && !this.abilities.contains(ultimateAbility))
            throw new IllegalArgumentException("Ultimate must be one of the hero's abilities");
        this.ultimateAbility = ultimateAbility;
        this.ultimateCaptures = ultimateCaptures;
    }

    @Override
    public final HeroType type() {
        return type;
    }

    @Override
    public final List<Abilities> abilities() {
        return abilities;
    }

    /** Charge threshold available to subclasses when calculating ultimate costs. */
    protected final int ultimateCaptures() {
        return ultimateCaptures;
    }

    /** Returns the charge-based ability, or null when there is no active ultimate. */
    protected final Abilities ultimateAbility() {
        return ultimateAbility;
    }

    @Override
    public final MoveResult availability(PieceState actor, Abilities ability) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor != null && actor.isPotted()) return StatusRejection.POTTED;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != type || !abilities.contains(ability)) return AbilityRejection.NO_ABILITY;
        if (ability != ultimateAbility && actor.cooldown() > 0) return AbilityRejection.ABILITY_COOLDOWN;
        MoveResult specific = availabilitySpecific(actor, ability);
        if (!specific.isSuccess()) return specific;
        if (ability == ultimateAbility && actor.ultimateCharge() < ultimateCaptures)
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    /** Runs after identity and cooldown checks, before the ultimate charge check. */
    protected MoveResult availabilitySpecific(PieceState actor, Abilities ability) {
        return Success.SUCCESS;
    }

    @Override
    public final HeroEffect preview(HeroActionContext context, PieceState actor, PieceState target,
                                    Position square, Abilities ability) {
        Objects.requireNonNull(context, "context");
        MoveResult ready = availability(actor, ability);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return previewAvailable(context, actor, target, square, ability);
    }

    /**
     * Validates targeting and calculates an effect for an available ability. The subclass
     * includes cooldown and charge costs in the proposed updates only when targeting succeeds.
     */
    protected abstract HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                   PieceState target, Position square, Abilities ability);

    /**
     * Default capture charging preserves cooldown, stored pieces and all other actor state.
     * Heroes with special capture rules may override this method and reuse it via super.
     */
    @Override
    public PieceState afterCapture(PieceState actor) {
        Objects.requireNonNull(actor, "actor");
        if (actor.isPotted() || actor.hero() != type || ultimateCaptures == 0) return actor;
        int charge = (int) Math.min(ultimateCaptures, (long) actor.ultimateCharge() + 1);
        if (charge == actor.ultimateCharge()) return actor;
        return actor.withAbilityState(actor.cooldown(), charge, actor.storedPieceId());
    }
}
