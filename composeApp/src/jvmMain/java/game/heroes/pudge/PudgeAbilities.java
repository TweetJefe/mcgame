package game.heroes.pudge;

import game.heroes.Ability;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.Capture;
import game.moves.MoveResult;
import game.state.PieceState;

import model.Field;
import model.Figure;
import model.Position;
import model.figures.Empty;
import model.figures.King;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Pudge's geometry, costs and passive reaction. Only immutable proposed changes leave this class. */
public final class PudgeAbilities {
    private PudgeAbilities() {}
    public static List<Ability> abilities() { return List.of(Ability.HOOK, Ability.SWALLOW); }
    public static MoveResult availability(PieceState actor, Ability ability, PudgeRules rules) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MoveResult.EMPTY_SOURCE;
        if (actor.hero() != HeroType.PUDGE_ROOK || !abilities().contains(ability)) return MoveResult.NO_ABILITY;
        if (ability == Ability.HOOK && actor.cooldown() > 0) return MoveResult.ABILITY_COOLDOWN;
        if (ability == Ability.SWALLOW && actor.storedPieceId() != null) return MoveResult.ALREADY_SWALLOWED;
        if (ability == Ability.SWALLOW && actor.ultimateCharge() < rules.ultimateCaptures()) return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }
    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Ability ability, PudgeRules rules) {
        MoveResult ready = availability(actor, ability, rules);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        if (target == null || target.status() != PieceState.Status.BOARD || target.id() == actor.id())
            return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(MoveResult.KING_IMMUNE);
        int dr = target.position().getRow() - actor.position().getRow(), dc = target.position().getColumn() - actor.position().getColumn();
        int distance = Math.max(Math.abs(dr), Math.abs(dc));
        if (ability == Ability.HOOK) {
            boolean straight = dr == 0 || dc == 0, diagonal = Math.abs(dr) == Math.abs(dc);
            if (distance > 3 || (!straight && !(rules.hookDiagonals() && diagonal)))
                return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
            int rowStep = Integer.signum(dr), columnStep = Integer.signum(dc);
            for (int step = 1; step < distance; step++) {
                Position square = new Position(actor.position().getRow() + rowStep * step, actor.position().getColumn() + columnStep * step);
                if (!(field.getFigureByPosition(square) instanceof Empty)) return HeroEffect.rejected(MoveResult.ABILITY_BLOCKED);
            }
            Position landing = new Position(actor.position().getRow() + rowStep, actor.position().getColumn() + columnStep);
            return HeroEffect.success(Map.of(target.id(), target.placed(landing, target.type()), actor.id(),
                    actor.withAbilityState(rules.hookCooldown(), actor.ultimateCharge(), actor.storedPieceId())));
        }
        if (distance != 1 || (!rules.swallowDiagonals() && Math.abs(dr) + Math.abs(dc) != 1))
            return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
        if (target.color() != actor.color()) return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        return HeroEffect.success(Map.of(target.id(), target.hidden(PieceState.Status.SWALLOWED), actor.id(),
                actor.withAbilityState(actor.cooldown(), actor.ultimateCharge() - rules.ultimateCaptures(), target.id())));
    }
    public static PieceState afterCapture(PieceState actor, PudgeRules rules) {
        return actor.hero() != HeroType.PUDGE_ROOK || rules.ultimateCaptures() == 0 ? actor
                : actor.withAbilityState(actor.cooldown(), Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), actor.storedPieceId());
    }
    public static HeroEffect captureReaction(Field field, Map<Long, PieceState> pieces, PieceState victim,
                                            long captorId, Position square, UnaryOperator<PieceState> onCapture) {
        var working = new HashMap<>(pieces); var updates = new HashMap<Long, PieceState>(); var captures = new ArrayList<Capture>();
        Long passenger = victim.storedPieceId(); long occupantId = captorId;
        // A loaded attacking Pudge releases its own ally after being counter-captured.
        while (passenger != null) {
            PieceState occupant = working.get(occupantId);
            if (occupant.type() == King.class) return HeroEffect.rejected(MoveResult.KING_IN_CHECK);
            PieceState released = working.get(passenger);
            if (released == null || released.status() != PieceState.Status.SWALLOWED) return HeroEffect.rejected(MoveResult.INVALID_BOARD);
            Long nextPassenger = occupant.storedPieceId();
            Figure eaten = field.createFigure(occupant.type(), square, occupant.color());
            captures.add(new Capture(released.id(), occupant.id(), released.color(), eaten, square, true));
            PieceState dead = occupant.hidden(PieceState.Status.CAPTURED);
            PieceState returned = onCapture.apply(released.placed(square, released.type()));
            working.put(dead.id(), dead); updates.put(dead.id(), dead);
            working.put(returned.id(), returned); updates.put(returned.id(), returned);
            passenger = nextPassenger; occupantId = returned.id();
        }
        return new HeroEffect(MoveResult.SUCCESS, updates, captures);
    }
}
