package game.heroes.supernova;

import game.mechanics.PieceRestriction;

import game.heroes.AbstractHero;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import model.Position;
import model.figures.King;

/** Геометрия общей области не зависит от стороны или личности затронутых фигур. */
public final class SupernovaHero extends AbstractHero {
    private final SupernovaRules rules;
    public SupernovaHero(SupernovaRules rules) {
        super(SupernovaDefinition.INSTANCE, SupernovaAbility.getAbilities(), SupernovaAbility.SHUFFLE,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }
    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                    PieceState target, AbilityRequest request) {
        if (distance(actor.position(), request.target()) > rules.castRadius())
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        boolean shuffle = request.ability() == SupernovaAbility.SHUFFLE;
        var participants = participants(context, request.target(), rules.effectRadius(), shuffle);
        var placements = shuffle ? List.<SupernovaMechanic.Placement>of()
                : pull(context, request.target(), participants);
        if (shuffle ? participants.size() < 2 : placements.isEmpty())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        return HeroEffect.empty().withOperations(new SupernovaMechanic.Cast(actor.id(), request.target(), shuffle,
                participants.stream().map(PieceState::id).toList(), placements))
                .withCost(shuffle ? AbilityCost.ultimate(ultimateCaptures()) : AbilityCost.cooldown(rules.pullCooldown()));
    }
    static List<PieceState> participants(HeroActionContext context, Position center, int radius, boolean kings) {
        return context.pieces().values().stream().filter(p -> p.status() == PieceState.Status.BOARD
                && !p.isBoxed() && !context.blocks(p, PieceRestriction.DISPLACEMENT) && (kings || p.type() != King.class) && distance(p.position(), center) <= radius
                && context.field().createFigure(p.type(), p.position(), p.color()).canBeDisplaced())
                .sorted(Comparator.comparingLong(PieceState::id)).toList();
    }
    static List<SupernovaMechanic.Placement> pull(HeroActionContext context, Position center, List<PieceState> participants) {
        var slots = new ArrayList<Position>();
        for (int row = Math.max(0, center.getRow() - 1); row <= Math.min(7, center.getRow() + 1); row++)
            for (int column = Math.max(0, center.getColumn() - 1); column <= Math.min(7, center.getColumn() + 1); column++) {
                var square = new Position(row, column);
                if (!square.equals(center) && context.pieceAt(square) == null) slots.add(square);
            }
        var result = new ArrayList<SupernovaMechanic.Placement>();
        // Уже стоящие вокруг центра занимают свои ближайшие места и не вытесняются.
        for (var piece : participants.stream().sorted(Comparator
                .comparingInt((PieceState p) -> distance(p.position(), center)).thenComparingLong(PieceState::id)).toList()) {
            if (distance(piece.position(), center) == 1 || slots.isEmpty()) continue;
            var to = slots.stream().min(Comparator.comparingInt((Position p) -> distance(piece.position(), p))
                    .thenComparingInt(Position::getColumn).thenComparingInt(Position::getRow)).orElseThrow();
            slots.remove(to);
            result.add(new SupernovaMechanic.Placement(piece.id(), piece.position(), to));
        }
        return List.copyOf(result);
    }
    public static int distance(Position a, Position b) {
        return Math.max(Math.abs(a.getRow() - b.getRow()), Math.abs(a.getColumn() - b.getColumn()));
    }
    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        return request.ability() == SupernovaAbility.PULL ? "Supernova: стягивание фигур" : "Supernova: перемешивание фигур";
    }
}
