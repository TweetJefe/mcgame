package game.heroes.bountycrow;

import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.figures.King;
import java.util.List;
import java.util.Map;

public final class BountyCrowAbilities {
    private BountyCrowAbilities() {}
    public static List<Ability> abilities() { return List.of(Ability.BOUNTY, Ability.STEAL); }
    public static MoveResult availability(PieceState actor, Ability ability, BountyCrowRules rules) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MoveResult.EMPTY_SOURCE;
        if (actor.hero() != HeroType.BOUNTY_CROW_PAWN || !abilities().contains(ability)) return MoveResult.NO_ABILITY;
        if (ability == Ability.BOUNTY && actor.cooldown() > 0) return MoveResult.ABILITY_COOLDOWN;
        if (ability == Ability.STEAL && actor.ultimateCharge() < rules.ultimateCaptures()) return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }
    public static HeroEffect preview(PieceState actor, PieceState target, Ability ability, BountyCrowRules rules) {
        MoveResult ready = availability(actor, ability, rules);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (ability == Ability.STEAL)
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(), actor.ultimateCharge() - rules.ultimateCaptures(), null)));
        if (target == null || target.status() != PieceState.Status.BOARD || target.color() == actor.color())
            return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(MoveResult.KING_IMMUNE);
        return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(rules.bountyCooldown(), actor.ultimateCharge(), null)));
    }
    public static PieceState afterCapture(PieceState actor, BountyCrowRules rules) {
        return rules.ultimateCaptures() == 0 ? actor : actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
