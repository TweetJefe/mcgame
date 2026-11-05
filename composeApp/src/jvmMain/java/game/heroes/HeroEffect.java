package game.heroes;

import game.mechanics.MechanicOperation;
import game.moves.Capture;
import game.moves.MoveResult;
import game.moves.Success;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;

/**
 * Immutable proposed changes. They never modify a live Game or the supplied state.
 */
public record HeroEffect(MoveResult result, Map<Long, PieceState> updates, List<Capture> captures,
                         List<MechanicOperation> operations, AbilityCost cost) {
    public HeroEffect(MoveResult result, Map<Long, PieceState> updates, List<Capture> captures,
                      List<MechanicOperation> operations) {
        this(result, updates, captures, operations, AbilityCost.NONE);
    }
    public HeroEffect(MoveResult result, Map<Long, PieceState> updates, List<Capture> captures) {
        this(result, updates, captures, List.of());
    }

    public HeroEffect {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(cost, "cost");
        operations = List.copyOf(operations);
        updates = Map.copyOf(updates);
        captures = List.copyOf(captures);
        if (!result.isSuccess() && (!updates.isEmpty() || !captures.isEmpty() || !operations.isEmpty()
                || !cost.equals(AbilityCost.NONE)))
            throw new IllegalArgumentException("Rejected effect contains changes");
    }

    public static HeroEffect rejected(MoveResult result) {
        return new HeroEffect(result, Map.of(), List.of());
    }

    public static HeroEffect success(Map<Long, PieceState> updates) {
        return new HeroEffect(Success.SUCCESS, updates, List.of());
    }

    public HeroEffect withOperations(MechanicOperation... additions) {
        var combined = new ArrayList<>(operations);
        combined.addAll(List.of(additions));
        return new HeroEffect(result, updates, captures, combined, cost);
    }

    public HeroEffect withCost(AbilityCost cost) {
        return new HeroEffect(result, updates, captures, operations, cost);
    }

    /** Стоимость учитывается один раз, до операций и последующих наград за взятия. */
    public HeroEffect applyCost(PieceState actor, AbilityUse use) {
        if (!result.isSuccess() || cost.equals(AbilityCost.NONE)) return this;
        PieceState proposed = updates.getOrDefault(actor.id(), actor);
        PieceState paid = cost.apply(proposed, use);
        var next = new HashMap<>(updates);
        next.put(actor.id(), paid);
        return new HeroEffect(result, next, captures, operations);
    }

    public static HeroEffect empty() {
        return success(Map.of());
    }
}
