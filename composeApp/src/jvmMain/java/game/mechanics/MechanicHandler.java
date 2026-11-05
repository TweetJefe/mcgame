package game.mechanics;

import game.moves.MoveResult;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

public record MechanicHandler<O extends MechanicOperation>(Class<O> type, String id,
        MechanicPhase phase, Set<String> before, Set<String> after,
        BiFunction<MechanicSession, O, MoveResult> action) {
    public MechanicHandler {
        Objects.requireNonNull(type, "type");
        before = Set.copyOf(before);
        after = Set.copyOf(after);
        Objects.requireNonNull(action, "action");
    }
    MechanicStep step() {
        return new MechanicStep(id, phase, before, after, session -> session.handle(type, action));
    }
}
