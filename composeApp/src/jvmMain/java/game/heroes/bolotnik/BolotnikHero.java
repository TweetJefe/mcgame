package game.heroes.bolotnik;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.state.PieceState;
import game.terrain.TerrainMechanic;
import java.util.Map;
import java.util.Objects;
import model.Position;
import model.figures.King;

/** Неизменяемая стратегия: геометрия и эффекты героя, общие проверки предоставляет база. */
public final class BolotnikHero extends AbstractHero {
    private final BolotnikRules rules;

    public BolotnikHero(BolotnikRules rules) {
        super(BolotnikDefinition.INSTANCE, BolotnikAbility.getAbilities(), BolotnikAbility.AMBUSH,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                    PieceState target, AbilityRequest request) {
        HeroEffect effect = effect(actor, target, request.target(), request.ability());
        return effect.result().isSuccess()
                ? effect.withOperations(new TerrainMechanic.Place(actor, request.target(), request.ability())) : effect;
    }

    private HeroEffect effect(PieceState actor, PieceState target, Position square, Abilities ability) {
        if (ability == BolotnikAbility.AMBUSH)
            return HeroEffect.success(Map.of(actor.id(), actor.hidden(PieceState.Status.HIDDEN_BOG)))
                    .withCost(AbilityCost.ultimate(ultimateCaptures()));
        if (target != null && target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
        if (target != null && target.color() != actor.color())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        int distance = Math.max(Math.abs(square.getRow() - actor.position().getRow()),
                Math.abs(square.getColumn() - actor.position().getColumn()));
        if (distance > 4) return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        return HeroEffect.success(Map.of(actor.id(), actor)).withCost(AbilityCost.cooldown(rules.mudCooldown()));
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                           PieceState target, AbilityRequest request) {
        return request.ability() == BolotnikAbility.MUD
                ? "Bolotnik: грязь на " + cell(request.target())
                : "Bolotnik: спрятался в болоте у " + cell(actor.position());
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
