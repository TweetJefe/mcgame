package game.mechanics.compat;

import game.history.GameEvent;
import game.mechanics.MechanicEvent;
import game.mechanics.MechanicEventPolicy;
import game.mechanics.MechanicEventType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import model.Position;

/** Замороженное соответствие прежних категорий. Новые события регистрирует их модуль. */
public final class HistoricalEvents {
    private HistoricalEvents() {}
    public record Participants(Long sourceId, Long targetId, Position from, Position to) {}
    private static final Map<GameEvent.Kind, MechanicEventType<Participants>> TYPES = types();
    private static Map<GameEvent.Kind, MechanicEventType<Participants>> types() {
        var result = new EnumMap<GameEvent.Kind, MechanicEventType<Participants>>(GameEvent.Kind.class);
        for (var kind : List.of(GameEvent.Kind.YARN_STUN, GameEvent.Kind.LIGHTNING_HIT,
                GameEvent.Kind.LIGHTNING_RESET, GameEvent.Kind.LIGHTNING_DISCHARGE, GameEvent.Kind.LIGHTNING_CAPTURE,
                GameEvent.Kind.MINE_TRIGGER, GameEvent.Kind.MINE_CAPTURE, GameEvent.Kind.BLAST_AREA,
                GameEvent.Kind.BLAST_CAPTURE, GameEvent.Kind.BLAST_DEATH, GameEvent.Kind.ILLUSION_HIT,
                GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE, GameEvent.Kind.YAGA_BIRTH,
                GameEvent.Kind.YAGA_MATURE, GameEvent.Kind.REPLACE, GameEvent.Kind.STUN, GameEvent.Kind.ARES_REVENGE)) {
            result.put(kind, new MechanicEventType<>("legacy." + kind.name().toLowerCase(Locale.ROOT),
                    Participants.class, p -> Map.of("source", Long.toString(p.sourceId()),
                    "target", Long.toString(p.targetId()), "from", cell(p.from()), "to", cell(p.to())),
                    a -> new Participants(Long.parseLong(a.get("source")), Long.parseLong(a.get("target")),
                            square(a.get("from")), square(a.get("to"))),
                    new MechanicEventPolicy(activeTurn(kind), true, true, true)));
        }
        return Map.copyOf(result);
    }
    public static boolean isLegacy(GameEvent.Kind kind) { return TYPES.containsKey(kind); }
    public static MechanicEventType<Participants> type(GameEvent.Kind kind) {
        return Objects.requireNonNull(TYPES.get(kind), "legacy event");
    }
    public static List<MechanicEventType<?>> types(GameEvent.Kind... kinds) {
        return Arrays.stream(kinds).<MechanicEventType<?>>map(HistoricalEvents::type).toList();
    }
    public static MechanicEvent<?> effect(GameEvent.Kind kind, Long actor, Long target, Position from, Position to) {
        return type(kind).event(new Participants(actor, target, from, to));
    }
    public static GameEvent.Kind kind(MechanicEvent<?> effect) {
        if (effect == null) return null;
        for (var entry : TYPES.entrySet()) if (entry.getValue() == effect.type()) return entry.getKey();
        return null;
    }
    public static boolean requiresActiveTurn(GameEvent event) {
        return activeTurn(kind(event.effect()));
    }
    private static boolean activeTurn(GameEvent.Kind kind) {
        return kind == null || !List.of(GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE,
                GameEvent.Kind.YAGA_BIRTH, GameEvent.Kind.ARES_REVENGE, GameEvent.Kind.ILLUSION_HIT,
                GameEvent.Kind.MINE_TRIGGER, GameEvent.Kind.MINE_CAPTURE, GameEvent.Kind.BLAST_CAPTURE,
                GameEvent.Kind.BLAST_DEATH, GameEvent.Kind.LIGHTNING_CAPTURE).contains(kind);
    }
    private static String cell(Position p) { return "" + (char) ('a' + p.getColumn()) + (p.getRow() + 1); }
    private static Position square(String cell) {
        if (cell == null || !cell.matches("[a-h][1-8]")) throw new IllegalArgumentException("Invalid square");
        return new Position(cell.charAt(1) - '1', cell.charAt(0) - 'a');
    }
}
