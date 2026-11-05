package game.mechanics;

import java.util.Map;
import java.util.Objects;

public record MechanicEvent<P>(MechanicEventType<P> type, P data) {
    public MechanicEvent {
        Objects.requireNonNull(type, "type");
        data = type.type().cast(Objects.requireNonNull(data, "data"));
    }
    public String id() { return type.id(); }
    public Map<String, String> attributes() { return Map.copyOf(type.encoder().apply(data)); }
}
