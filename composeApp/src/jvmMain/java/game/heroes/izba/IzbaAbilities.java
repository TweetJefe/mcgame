package game.heroes.izba;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.MoveResult;
import game.state.PieceState;

import java.util.List;

/** Совместимый фасад над стратегией призыва. */
public final class IzbaAbilities {
    private IzbaAbilities() {
    }

    public static List<Abilities> abilities() {
        return IzbaAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, IzbaRules rules) {
        return availability(actor, ability, rules, false);
    }

    public static MoveResult availability(PieceState actor, Abilities ability, IzbaRules rules, boolean borrowed) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.IZBA) || ability != IzbaAbility.SUMMON_YAGA)
            return AbilityRejection.NO_ABILITY;
        return !borrowed && actor.ultimateCharge() < rules.ultimateCaptures()
                ? AbilityRejection.ULTIMATE_NOT_READY : Success.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, Abilities ability, IzbaRules rules) {
        return preview(actor, ability, rules, false);
    }

    public static HeroEffect preview(PieceState actor, Abilities ability, IzbaRules rules, boolean borrowed) {
        return new IzbaHero(rules).detachedPreview(actor, ability, borrowed);
    }

    public static PieceState afterCapture(PieceState actor, IzbaRules rules) {
        return actor.withAbilityState(0, Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
