package game.heroes.ares;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.moves.Capture;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import game.terrain.TerrainState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.Position;
import model.figures.King;

/** Направление копья задаётся клеткой луча; полёт всегда использует полную дальность. */
public final class AresHero extends AbstractHero implements LegacyPreview {
    private final AresRules rules;

    public AresHero(AresRules rules) {
        super(AresDefinition.INSTANCE, AresAbility.getAbilities(), null,
                Objects.requireNonNull(rules, "rules").revengeCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
    }

    @Override
    protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                           AbilityRequest request) {
        return spearAvailable(context.pieces(), actor, request.target(), context.currentTurn(), context.terrain());
    }

    HeroEffect spearAvailable(Map<Long, PieceState> pieces, PieceState actor, Position direction,
                              long turn, TerrainState terrain) {
        if (direction == null) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        int dr = direction.getRow() - actor.position().getRow();
        int dc = direction.getColumn() - actor.position().getColumn();
        boolean straight = dr == 0 || dc == 0, diagonal = Math.abs(dr) == Math.abs(dc);
        int range = straight ? 3 : 2;
        if ((dr == 0 && dc == 0) || (!straight && !diagonal) || Math.max(Math.abs(dr), Math.abs(dc)) > range)
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        var updates = new HashMap<Long, PieceState>();
        updates.put(actor.id(), actor);
        PieceState carried = null;
        Position landing = null;
        int rowStep = Integer.signum(dr), columnStep = Integer.signum(dc);
        for (int n = 1; n <= range; n++) {
            Position cell = new Position(actor.position().getRow() + rowStep * n,
                    actor.position().getColumn() + columnStep * n);
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
                    if (!carried.isPotted()) carried = carried.withStun(Math.max(carried.stunnedThroughTurn(), until));
                }
                break;
            } else {
                landing = cell;
                // Принудительное движение входит в первую вражескую ловушку, не перескакивая её.
                if (terrain.hostileSquares(pieces, carried.color()).contains(cell)) break;
            }
        }
        if (carried != null) updates.put(carried.id(), carried.placed(landing, carried.type()));
        return HeroEffect.success(updates).withCost(AbilityCost.cooldown(rules.spearCooldown()));
    }

    static List<Position> directions(Position from) {
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

    @Override
    public List<AbilityRequest> requests(HeroActionContext context, PieceState actor, Abilities ability, boolean borrowed) {
        if (ability != AresAbility.SPEAR) return super.requests(context, actor, ability, borrowed);
        return directions(actor.position()).stream()
                .map(target -> new AbilityRequest(actor.position(), ability, target, null, borrowed)).toList();
    }

    static boolean armed(PieceState actor, AresRules rules) {
        return !actor.isPotted() && actor.hero() == HeroType.ARES && actor.ultimateCharge() >= rules.revengeCaptures();
    }

    @Override
    public HeroEffect onCaptured(HeroActionContext context, PieceState victim, long captorId, Position square) {
        if (!armed(victim, rules)) return HeroEffect.empty();
        PieceState spent = context.pieceById(victim.id()).withAbilityState(victim.cooldown(), 0, null);
        PieceState killer = context.pieceById(captorId);
        if (killer == null || killer.isRemoved() || killer.isPotted())
            return HeroEffect.success(Map.of(spent.id(), spent));
        if (killer.type() == King.class) return HeroEffect.rejected(MovementRejection.KING_IN_CHECK);
        PieceState killed = killer.hidden(killer.color() == victim.color()
                ? PieceState.Status.DESTROYED : PieceState.Status.CAPTURED);
        Position capturedAt = killer.position() == null ? square : killer.position();
        Capture capture = new Capture(victim.id(), killer.id(), victim.color(),
                context.field().createFigure(killer.type(), capturedAt, killer.color()), capturedAt, true, Capture.Cause.ARES);
        return new HeroEffect(Success.SUCCESS, Map.of(spent.id(), spent, killed.id(), killed), List.of(capture));
    }

    @Override
    public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                  PieceState target, AbilityRequest request) {
        return "Ares: копьё в направлении " + cell(request.target());
    }

    private static PieceState at(Map<Long, PieceState> pieces, Position square) {
        return pieces.values().stream().filter(piece -> square.equals(piece.position())).findFirst().orElse(null);
    }

    private static boolean inside(Position square) {
        return square.getRow() >= 0 && square.getRow() < 8 && square.getColumn() >= 0 && square.getColumn() < 8;
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
