package game.heroes.phoenix;

import game.heroes.AbstractHero;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.Objects;

/** Пассивный возврат и созревание обслуживаются локальным модулем. */
public final class PhoenixHero extends AbstractHero {
    public PhoenixHero(PhoenixRules rules) {
        super(PhoenixDefinition.INSTANCE, PhoenixAbility.getAbilities(), PhoenixAbility.REBIRTH,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
    }
    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                    PieceState target, AbilityRequest request) {
        if (context.state(PhoenixMechanic.KEY).eggs().containsKey(actor.id()))
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        return HeroEffect.empty().withOperations(new PhoenixMechanic.Incubate(actor.id()))
                .withCost(AbilityCost.ultimate(ultimateCaptures()));
    }
    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        return "Phoenix: превращение в яйцо";
    }
}
