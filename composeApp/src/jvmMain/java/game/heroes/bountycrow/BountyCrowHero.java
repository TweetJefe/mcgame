package game.heroes.bountycrow;

import game.economy.EconomyMechanic;
import game.economy.GoldRules;
import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbilityUse;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.state.PieceState;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.Position;
import model.figures.King;

/** Неизменяемая стратегия: геометрия и эффекты героя, общие проверки предоставляет база. */
public final class BountyCrowHero extends AbstractHero implements LegacyPreview {
    private final BountyCrowRules rules;

    public BountyCrowHero(BountyCrowRules rules) {
        super(BountyCrowDefinition.INSTANCE, BountyCrowAbility.getAbilities(), BountyCrowAbility.STEAL,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return BountyCrowAbilities.preview(actor, target, ability, rules);
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

    /** Старый detached-вход возвращает только геометрию и оплаченное состояние, без операций механик. */
    HeroEffect legacyPreview(PieceState actor, PieceState target, Abilities ability, boolean borrowed) {
        return effect(actor, target, ability).applyCost(actor, AbilityUse.of(type(), borrowed));
    }

    /** Сохранение семантики старого публичного помощника; игровой путь использует afterCapture базы. */
    PieceState legacyAfterCapture(PieceState actor) {
        if (rules.ultimateCaptures() == 0) return actor;
        return actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
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
