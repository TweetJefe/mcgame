package game.heroes.izba;

import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import java.util.List;
import java.util.Map;

public final class IzbaAbilities {
    private IzbaAbilities() {}
    public static List<Ability> abilities() { return List.of(Ability.SUMMON_YAGA); }
    public static MoveResult availability(PieceState actor, Ability ability, IzbaRules rules) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MoveResult.EMPTY_SOURCE;
        if (actor.hero() != HeroType.IZBA_ROOK || ability != Ability.SUMMON_YAGA) return MoveResult.NO_ABILITY;
        return actor.ultimateCharge() < rules.ultimateCaptures() ? MoveResult.ULTIMATE_NOT_READY : MoveResult.SUCCESS;
    }
    public static HeroEffect preview(PieceState actor, Ability ability, IzbaRules rules) {
        MoveResult result = availability(actor, ability, rules);
        return result.isSuccess() ? HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(0,
                actor.ultimateCharge()-rules.ultimateCaptures(), null))) : HeroEffect.rejected(result);
    }
    public static PieceState afterCapture(PieceState actor, IzbaRules rules) {
        return actor.withAbilityState(0, Math.min(rules.ultimateCaptures(),actor.ultimateCharge()+1), null);
    }
}
