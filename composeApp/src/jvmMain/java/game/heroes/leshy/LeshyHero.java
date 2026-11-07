package game.heroes.leshy;

import game.heroes.AbstractHero;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.mechanics.PieceRestriction;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import model.FigureColor;
import model.Position;
import model.figures.King;

/** Направление задаётся стороной настоящего исполнителя, даже при копировании. */
public final class LeshyHero extends AbstractHero {
    private final LeshyRules rules;
    public LeshyHero(LeshyRules rules) {
        super(LeshyDefinition.INSTANCE, LeshyAbility.getAbilities(), LeshyAbility.FOREST,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }
    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                    PieceState target, AbilityRequest request) {
        if (request.ability() == LeshyAbility.VINE) {
            var cells = vineCells(actor, rules.vineLength());
            if (cells.isEmpty()) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            if (cells.stream().map(context::pieceAt).filter(Objects::nonNull).anyMatch(p -> p.type() == King.class))
                return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
            return HeroEffect.empty().withOperations(new LeshyMechanic.Plant(actor.id(), cells))
                    .withCost(AbilityCost.cooldown(rules.vineCooldown()));
        }
        var targets = forestTargets(context, actor);
        if (targets.isEmpty()) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        return HeroEffect.empty().withOperations(new LeshyMechanic.Forest(actor.id(), targets))
                .withCost(AbilityCost.ultimate(ultimateCaptures()));
    }
    static List<Position> vineCells(PieceState actor, int length) {
        return vineCells(actor.position(), actor.color(), length);
    }
    static List<Position> vineCells(Position from, FigureColor color, int length) {
        var result = new ArrayList<Position>();
        int direction = color == FigureColor.WHITE ? 1 : -1;
        for (int i = 1; i <= length; i++) {
            int row = from.getRow() + i * direction;
            if (row < 0 || row > 7) break;
            result.add(new Position(row, from.getColumn()));
        }
        return List.copyOf(result);
    }
    static List<Long> forestTargets(HeroActionContext context, PieceState actor) {
        var result = new ArrayList<Long>();
        for (int step : new int[]{-1, 1}) {
            int column = actor.position().getColumn() + step;
            if (column < 0 || column > 7) continue;
            var piece = context.pieceAt(new Position(actor.position().getRow(), column));
            if (piece != null && piece.type() != King.class && !piece.isBoxed() && !context.blocks(piece, PieceRestriction.CAPTURE)) result.add(piece.id());
        }
        return List.copyOf(result);
    }
    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        return request.ability() == LeshyAbility.VINE ? "Leshy: лоза перед собой" : "Leshy: соседние фигуры стали деревьями";
    }
}
