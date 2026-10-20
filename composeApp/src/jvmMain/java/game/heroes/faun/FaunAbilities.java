package game.heroes.faun;

import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.Position;

import java.util.*;

public final class FaunAbilities {
    private FaunAbilities() {
    }

    public static List<Ability> abilities() {
        return List.of(Ability.CLOUD, Ability.INVISIBILITY);
    }

    public static MoveResult availability(PieceState actor, Ability ability, FaunRules rules) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MoveResult.EMPTY_SOURCE;
        if (actor.hero() != HeroType.FAUN_KNIGHT || !abilities().contains(ability)) return MoveResult.NO_ABILITY;
        if (ability == Ability.CLOUD && actor.cooldown() > 0) return MoveResult.ABILITY_COOLDOWN;
        if (ability == Ability.INVISIBILITY && actor.ultimateCharge() < rules.ultimateCaptures())
            return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, Position center, Ability ability, FaunRules rules) {
        var ready = availability(actor, ability, rules);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (ability == Ability.CLOUD) {
            if (center == null || Math.max(Math.abs(center.getRow() - actor.position().getRow()), Math.abs(center.getColumn() - actor.position().getColumn())) > 4)
                return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(rules.cloudCooldown(), actor.ultimateCharge(), null)));
        }
        return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(), actor.ultimateCharge() - rules.ultimateCaptures(), null)));
    }

    public static PieceState afterCapture(PieceState actor, FaunRules rules) {
        return actor.withAbilityState(actor.cooldown(), Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
