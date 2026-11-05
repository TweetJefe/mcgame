package game.heroes.bountycrow;

import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.List;

/** Совместимый публичный фасад; расчёт способностей принадлежит BountyCrowHero. */
public final class BountyCrowAbilities {
    private BountyCrowAbilities() {
    }

    public static List<Abilities> abilities() {
        return BountyCrowAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, BountyCrowRules rules) {
        return availability(actor, ability, rules, false);
    }

    public static MoveResult availability(PieceState actor, Abilities ability, BountyCrowRules rules, boolean borrowed) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.BOUNTY_CROW) || !abilities().contains(ability)) return AbilityRejection.NO_ABILITY;
        if (!borrowed && ability == BountyCrowAbility.BOUNTY && actor.cooldown() > 0) return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == BountyCrowAbility.STEAL && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, PieceState target, Abilities ability, BountyCrowRules rules) {
        return preview(actor, target, ability, rules, false);
    }

    public static HeroEffect preview(PieceState actor, PieceState target, Abilities ability, BountyCrowRules rules, boolean borrowed) {
        MoveResult ready = availability(actor, ability, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return new BountyCrowHero(rules).legacyPreview(actor, target, ability, borrowed);
    }

    public static PieceState afterCapture(PieceState actor, BountyCrowRules rules) {
        return new BountyCrowHero(rules).legacyAfterCapture(actor);
    }
}
