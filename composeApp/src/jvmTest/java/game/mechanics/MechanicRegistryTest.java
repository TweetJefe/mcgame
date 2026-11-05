package game.mechanics;

import game.heroes.HeroRegistry;
import game.heroes.HeroActionContext;
import model.Field;
import model.FigureColor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.Mechanics.update;
import static org.junit.Assert.*;

public class MechanicRegistryTest {
    private enum Unit { VALUE }
    private static MechanicDefinition<Unit> module(String id, List<MechanicStep> steps) {
        var key = new MechanicKey<>(id, Unit.class, Unit.VALUE);
        return new MechanicDefinition<>() {
            @Override public MechanicKey<Unit> key() { return key; }
            @Override public List<MechanicStep> steps() { return List.copyOf(steps); }
        };
    }
    private static MechanicSession session() {
        var heroes = HeroRegistry.standard();
        return new MechanicSession(new HeroActionContext(new Field(8), Map.of(), 1), Map.of(),
                heroes.rules(), heroes, FigureColor.WHITE, null, List.of());
    }

    @Test public void beforeAfterOrderingIsStableAndSharedModuleIsExecutedOnce() {
        var calls = new ArrayList<String>();
        var first = module("first", List.of(update("a", ABILITY_PREPARE, null, s -> calls.add("a"))));
        var last = module("last", List.of(new MechanicStep("c", ABILITY_PREPARE, Set.of(), Set.of("b"),
                s -> { calls.add("c"); return game.moves.Success.SUCCESS; })));
        var middle = module("middle", List.of(new MechanicStep("b", ABILITY_PREPARE, Set.of("c"), Set.of("a"),
                s -> { calls.add("b"); return game.moves.Success.SUCCESS; })));
        for (var modules : List.of(List.of(last, middle, first, first), List.of(first, last, middle))) {
            calls.clear();
            new MechanicRegistry(modules).run(ABILITY_PREPARE, session());
            assertEquals(List.of("a", "b", "c"), calls);
        }
    }

    @Test public void conflictingIdsMissingStepsAndDependencyCyclesFailBeforePlay() {
        var empty = module("same", List.of());
        assertThrows(IllegalArgumentException.class, () -> new MechanicRegistry(List.of(empty, module("same", List.of()))));
        var missing = module("missing", List.of(update("a", ABILITY_PREPARE, "absent", s -> {})));
        assertThrows(IllegalArgumentException.class, () -> new MechanicRegistry(List.of(missing)));
        var cycle = module("cycle", List.of(update("a", ABILITY_PREPARE, "b", s -> {}),
                update("b", ABILITY_PREPARE, "a", s -> {})));
        assertThrows(IllegalArgumentException.class, () -> new MechanicRegistry(List.of(cycle)));
        var key = new MechanicKey<>("recursive", Unit.class, Unit.VALUE);
        MechanicDefinition<Unit> recursive = new MechanicDefinition<>() {
            @Override public MechanicKey<Unit> key() { return key; }
            @Override public List<MechanicDefinition<?>> dependencies() { return List.of(this); }
        };
        assertThrows(IllegalArgumentException.class, () -> new MechanicRegistry(List.of(recursive)));
    }

    @Test public void typedStateRejectsAliasKeysAndKeepsTheOriginalSnapshot() {
        var key = new MechanicKey<>("counter", Integer.class, 0);
        var initial = MechanicStates.empty();
        var changed = initial.with(key, 7);
        assertEquals(Integer.valueOf(0), initial.get(key));
        assertEquals(Integer.valueOf(7), changed.get(key));
        assertThrows(IllegalArgumentException.class, () -> changed.get(new MechanicKey<>("counter", String.class, "")));
        assertThrows(UnsupportedOperationException.class, () -> changed.keys().clear());
    }
}
