package game.heroes.chronomlin;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.Success;
import game.state.PieceState;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import model.Field;
import model.Figure;
import model.Position;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import rules.moba.TerrainGeometry;

/** Притяжение и эволюция сохраняют личность, инвентарь и длительные воздействия исполнителя. */
public final class ChronomlinHero extends AbstractHero implements LegacyPreview {
    private final ChronomlinRules rules;

    public ChronomlinHero(ChronomlinRules rules) {
        super(ChronomlinDefinition.INSTANCE, ChronomlinAbility.getAbilities(), ChronomlinAbility.EVOLVE,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return ChronomlinAbilities.preview(field, actor, target, ability, rules, false);
    }

    @Override
    protected MoveResult availabilitySpecific(PieceState actor, Abilities ability) {
        return ability == ChronomlinAbility.EVOLVE && nextType(actor.type()) == null
                ? AbilityRejection.MAX_EVOLUTION : Success.SUCCESS;
    }

    @Override
    protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                           AbilityRequest request) {
        return geometryAvailable(context, actor, target, request.ability(), square -> context.pieceAt(square) == null);
    }

    HeroEffect geometryAvailable(HeroActionContext context, PieceState actor, PieceState target, Abilities ability,
                                 Predicate<Position> empty) {
        if (ability == ChronomlinAbility.EVOLVE)
            return HeroEffect.success(Map.of(actor.id(), actor.placed(actor.position(), nextType(actor.type()))))
                    .withCost(AbilityCost.ultimate(ultimateCaptures()));
        if (target == null || target.position() == null || target.id() == actor.id())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        int dr = target.position().getRow() - actor.position().getRow();
        int dc = target.position().getColumn() - actor.position().getColumn();
        if (dr != 0 && dc != 0 && Math.abs(dr) != Math.abs(dc))
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        int distance = Math.max(Math.abs(dr), Math.abs(dc));
        if (distance <= 1) return HeroEffect.rejected(AbilityRejection.NO_GRAPPLE_MOVEMENT);
        int rowStep = Integer.signum(dr), columnStep = Integer.signum(dc);
        for (int step = 1; step < distance; step++) {
            Position square = new Position(actor.position().getRow() + rowStep * step,
                    actor.position().getColumn() + columnStep * step);
            if (!empty.test(square))
                return HeroEffect.rejected(AbilityRejection.ABILITY_BLOCKED);
        }
        Position landing = new Position(target.position().getRow() - rowStep, target.position().getColumn() - columnStep);
        if (TerrainGeometry.blocksRay(actor.position(), landing,
                context.terrain().hostileSquares(context.pieces(), actor.color())))
            return HeroEffect.rejected(AbilityRejection.MUD_BLOCKED);
        return HeroEffect.success(Map.of(actor.id(), actor.placed(landing, actor.type())))
                .withCost(AbilityCost.cooldown(rules.grappleCooldown()));
    }

    static Class<? extends Figure> nextType(Class<? extends Figure> type) {
        if (type == Pawn.class) return Bishop.class;
        if (type == Bishop.class) return Knight.class;
        if (type == Knight.class) return Rook.class;
        return null;
    }

    @Override
    public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                  PieceState target, AbilityRequest request) {
        return request.ability() == ChronomlinAbility.GRAPPLE
                ? "Chronomlin: притяжение к " + cell(request.target()) : "Chronomlin: улучшение фигуры";
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
