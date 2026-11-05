package game.heroes.bountycrow;

import game.economy.EconomyMechanic;
import game.economy.GoldRules;
import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.state.PieceState;
import java.util.Map;
import java.util.Objects;
import model.Position;
import model.figures.King;

/** Неизменяемая стратегия: геометрия и эффекты героя, общие проверки предоставляет база. */
public final class BountyCrowHero extends AbstractHero {
    private final BountyCrowRules rules;

    public BountyCrowHero(BountyCrowRules rules) {
        super(BountyCrowDefinition.INSTANCE, BountyCrowAbility.getAbilities(), BountyCrowAbility.STEAL,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                    PieceState target, AbilityRequest request) {
        HeroEffect effect = effect(actor, target, request.ability());
        if (!effect.result().isSuccess()) return effect;
        return request.ability() == BountyCrowAbility.BOUNTY
                ? effect.withOperations(new EconomyMechanic.Mark(actor, target))
                : effect.withOperations(new EconomyMechanic.Steal(actor.id()));
    }

    private HeroEffect effect(PieceState actor, PieceState target, Abilities ability) {
        if (ability == BountyCrowAbility.STEAL)
            return HeroEffect.success(Map.of(actor.id(), actor)).withCost(AbilityCost.ultimate(ultimateCaptures()));
        if (target == null || target.status() != PieceState.Status.BOARD || target.color() == actor.color())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
        return HeroEffect.success(Map.of(actor.id(), actor)).withCost(AbilityCost.cooldown(rules.bountyCooldown()));
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                           PieceState target, AbilityRequest request) {
        return request.ability() == BountyCrowAbility.BOUNTY
                ? "Bounty Crow: награда +1,5 золота за " + target.type().getSimpleName() + " на " + cell(request.target())
                : "Bounty Crow: украл " + GoldRules.format(after.economy().history().getLast().totalHalves()) + " золота";
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
