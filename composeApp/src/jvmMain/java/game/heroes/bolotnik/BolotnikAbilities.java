package game.heroes.bolotnik;

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

    public static List<Ability> abilities() {
        return List.of(Ability.MUD, Ability.AMBUSH);
    }

    public static MoveResult availability(PieceState actor, Ability ability, BolotnikRules rules) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MoveResult.EMPTY_SOURCE;
        if (actor.hero() != HeroType.BOLOTNIK_KNIGHT || !abilities().contains(ability)) return MoveResult.NO_ABILITY;
        if (ability == Ability.MUD && actor.cooldown() > 0) return MoveResult.ABILITY_COOLDOWN;
        if (ability == Ability.AMBUSH && actor.ultimateCharge() < rules.ultimateCaptures())
            return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, PieceState target, Position square, Ability ability, BolotnikRules rules) {
        MoveResult ready = availability(actor, ability, rules);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (ability == Ability.AMBUSH)
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(), actor.ultimateCharge() - rules.ultimateCaptures(), null)
                    .hidden(PieceState.Status.HIDDEN_BOG)));
        if (target != null && target.type() == King.class) return HeroEffect.rejected(MoveResult.KING_IMMUNE);
        if (target != null && target.color() != actor.color())
            return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        int distance = Math.max(Math.abs(square.getRow() - actor.position().getRow()), Math.abs(square.getColumn() - actor.position().getColumn()));
        if (distance > 4) return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
        return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(rules.mudCooldown(), actor.ultimateCharge(), null)));
    }

    public static PieceState afterCapture(PieceState actor, BolotnikRules rules) {
        return rules.ultimateCaptures() == 0 ? actor : actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
