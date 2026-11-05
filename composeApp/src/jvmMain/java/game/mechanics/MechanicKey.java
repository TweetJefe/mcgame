package game.mechanics;

import java.util.Objects;

/** Типизированный ключ неизменяемого состояния. Один экземпляр ключа на модуль. */
public final class MechanicKey<S> {
    private final String id;
    private final Class<S> type;
    private final S initial;

    public MechanicKey(String id, Class<S> type, S initial) {
        if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_.-]*"))
            throw new IllegalArgumentException("Invalid mechanic ID");
        this.id = id;
        this.type = Objects.requireNonNull(type, "type");
        this.initial = checked(initial);
    }

    public String id() { return id; }
    public Class<S> type() { return type; }
    public S initial() { return initial; }
    public S checked(Object value) { return type.cast(Objects.requireNonNull(value, "state")); }
}
