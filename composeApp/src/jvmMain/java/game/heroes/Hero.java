package game.heroes;

import game.history.AbilityEventTarget;
import game.moves.MoveResult;
import game.state.PieceState;
import java.util.List;
import java.util.ArrayList;
import model.Figure;
import model.Position;

/**
 * Reusable hero behavior. Instances hold only immutable rules, never per-piece game state.
 * Implementations propose changes without modifying the context or applying them to a game.
 */
public interface Hero {
    HeroType type();

    default HeroDefinition<?> definition() {
        return HeroDefinitions.get(type());
    }

    List<Abilities> abilities();

    /** Eligible initial chess bodies; promotions and copied spells retain their own state rules. */
    default boolean supports(Class<? extends Figure> figureType) {
        return definition().supports(figureType);
    }

    /** Declarative targeting shared by execution and target discovery. */
    default HeroTargeting targeting(Abilities ability) {
        return ability.targeting();
    }

    /** Candidate requests; the engine validates each preview and the remaining turn. */
    default List<AbilityRequest> requests(HeroActionContext context, PieceState actor,
                                         Abilities ability, boolean borrowed) {
        if (!targeting(ability).requiresTarget())
            return List.of(new AbilityRequest(actor.position(), ability, null, null, borrowed));
        if (targeting(ability).requiresDestination()) {
            var requests = new ArrayList<AbilityRequest>();
            for (Position target : context.field().getGameField().keySet())
                for (Position destination : context.field().getGameField().keySet())
                    requests.add(new AbilityRequest(actor.position(), ability, target, destination, borrowed));
            return List.copyOf(requests);
        }
        return context.field().getGameField().keySet().stream()
                .map(square -> new AbilityRequest(actor.position(), ability, square, null, borrowed)).toList();
    }

    /** Checks the actor's own ability requirements, without validating a target or turn budgets. */
    MoveResult availability(PieceState actor, Abilities ability);

    /** Общие условия и специфические ограничения для выбранного канала применения. */
    default MoveResult availability(PieceState actor, Abilities ability, AbilityUse use) {
        return availability(actor, ability);
    }

    /** Resolves the actor and target from one snapshot and proposes an effect for the full request. */
    HeroEffect preview(HeroActionContext context, AbilityRequest request);

    /** Compatibility entry for callers supplying detached piece states. */
    default HeroEffect preview(HeroActionContext context, PieceState actor, PieceState target,
                               Position square, Abilities ability) {
        return preview(context, new AbilityRequest(actor == null ? null : actor.position(), ability, square));
    }

    /** Compatibility facade for callers supplying detached piece states and a full request. */
    default HeroEffect previewRequest(HeroActionContext context, PieceState actor, PieceState target,
                                      AbilityRequest request) {
        return preview(context, request);
    }

    default AbilityEventDetails abilityEventDetails(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        return new AbilityEventDetails(target == null || request.ability().eventTarget() == AbilityEventTarget.NONE
                ? null : target.id(), request.destination() != null ? request.destination()
                : request.target() == null ? actor.position() : request.target(), null);
    }

    /** Returns the actor's state after one capture, including any earned ultimate charge. */
    PieceState afterCapture(PieceState actor);

    /** Human-readable history text after a successful action; must not change either snapshot. */
    default String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                   PieceState target, AbilityRequest request) {
        return definition().displayName() + ": " + request.ability().getDisplayName();
    }

    /**
     * Proposes one death reaction. Victim is its state immediately before removal; context
     * contains the prepared states after removal. Further deaths are processed by the resolver.
     */
    default HeroEffect onCaptured(HeroActionContext context, PieceState victim,
                                   long captorId, Position square) {
        return HeroEffect.empty();
    }
}
