package game.heroes.ares;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import game.terrain.TerrainState;
import model.*;
import model.figures.King;

import java.util.*;

/**
 * A direction is selected by a square on its ray. The spear always travels its full range.
 */
public final class AresAbilities {
    private AresAbilities() {
    }

    public static List<Abilities> abilities() {
        return AresAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, AresRules rules) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != HeroType.ARES_BISHOP || ability != AresAbility.SPEAR) return AbilityRejection.NO_ABILITY;
        return actor.cooldown() > 0 ? AbilityRejection.ABILITY_COOLDOWN : Success.SUCCESS;
    }

    public static List<Position> directions(Position from) {
        var result = new ArrayList<Position>();
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++)
                if (dr != 0 || dc != 0) {
                    Position end = from;
                    int range = dr == 0 || dc == 0 ? 3 : 2;
                    for (int n = 1; n <= range; n++) {
                        Position p = new Position(from.getRow() + dr * n, from.getColumn() + dc * n);
                        if (!inside(p)) break;
                        end = p;
                    }
                    if (!end.equals(from)) result.add(end);
                }
        return List.copyOf(result);
    }

    public static HeroEffect spear(Field field, Map<Long, PieceState> pieces, PieceState actor, Position direction, long turn, AresRules rules, TerrainState terrain, boolean borrowed) {
        if (direction == null) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        int dr = direction.getRow() - actor.position().getRow(), dc = direction.getColumn() - actor.position().getColumn();
        boolean straight = dr == 0 || dc == 0, diagonal = Math.abs(dr) == Math.abs(dc);
        int range = straight ? 3 : 2;
        if ((dr == 0 && dc == 0) || (!straight && !diagonal) || Math.max(Math.abs(dr), Math.abs(dc)) > range)
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        var updates = new HashMap<Long, PieceState>();
        updates.put(actor.id(), actor.withAbilityState(borrowed ? actor.cooldown() : rules.spearCooldown(), actor.ultimateCharge(), actor.storedPieceId()));
        PieceState carried = null;
        Position landing = null;
        int rowStep = Integer.signum(dr), colStep = Integer.signum(dc);
        for (int n = 1; n <= range; n++) {
            Position cell = new Position(actor.position().getRow() + rowStep * n, actor.position().getColumn() + colStep * n);
            if (!inside(cell)) break;
            PieceState hit = at(pieces, cell);
            if (carried == null) {
                if (hit != null && (hit.type() == King.class || hit.isBoxed())) break;
                if (hit != null) {
                    carried = hit;
                    landing = cell;
                }
            } else if (hit != null) {
                if (hit.type() != King.class && !hit.isBoxed() && !hit.isPotted()) {
                    long until = Math.addExact(turn, 1L);
                    updates.put(hit.id(), hit.withStun(Math.max(hit.stunnedThroughTurn(), until)));
                    if(!carried.isPotted())carried = carried.withStun(Math.max(carried.stunnedThroughTurn(), until));
                }
                break;
            } else {
                landing = cell;
                // Forced movement enters the first hostile trap instead of crossing it.
                if (terrain.hostileSquares(pieces, carried.color()).contains(cell)) break;
            }
        }
        if (carried != null) updates.put(carried.id(), carried.placed(landing, carried.type()));
        return HeroEffect.success(updates);
    }

    public static PieceState afterCapture(PieceState actor, AresRules rules) {
        return actor.withAbilityState(actor.cooldown(), Math.min(rules.revengeCaptures(), actor.ultimateCharge() + 1), null);
    }

    public static boolean armed(PieceState actor, AresRules rules) {
        return !actor.isPotted() && actor.hero() == HeroType.ARES_BISHOP && actor.ultimateCharge() >= rules.revengeCaptures();
    }

    private static PieceState at(Map<Long, PieceState> pieces, Position p) {
        return pieces.values().stream().filter(s -> p.equals(s.position())).findFirst().orElse(null);
    }

    private static boolean inside(Position p) {
        return p.getRow() >= 0 && p.getRow() < 8 && p.getColumn() >= 0 && p.getColumn() < 8;
    }
}
