package game.heroes.bolotnik;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.Position;
import model.figures.King;

import java.util.List;
import java.util.Map;

/**
 * Hero costs and targeting. Terrain placement and reactions are resolved atomically by the terrain layer.
 */
public final class BolotnikAbilities {
    private BolotnikAbilities() {
    }

    public static List<Abilities> abilities() {
        return BolotnikAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, BolotnikRules rules) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != HeroType.BOLOTNIK || !abilities().contains(ability)) return AbilityRejection.NO_ABILITY;
        if (ability == BolotnikAbility.MUD && actor.cooldown() > 0) return AbilityRejection.ABILITY_COOLDOWN;
        if (ability == BolotnikAbility.AMBUSH && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, PieceState target, Position square, Abilities ability, BolotnikRules rules) {
        MoveResult ready = availability(actor, ability, rules);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (ability == BolotnikAbility.AMBUSH)
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(), actor.ultimateCharge() - rules.ultimateCaptures(), null)
                    .hidden(PieceState.Status.HIDDEN_BOG)));
        if (target != null && target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
        if (target != null && target.color() != actor.color())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        int distance = Math.max(Math.abs(square.getRow() - actor.position().getRow()), Math.abs(square.getColumn() - actor.position().getColumn()));
        if (distance > 4) return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(rules.mudCooldown(), actor.ultimateCharge(), null)));
    }

    public static PieceState afterCapture(PieceState actor, BolotnikRules rules) {
        return rules.ultimateCaptures() == 0 ? actor : actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
