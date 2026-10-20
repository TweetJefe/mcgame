package game.heroes;

import game.moves.MoveResult;
import game.state.PieceState;
import model.Figure;
import model.Position;

import java.util.List;

/**
 * Reusable hero behavior. Instances hold only immutable rules, never per-piece game state.
 * Implementations propose changes without modifying the context or applying them to a game.
 */
public interface Hero {
    HeroType type();

    List<Ability> abilities();

    /** Eligible initial chess bodies; promotions and copied spells retain their own state rules. */
    default boolean supports(Class<? extends Figure> figureType) {
        return type().supports(figureType);
    }

    /** Declarative targeting shared by execution and target discovery. */
    default HeroTargeting targeting(Ability ability) {
        return HeroTargeting.square(HeroTargeting.Permission.ILLUSION);
    }

    /** Candidate requests; the engine validates each preview and the remaining turn. */
    default List<AbilityRequest> requests(HeroActionContext context, PieceState actor,
                                         Ability ability, boolean borrowed) {
        if (!targeting(ability).requiresTarget())
            return List.of(new AbilityRequest(actor.position(), ability, null, null, borrowed));
        return context.field().getGameField().keySet().stream()
                .map(square -> new AbilityRequest(actor.position(), ability, square, null, borrowed)).toList();
    }

    /** Checks the actor's own ability requirements, without validating a target or turn budgets. */
    MoveResult availability(PieceState actor, Ability ability);

    /**
     * Checks availability and proposes the effect. Target and square may be null for abilities
     * that do not need them; the implementation validates the targeting it requires.
     */
    HeroEffect preview(HeroActionContext context, PieceState actor, PieceState target,
                       Position square, Ability ability);

    /** Returns the actor's state after one capture, including any earned ultimate charge. */
    PieceState afterCapture(PieceState actor);

    /** Human-readable history text after a successful action; must not change either snapshot. */
    default String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                   PieceState target, AbilityRequest request) {
        return type().getName() + ": " + request.ability().getDisplayName();
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
