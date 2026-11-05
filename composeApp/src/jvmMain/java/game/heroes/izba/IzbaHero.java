package game.heroes.izba;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbilityUse;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.heroes.compat.LegacyPreview;
import game.state.PieceState;
import game.summoning.SummonMechanic;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.Position;

/** Призыв предлагает операцию; размещение и созревание принадлежат модулю призыва. */
public final class IzbaHero extends AbstractHero implements LegacyPreview {
    private final IzbaRules rules;

    public IzbaHero(IzbaRules rules) {
        super(IzbaDefinition.INSTANCE, IzbaAbility.getAbilities(), IzbaAbility.SUMMON_YAGA,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return IzbaAbilities.preview(actor, ability, rules);
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                                    AbilityRequest request) {
        return castEffect(actor, request.borrowed()).withOperations(new SummonMechanic.Cast(actor));
    }

    HeroEffect detachedPreview(PieceState actor, Abilities ability, boolean borrowed) {
        var result = IzbaAbilities.availability(actor, ability, rules, borrowed);
        return result.isSuccess() ? castEffect(actor, borrowed).applyCost(actor, AbilityUse.of(HeroType.IZBA, borrowed))
                : HeroEffect.rejected(result);
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
