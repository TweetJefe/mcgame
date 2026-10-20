package game.heroes.ginseng;

import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.*;
import model.figures.*;

import java.util.*;

public final class GinsengAbilities {
    private GinsengAbilities() {
    }

    public static List<Ability> abilities() {
        return List.of(Ability.DISPLACE, Ability.COPY_SPELL);
    }

    public static MoveResult availability(PieceState actor, Ability ability, GinsengRules rules, boolean borrowed) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MoveResult.EMPTY_SOURCE;
        if (actor.hero() != HeroType.GINSENG_BISHOP || !abilities().contains(ability)) return MoveResult.NO_ABILITY;
        if (!borrowed && ability == Ability.DISPLACE && actor.cooldown() > 0) return MoveResult.ABILITY_COOLDOWN;
        if (!borrowed && ability == Ability.COPY_SPELL && actor.ultimateCharge() < rules.ultimateCaptures())
            return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }

    public static HeroEffect displace(Field field, PieceState actor, PieceState target, Position destination, GinsengRules rules, boolean borrowed) {
        MoveResult ready = availability(actor, Ability.DISPLACE, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (target == null) return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(MoveResult.KING_IMMUNE);
        if (distance(actor.position(), target.position()) > 3)
            return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
        if (destination == null || destination.getRow() < 0 || destination.getRow() > 7 || destination.getColumn() < 0 || destination.getColumn() > 7)
            return HeroEffect.rejected(MoveResult.OUT_OF_BOUNDS);
        if (distance(target.position(), destination) != 1 || !(field.getFigureByPosition(destination) instanceof Empty))
            return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        var updates = new HashMap<Long, PieceState>();
        PieceState next = actor.withAbilityState(borrowed ? actor.cooldown() : rules.displacementCooldown(), actor.ultimateCharge(), actor.storedPieceId());
        updates.put(actor.id(), next);
        updates.put(target.id(), (target.id() == actor.id() ? next : target).placed(destination, target.type()));
        return HeroEffect.success(updates);
    }

    public static HeroEffect copy(PieceState actor, PieceState source, Ability last, GinsengRules rules, boolean borrowed) {
        MoveResult ready = availability(actor, Ability.COPY_SPELL, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (source == null || source.hero() == HeroType.NONE || source.type() == King.class)
            return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if (last == null) return HeroEffect.rejected(MoveResult.NO_USED_ABILITY);
        return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(),
                borrowed ? actor.ultimateCharge() : actor.ultimateCharge() - rules.ultimateCaptures(), actor.storedPieceId())));
    }

    public static PieceState afterCapture(PieceState actor, GinsengRules rules) {
        return actor.withAbilityState(actor.cooldown(), Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), actor.storedPieceId());
    }

    private static int distance(Position a, Position b) {
        return Math.max(Math.abs(a.getRow() - b.getRow()), Math.abs(a.getColumn() - b.getColumn()));
    }
}
