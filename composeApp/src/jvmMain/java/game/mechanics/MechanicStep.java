package game.mechanics;

import game.moves.MoveResult;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record MechanicStep(String id, MechanicPhase phase, Set<String> before, Set<String> after,
                           Function<MechanicSession, MoveResult> action) {
    public MechanicStep {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Blank step ID");
        Objects.requireNonNull(phase, "phase");
        before = Set.copyOf(before);
        after = Set.copyOf(after);
        Objects.requireNonNull(action, "action");
    }
    public MechanicStep(String id, MechanicPhase phase, Function<MechanicSession, MoveResult> action) {
        this(id, phase, Set.of(), Set.of(), action);
    }
}
