package game.heroes.sunwukong;

import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.Objects;
import model.figures.Pawn;

/** Стоимость принадлежит шаблону; случайность и длительное состояние — модулю механики. */
public final class SunWukongHero extends AbstractHero {
    private final SunWukongRules rules;

    public SunWukongHero(SunWukongRules rules) {
        super(SunWukongDefinition.INSTANCE, SunWukongAbility.getAbilities(), SunWukongAbility.CLONE,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                     PieceState target, AbilityRequest request) {
        if (request.ability() == SunWukongAbility.SHAPESHIFT) {
            if (context.state(SunWukongMechanic.KEY).forms().containsKey(actor.id()))
                return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            return HeroEffect.empty().withOperations(new SunWukongMechanic.Shapeshift(actor.id(), request.borrowed()))
                    .withCost(AbilityCost.cooldown(rules.shapeshiftCooldown()));
        }
        if (target == null || target.hero() != HeroType.NONE || target.type() != Pawn.class
                || target.color() != actor.color())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        return HeroEffect.empty().withOperations(new SunWukongMechanic.Clone(actor.id(), target.id()))
                .withCost(AbilityCost.ultimate(ultimateCaptures()));
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        return request.ability() == SunWukongAbility.SHAPESHIFT
                ? "Sun Wukong: временная форма — " + WukongState.Body.of(after.pieceById(actor.id()).type()).displayName()
                : "Sun Wukong: союзная пешка стала клоном";
    }
}
