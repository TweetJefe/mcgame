package game.heroes.pudge;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbilityUse;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.moves.Capture;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import model.Field;
import model.Figure;
import model.Position;
import model.figures.Empty;
import model.figures.King;
import rules.moba.TerrainGeometry;

/** Pudge's targeting and effects, sharing availability and capture charging with AbstractHero. */
public final class PudgeHero extends AbstractHero implements LegacyPreview {
    private final PudgeRules rules;

    public PudgeHero(PudgeRules rules) {
        super(PudgeDefinition.INSTANCE, PudgeAbility.getAbilities(), PudgeAbility.SWALLOW,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return PudgeAbilities.preview(field, actor, target, ability, rules);
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

    /** Старый публичный расчёт использует занятость Field, даже если переданы только исполнитель и цель. */
    HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Abilities ability) {
        var pieces = new HashMap<Long, PieceState>();
        if (actor != null) pieces.put(actor.id(), actor);
        if (target != null) pieces.put(target.id(), target);
        var context = new HeroActionContext(field, pieces, 0);
        MoveResult ready = availability(actor, ability);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return calculateEffect(context, actor, target, ability,
                square -> field.getFigureByPosition(square) instanceof Empty)
                .applyCost(actor, AbilityUse.of(type(), false));
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

    /** Preserves the original standalone callback API; game reactions use onCaptured. */
    HeroEffect legacyCaptureReaction(Field field, Map<Long, PieceState> pieces, PieceState victim,
                                             long captorId, Position square, UnaryOperator<PieceState> onCapture) {
        var working = new HashMap<>(pieces);
        var updates = new HashMap<Long, PieceState>();
        var captures = new ArrayList<Capture>();
        Long passenger = victim.storedPieceId();
        long occupantId = captorId;
        // A loaded attacking Pudge releases its own ally after being counter-captured.
        while (passenger != null) {
            PieceState occupant = working.get(occupantId);
            if (occupant.type() == King.class) return HeroEffect.rejected(MovementRejection.KING_IN_CHECK);
            PieceState released = working.get(passenger);
            if (released == null || released.status() != PieceState.Status.SWALLOWED)
                return HeroEffect.rejected(MovementRejection.INVALID_BOARD);
            Long nextPassenger = occupant.storedPieceId();
            Figure eaten = field.createFigure(occupant.type(), square, occupant.color());
            captures.add(new Capture(released.id(), occupant.id(), released.color(), eaten, square, true));
            PieceState dead = occupant.hidden(PieceState.Status.CAPTURED);
            PieceState returned = onCapture.apply(released.placed(square, released.type()));
            working.put(dead.id(), dead);
            updates.put(dead.id(), dead);
            working.put(returned.id(), returned);
            updates.put(returned.id(), returned);
            passenger = nextPassenger;
            occupantId = returned.id();
        }
        return new HeroEffect(Success.SUCCESS, updates, captures);
    }
}
