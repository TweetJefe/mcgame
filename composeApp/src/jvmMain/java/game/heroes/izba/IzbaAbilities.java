package game.heroes.izba;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;

import java.util.List;
import java.util.Map;

public final class IzbaAbilities {
    private IzbaAbilities() {
    }

    public static List<Abilities> abilities() {
        return IzbaAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, IzbaRules rules) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != HeroType.IZBA_ROOK || ability != IzbaAbility.SUMMON_YAGA) return AbilityRejection.NO_ABILITY;
        return actor.ultimateCharge() < rules.ultimateCaptures() ? AbilityRejection.ULTIMATE_NOT_READY : Success.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, Abilities ability, IzbaRules rules) {
        MoveResult result = availability(actor, ability, rules);
        return result.isSuccess() ? HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(0,
                actor.ultimateCharge() - rules.ultimateCaptures(), null))) : HeroEffect.rejected(result);
    }

    public static PieceState afterCapture(PieceState actor, IzbaRules rules) {
        return actor.withAbilityState(0, Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
