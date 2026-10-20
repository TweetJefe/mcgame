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

/** Target shape and exceptions, independent of any particular hero or ability identifier. */
public record HeroTargeting(boolean requiresTarget, Set<Permission> permissions) {
    public enum Permission { SELF, KING, INVISIBLE, POTTED, BOXED, ILLUSION, DESTINATION }

    public HeroTargeting {
        permissions = Set.copyOf(permissions);
    }

    public static HeroTargeting square(Permission... permissions) {
        return new HeroTargeting(true, Set.of(permissions));
    }

    public static HeroTargeting untargeted() {
        return new HeroTargeting(false, Set.of(Permission.ILLUSION));
    }

    /** Preserves the existing order of target rejection reasons. */
    public MoveResult validate(HeroActionContext context, PieceState actor, AbilityRequest request) {
        if (request.destination() != null && !permissions.contains(Permission.DESTINATION))
            return AbilityRejection.INVALID_ABILITY_TARGET;
        if (!requiresTarget) {
            if (request.target() != null) return AbilityRejection.INVALID_ABILITY_TARGET;
        } else {
            if (!MoveValidator.inside(context.field(), request.target())) return MovementRejection.OUT_OF_BOUNDS;
            if (actor.position().equals(request.target()) && !permissions.contains(Permission.SELF))
                return AbilityRejection.INVALID_ABILITY_TARGET;
        }
        PieceState target = context.pieceAt(request.target());
        if (!permissions.contains(Permission.INVISIBLE) && context.faun().hidden(target, actor.color()))
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
