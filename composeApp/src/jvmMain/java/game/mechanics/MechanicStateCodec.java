package game.mechanics;

import java.util.Map;

/** Явная схема состояния; имена Java-классов в сохранениях не используются. */
public interface MechanicStateCodec<S> {
    Map<String, String> encode(S state);
    S decode(Map<String, String> attributes);
}
