package game.heroes.lumeshard;

import game.heroes.Ability;
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
    private LumeshardAbilities() {}
    public static List<Ability> abilities() { return List.of(Ability.LASER, Ability.TELEPORT); }
    public static MoveResult availability(PieceState actor, Ability ability, LumeshardRules rules) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MoveResult.EMPTY_SOURCE;
        if (actor.hero() != HeroType.LUMESHARD_BISHOP || !abilities().contains(ability)) return MoveResult.NO_ABILITY;
        if (ability == Ability.LASER && actor.cooldown() > 0) return MoveResult.ABILITY_COOLDOWN;
        if (ability == Ability.TELEPORT && actor.ultimateCharge() < rules.ultimateCaptures()) return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }
    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square, Ability ability, LumeshardRules rules) {
        MoveResult ready = availability(actor, ability, rules);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (ability == Ability.TELEPORT) {
            if (target != null) return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
            if (SquareColor.at(square) != actor.startingColor()) return HeroEffect.rejected(MoveResult.WRONG_SQUARE_COLOR);
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(),
                    actor.ultimateCharge() - rules.ultimateCaptures(), null).placed(square, actor.type())));
        }
        if (target == null || target.status() != PieceState.Status.BOARD || target.color() == actor.color())
            return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(MoveResult.KING_IMMUNE);
        int distance = Math.max(Math.abs(square.getRow() - actor.position().getRow()), Math.abs(square.getColumn() - actor.position().getColumn()));
        if (distance > 3) return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
        return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(rules.laserCooldown(), actor.ultimateCharge(), null),
                target.id(), target.withBlindness(Math.max(target.blindTurns(), rules.blindnessTurns()))));
    }
    public static PieceState afterCapture(PieceState actor, LumeshardRules rules) {
        return rules.ultimateCaptures() == 0 ? actor : actor.withAbilityState(actor.cooldown(),
                Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), null);
    }
}
