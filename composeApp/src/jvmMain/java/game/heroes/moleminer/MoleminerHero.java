package game.heroes.moleminer;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbilityUse;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.Position;

/** Геометрия прыжка и стоимость активации; взрыв и мины применяются MineMechanic. */
public final class MoleminerHero extends AbstractHero implements LegacyPreview {
    private final MoleminerRules rules;

    public MoleminerHero(MoleminerRules rules) {
        super(MoleminerDefinition.INSTANCE, MoleminerAbility.getAbilities(), MoleminerAbility.DETONATE,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                                    AbilityRequest request) {
        if (request.ability() == MoleminerAbility.PLANT_MINE && context.mines().cells().containsKey(actor.position()))
            return HeroEffect.rejected(AbilityRejection.TERRAIN_OCCUPIED);
        HeroEffect effect = calculate(actor, request.target(), request.ability());
        if (!effect.result().isSuccess()) return effect;
        return request.ability() == MoleminerAbility.PLANT_MINE
                ? effect.withOperations(new MineMechanic.Plant(actor.id(), actor.position()))
                : effect.withOperations(new MineMechanic.Blast(actor.id(), request.target()));
    }

    HeroEffect detachedPreview(PieceState actor, Position square, Abilities ability, boolean borrowed) {
        return calculate(actor, square, ability).applyCost(actor, AbilityUse.of(HeroType.MOLEMINER, borrowed));
    }

    private HeroEffect calculate(PieceState actor, Position square, Abilities ability) {
        if (ability == MoleminerAbility.PLANT_MINE)
            return HeroEffect.success(Map.of(actor.id(), actor)).withCost(AbilityCost.cooldown(rules.mineCooldown()));
        if (ability != MoleminerAbility.DETONATE) return HeroEffect.rejected(AbilityRejection.NO_ABILITY);
        if (Math.max(Math.abs(square.getRow() - actor.position().getRow()),
                Math.abs(square.getColumn() - actor.position().getColumn())) > 3)
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        return HeroEffect.success(Map.of(actor.id(), actor.placed(square, actor.type())))
                .withCost(AbilityCost.ultimate(ultimateCaptures()));
    }

    /** Возможные левые нижние углы области взрыва, в прежнем порядке для выбора RNG. */
    public static List<Position> corners(Position landing) {
        var result = new ArrayList<Position>();
        for (int row = landing.getRow() - 1; row <= landing.getRow(); row++)
            for (int column = landing.getColumn() - 1; column <= landing.getColumn(); column++)
                if (row >= 0 && row < 7 && column >= 0 && column < 7) result.add(new Position(row, column));
        return List.copyOf(result);
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                          PieceState target, AbilityRequest request) {
        return request.ability() == MoleminerAbility.PLANT_MINE ? "Moleminer: заложил мину"
                : "Moleminer: прыжок и взрыв на " + cell(request.target());
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
