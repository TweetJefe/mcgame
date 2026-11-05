package game.heroes.pudge;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import model.Position;
import model.figures.King;
import rules.moba.TerrainGeometry;

/** Pudge's targeting and effects, sharing availability and capture charging with AbstractHero. */
public final class PudgeHero extends AbstractHero {
    private final PudgeRules rules;

    public PudgeHero(PudgeRules rules) {
        super(PudgeDefinition.INSTANCE, PudgeAbility.getAbilities(), PudgeAbility.SWALLOW,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    @Override
    public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                  PieceState target, AbilityRequest request) {
        return request.ability() == PudgeAbility.HOOK
                ? "Пудж: хук " + cell(request.target()) + " → " + cell(after.pieceById(target.id()).position())
                : "Пудж: проглотил " + target.type().getSimpleName() + " на " + cell(request.target());
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }

    @Override
    protected MoveResult availabilitySpecific(PieceState actor, Abilities ability) {
        return ability == ultimateAbility() && actor.storedPieceId() != null
                ? AbilityRejection.ALREADY_SWALLOWED : Success.SUCCESS;
    }

    @Override
    protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                           AbilityRequest request) {
        return calculateEffect(context, actor, target, request.ability(), square -> context.pieceAt(square) == null);
    }

    private HeroEffect calculateEffect(HeroActionContext context, PieceState actor, PieceState target,
                                       Abilities ability, Predicate<Position> vacant) {
        if (target == null || target.status() != PieceState.Status.BOARD || target.id() == actor.id())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);

        int dr = target.position().getRow() - actor.position().getRow();
        int dc = target.position().getColumn() - actor.position().getColumn();
        int distance = Math.max(Math.abs(dr), Math.abs(dc));
        if (ability == PudgeAbility.HOOK) {
            boolean straight = dr == 0 || dc == 0;
            boolean diagonal = Math.abs(dr) == Math.abs(dc);
            if (distance > 3 || (!straight && !(rules.hookDiagonals() && diagonal)))
                return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);

            int rowStep = Integer.signum(dr), columnStep = Integer.signum(dc);
            for (int step = 1; step < distance; step++) {
                Position cell = new Position(actor.position().getRow() + rowStep * step,
                        actor.position().getColumn() + columnStep * step);
                if (!vacant.test(cell))
                    return HeroEffect.rejected(AbilityRejection.ABILITY_BLOCKED);
            }
            Position landing = new Position(actor.position().getRow() + rowStep,
                    actor.position().getColumn() + columnStep);
            if (TerrainGeometry.blocksRay(target.position(), landing,
                    context.terrain().hostileSquares(context.pieces(), target.color())))
                return HeroEffect.rejected(AbilityRejection.MUD_BLOCKED);
            return HeroEffect.success(Map.of(target.id(), target.placed(landing, target.type())))
                    .withCost(AbilityCost.cooldown(rules.hookCooldown()));
        }

        if (distance != 1 || (!rules.swallowDiagonals() && Math.abs(dr) + Math.abs(dc) != 1))
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        if (target.color() != actor.color()) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        return HeroEffect.success(Map.of(target.id(), target.hidden(PieceState.Status.SWALLOWED), actor.id(),
                actor.withAbilityState(actor.cooldown(), actor.ultimateCharge(), target.id())))
                .withCost(AbilityCost.ultimate(ultimateCaptures()));
    }

}
