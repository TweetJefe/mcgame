package game.heroes.lumeshard;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.MoveResult;
import game.state.PieceState;
import game.state.SquareColor;
import model.Field;
import model.Position;
import model.figures.King;

import java.util.List;
import java.util.Map;

public final class LumeshardAbilities {
    private LumeshardAbilities() {
    }

    public static List<Abilities> abilities() {
        return LumeshardAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, LumeshardRules rules) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != HeroType.LUMESHARD || !abilities().contains(ability)) return AbilityRejection.NO_ABILITY;
        if (ability == LumeshardAbility.LASER && actor.cooldown() > 0) return AbilityRejection.ABILITY_COOLDOWN;
        if (ability == LumeshardAbility.TELEPORT && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square, Abilities ability, LumeshardRules rules) {
        MoveResult ready = availability(actor, ability, rules);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (ability == LumeshardAbility.TELEPORT) {
            if (target != null) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            if (SquareColor.at(square) != actor.startingColor())
                return HeroEffect.rejected(AbilityRejection.WRONG_SQUARE_COLOR);
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(),
                    actor.ultimateCharge() - rules.ultimateCaptures(), null).placed(square, actor.type())));
        }
        if (target == null || target.status() != PieceState.Status.BOARD || target.color() == actor.color())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
        int distance = Math.max(Math.abs(square.getRow() - actor.position().getRow()), Math.abs(square.getColumn() - actor.position().getColumn()));
        if (distance > 3) return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(rules.laserCooldown(), actor.ultimateCharge(), null),
                target.id(), target.withBlindness(Math.max(target.blindTurns(), rules.blindnessTurns()))));
    }

    public static PieceState afterCapture(PieceState actor, LumeshardRules rules) {
        return rules.ultimateCaptures() == 0 ? actor : actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
