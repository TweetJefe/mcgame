package game.mechanics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Неизменяемый контейнер; отсутствующий ключ имеет стандартное состояние. */
public final class MechanicStates {
    private static final MechanicStates EMPTY = new MechanicStates(Map.of());
    private final Map<MechanicKey<?>, Object> values;

    private MechanicStates(Map<MechanicKey<?>, Object> values) { this.values = Map.copyOf(values); }
    public static MechanicStates empty() { return EMPTY; }
    public <S> S get(MechanicKey<S> key) {
        rejectAlias(key);
        return values.containsKey(key) ? key.checked(values.get(key)) : key.initial();
    }
    public <S> MechanicStates with(MechanicKey<S> key, S state) {
        rejectAlias(key);
        S checked = key.checked(state);
        var next = new HashMap<>(values);
        if (checked.equals(key.initial())) next.remove(key);
        else next.put(key, checked);
        return new MechanicStates(next);
    }
    public Set<MechanicKey<?>> keys() { return values.keySet(); }
    private void rejectAlias(MechanicKey<?> key) {
        for (var known : values.keySet())
            if (known != key && known.id().equals(key.id()))
                throw new IllegalArgumentException("Conflicting mechanic key: " + key.id());
    }
    @Override public boolean equals(Object other) {
        return other instanceof MechanicStates states && values.equals(states.values);
    }
    @Override public int hashCode() { return values.hashCode(); }
}
