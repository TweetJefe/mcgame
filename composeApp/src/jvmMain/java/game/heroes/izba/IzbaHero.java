package game.heroes.izba;

import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.state.PieceState;
import game.summoning.SummonMechanic;
import java.util.Map;
import java.util.Objects;

/** Призыв предлагает операцию; размещение и созревание принадлежат модулю призыва. */
public final class IzbaHero extends AbstractHero {
    private final IzbaRules rules;

    public IzbaHero(IzbaRules rules) {
        super(IzbaDefinition.INSTANCE, IzbaAbility.getAbilities(), IzbaAbility.SUMMON_YAGA,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                                    AbilityRequest request) {
        return castEffect(actor, request.borrowed()).withOperations(new SummonMechanic.Cast(actor));
    }

    private HeroEffect castEffect(PieceState actor, boolean borrowed) {
        HeroEffect effect = HeroEffect.success(Map.of(actor.id(), borrowed ? actor
                : actor.withAbilityState(actor.cooldown(), actor.ultimateCharge(), null)));
        return effect.withCost(new AbilityCost(0, ultimateCaptures()));
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                          PieceState target, AbilityRequest request) {
        return "Изба: призвала Бабу-ягу";
    }
}
