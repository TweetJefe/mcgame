package game.mechanics;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Стабильный ID, тип данных и явный кодек одного события механики. */
public record MechanicEventType<P>(String id, Class<P> type,
        Function<P, Map<String, String>> encoder, Function<Map<String, String>, P> decoder,
        MechanicEventPolicy policy) {
    public MechanicEventType(String id, Class<P> type, Function<P, Map<String, String>> encoder,
                             Function<Map<String, String>, P> decoder) {
        this(id, type, encoder, decoder, MechanicEventPolicy.action());
    }
    public MechanicEventType {
        Objects.requireNonNull(policy, "policy");
        if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_.-]*"))
            throw new IllegalArgumentException("Invalid mechanic event ID");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(decoder, "decoder");
    }
    public MechanicEvent<P> event(P data) { return new MechanicEvent<>(this, data); }
    public MechanicEvent<P> decode(Map<String, String> attributes) {
        return event(decoder.apply(Map.copyOf(attributes)));
    }
}
