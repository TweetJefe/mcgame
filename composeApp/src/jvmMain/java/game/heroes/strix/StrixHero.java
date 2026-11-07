package game.heroes.strix;

import game.heroes.AbstractHero;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbilityEventDetails;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.history.PositionHistoryMechanic;
import game.moves.AbilityRejection;
import game.state.PieceState;
import game.turns.TurnFlowMechanic;
import java.util.Map;
import java.util.Objects;
import model.Position;

/** Возврат меняет координаты; очередь и бюджет следующего хода принадлежат общему слою. */
public final class StrixHero extends AbstractHero {
    private final StrixRules rules;

    public StrixHero(StrixRules rules) {
        super(StrixDefinition.INSTANCE, StrixAbility.getAbilities(), StrixAbility.TIME_SKIP,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                     PieceState target, AbilityRequest request) {
        if (request.ability() == StrixAbility.TIME_SKIP) {
            if (context.state(TurnFlowMechanic.KEY).pending() != null)
                return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            return HeroEffect.empty().withOperations(new TurnFlowMechanic.RequestSkip(actor.id()))
                    .withCost(AbilityCost.ultimate(ultimateCaptures()));
        }
        if (target == null) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (distance(actor.position(), target.position()) > 2)
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        Position destination = context.state(PositionHistoryMechanic.KEY)
                .twoCompletedTurnsAgo(target, context.currentTurn());
        if (destination == null || destination.equals(target.position()) || context.pieceAt(destination) != null)
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        return HeroEffect.success(Map.of(target.id(), target.placed(destination, target.type())))
                .withOperations(new StrixMechanic.Rewind(actor.id(), target.id(), target.position(), destination))
                .withCost(AbilityCost.cooldown(rules.rewindCooldown()));
    }

    @Override public AbilityEventDetails abilityEventDetails(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        if (request.ability() != StrixAbility.REWIND)
            return super.abilityEventDetails(before, after, actor, target, request);
        Position destination = before.state(PositionHistoryMechanic.KEY)
                .twoCompletedTurnsAgo(target, before.currentTurn());
        return new AbilityEventDetails(target.id(), destination, null);
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        return request.ability() == StrixAbility.REWIND ? "Strix: возврат прежней позиции"
                : "Strix: следующий ход соперника пропускается";
    }

    static int distance(Position a, Position b) {
        return Math.max(Math.abs(a.getRow() - b.getRow()), Math.abs(a.getColumn() - b.getColumn()));
    }
}
