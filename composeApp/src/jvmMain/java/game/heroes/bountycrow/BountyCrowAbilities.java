package game.heroes.bountycrow;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.figures.King;

import java.util.List;
import java.util.Map;

public final class BountyCrowAbilities {
    private BountyCrowAbilities() {
    }

    public static List<Abilities> abilities() {
        return BountyCrowAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, BountyCrowRules rules) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != HeroType.BOUNTY_CROW || !abilities().contains(ability)) return AbilityRejection.NO_ABILITY;
        if (ability == BountyCrowAbility.BOUNTY && actor.cooldown() > 0) return AbilityRejection.ABILITY_COOLDOWN;
        if (ability == BountyCrowAbility.STEAL && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, PieceState target, Abilities ability, BountyCrowRules rules) {
        MoveResult ready = availability(actor, ability, rules);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (ability == BountyCrowAbility.STEAL)
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(), actor.ultimateCharge() - rules.ultimateCaptures(), null)));
        if (target == null || target.status() != PieceState.Status.BOARD || target.color() == actor.color())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
        return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(rules.bountyCooldown(), actor.ultimateCharge(), null)));
    }

    public static PieceState afterCapture(PieceState actor, BountyCrowRules rules) {
        return rules.ultimateCaptures() == 0 ? actor : actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
