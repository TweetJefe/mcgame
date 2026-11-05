package game.mechanics;

import game.heroes.HeroEffect;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Реакция состояния механики после собственной реакции героя; порядок задаётся зависимостями. */
public record MechanicCaptureReaction(String id, Set<String> before, Set<String> after,
                                     Function<DeathContext, HeroEffect> action) {
    public MechanicCaptureReaction {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Blank reaction ID");
        before = Set.copyOf(before);
        after = Set.copyOf(after);
        Objects.requireNonNull(action, "action");
    }
    public MechanicCaptureReaction(String id, Function<DeathContext, HeroEffect> action) {
        this(id, Set.of(), Set.of(), action);
    }
}
