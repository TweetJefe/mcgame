package game.heroes.lumeshard;

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
import game.state.SquareColor;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.Position;
import model.figures.King;

/** Неизменяемая стратегия: геометрия и эффекты героя, общие проверки предоставляет база. */
public final class LumeshardHero extends AbstractHero implements LegacyPreview {
    private final LumeshardRules rules;

    public LumeshardHero(LumeshardRules rules) {
        super(LumeshardDefinition.INSTANCE, LumeshardAbility.getAbilities(), LumeshardAbility.TELEPORT,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return LumeshardAbilities.preview(field, actor, target, square, ability, rules);
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                    PieceState target, AbilityRequest request) {
        return effect(actor, target, request.target(), request.ability());
    }

    private HeroEffect effect(PieceState actor, PieceState target, Position square, Abilities ability) {
        if (ability == LumeshardAbility.TELEPORT) {
            if (target != null) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            if (SquareColor.at(square) != actor.startingColor())
                return HeroEffect.rejected(AbilityRejection.WRONG_SQUARE_COLOR);
            return HeroEffect.success(Map.of(actor.id(), actor.placed(square, actor.type())))
                    .withCost(AbilityCost.ultimate(ultimateCaptures()));
        }
        if (target == null || target.status() != PieceState.Status.BOARD || target.color() == actor.color())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
        int distance = Math.max(Math.abs(square.getRow() - actor.position().getRow()),
                Math.abs(square.getColumn() - actor.position().getColumn()));
        if (distance > 3) return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        return HeroEffect.success(Map.of(actor.id(), actor,
                target.id(), target.withBlindness(Math.max(target.blindTurns(), rules.blindnessTurns()))))
                .withCost(AbilityCost.cooldown(rules.laserCooldown()));
    }

    /** Старый detached-вход возвращает только геометрию и оплаченное состояние, без операций механик. */
    HeroEffect legacyPreview(PieceState actor, PieceState target, Position square, Abilities ability, boolean borrowed) {
        return effect(actor, target, square, ability).applyCost(actor, AbilityUse.of(type(), borrowed));
    }

    /** Сохранение семантики старого публичного помощника; игровой путь использует afterCapture базы. */
    PieceState legacyAfterCapture(PieceState actor) {
        if (rules.ultimateCaptures() == 0) return actor;
        return actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                           PieceState target, AbilityRequest request) {
        return request.ability() == LumeshardAbility.LASER
                ? "Lumeshard: ослепил " + target.type().getSimpleName() + " на " + cell(request.target())
                : "Lumeshard: телепорт " + cell(actor.position()) + " → " + cell(request.target());
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
