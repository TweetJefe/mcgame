package game.heroes.faun;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbilityUse;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.Position;

/** Неизменяемая стратегия: геометрия и эффекты героя, общие проверки предоставляет база. */
public final class FaunHero extends AbstractHero implements LegacyPreview {
    private final FaunRules rules;

    public FaunHero(FaunRules rules) {
        super(FaunDefinition.INSTANCE, FaunAbility.getAbilities(), FaunAbility.INVISIBILITY,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return FaunAbilities.preview(actor, square, ability, rules);
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                    PieceState target, AbilityRequest request) {
        return effect(actor, request.target(), request.ability());
    }

    private HeroEffect effect(PieceState actor, Position square, Abilities ability) {
        if (ability == FaunAbility.CLOUD) {
            if (square == null || Math.max(Math.abs(square.getRow() - actor.position().getRow()),
                    Math.abs(square.getColumn() - actor.position().getColumn())) > 4)
                return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
            return HeroEffect.success(Map.of(actor.id(), actor)).withCost(AbilityCost.cooldown(rules.cloudCooldown()));
        }
        return HeroEffect.success(Map.of(actor.id(), actor)).withCost(AbilityCost.ultimate(ultimateCaptures()));
    }

    /** Старый detached-вход возвращает только геометрию и оплаченное состояние, без операций механик. */
    HeroEffect legacyPreview(PieceState actor, Position square, Abilities ability, boolean borrowed) {
        return effect(actor, square, ability).applyCost(actor, AbilityUse.of(type(), borrowed));
    }

    /** Сохранение семантики старого публичного помощника; игровой путь использует afterCapture базы. */
    PieceState legacyAfterCapture(PieceState actor) {
        return actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                           PieceState target, AbilityRequest request) {
        return request.ability() == FaunAbility.CLOUD
                ? "Faun: туча с центром " + cell(request.target()) : "Faun: стал невидимым";
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
