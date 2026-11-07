package game.heroes;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.MoveResult;
import game.state.PieceState;
import model.figures.King;
import rules.standard.MoveValidator;

import java.util.Set;
import java.util.Objects;

/** Target shape and exceptions, independent of any particular hero or ability identifier. */
public record HeroTargeting(Selection selection, Set<Permission> permissions) {
    public enum Selection { NONE, SQUARE, TARGET_AND_DESTINATION }

    /** DESTINATION is retained for callers of the former permission-based constructor. */
    public enum Permission { SELF, KING, INVISIBLE, POTTED, BOXED, ILLUSION, DESTINATION }

    public HeroTargeting {
        Objects.requireNonNull(selection, "selection");
        permissions = Set.copyOf(permissions);
    }

    public HeroTargeting(boolean requiresTarget, Set<Permission> permissions) {
        this(!requiresTarget ? Selection.NONE : permissions.contains(Permission.DESTINATION)
                ? Selection.TARGET_AND_DESTINATION : Selection.SQUARE, permissions);
    }

    public boolean requiresTarget() { return selection != Selection.NONE; }

    public boolean requiresDestination() { return selection == Selection.TARGET_AND_DESTINATION; }

    public static HeroTargeting square(Permission... permissions) {
        return new HeroTargeting(true, Set.of(permissions));
    }

    public static HeroTargeting untargeted() {
        return new HeroTargeting(Selection.NONE, Set.of(Permission.ILLUSION));
    }

    public static HeroTargeting targetAndDestination(Permission... permissions) {
        return new HeroTargeting(Selection.TARGET_AND_DESTINATION, Set.of(permissions));
    }

    /** Preserves the existing order of target rejection reasons. */
    public MoveResult validate(HeroActionContext context, PieceState actor, AbilityRequest request) {
        if (request.destination() != null && !requiresDestination())
            return AbilityRejection.INVALID_ABILITY_TARGET;
        if (!requiresTarget()) {
            if (request.target() != null) return AbilityRejection.INVALID_ABILITY_TARGET;
        } else {
            if (!MoveValidator.inside(context.field(), request.target())) return MovementRejection.OUT_OF_BOUNDS;
            if (actor.position().equals(request.target()) && !permissions.contains(Permission.SELF))
                return AbilityRejection.INVALID_ABILITY_TARGET;
        }
        PieceState target = context.pieceAt(request.target());
        if (!permissions.contains(Permission.INVISIBLE) && context.faun().hidden(target, actor.color())
                && !game.visibility.VisibilityRules.reveals(context, actor.color(), target.position(), game.visibility.Concealment.INVISIBILITY))
            return StatusRejection.TARGET_INVISIBLE;
        if (target == null) return Success.SUCCESS;
        if (target.type() == King.class && !permissions.contains(Permission.KING)) return StatusRejection.KING_IMMUNE;
        if (target.hero() == HeroType.ILLUSION && !permissions.contains(Permission.ILLUSION))
            return AbilityRejection.INVALID_ABILITY_TARGET;
        if (target.isPotted() && !permissions.contains(Permission.POTTED)) return StatusRejection.POTTED;
        if (target.isBoxed() && !permissions.contains(Permission.BOXED)) return StatusRejection.BOXED;
        return Success.SUCCESS;
    }
}
