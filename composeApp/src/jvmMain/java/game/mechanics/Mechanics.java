package game.mechanics;

import game.moves.MoveResult;
import game.moves.Success;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/** Короткие фабрики шагов с явными зависимостями. */
public final class Mechanics {
    private Mechanics() {}
    public static MechanicStep step(String id, MechanicPhase phase, String after,
                                     Function<MechanicSession, MoveResult> action) {
        return new MechanicStep(id, phase, Set.of(), after == null ? Set.of() : Set.of(after), action);
    }
    public static MechanicStep update(String id, MechanicPhase phase, String after,
                                      Consumer<MechanicSession> action) {
        return step(id, phase, after, session -> { action.accept(session); return Success.SUCCESS; });
    }
    public static <O extends MechanicOperation> MechanicHandler<O> handler(Class<O> type,
            String id, MechanicPhase phase, String after, BiFunction<MechanicSession, O, MoveResult> action) {
        return new MechanicHandler<>(type, id, phase, Set.of(), after == null ? Set.of() : Set.of(after), action);
    }
}
