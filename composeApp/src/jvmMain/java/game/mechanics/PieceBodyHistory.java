package game.mechanics;

import game.heroes.HeroRegistry;
import game.history.GameEvent;
import game.state.GameSnapshot;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import model.Figure;
import model.figures.Pawn;

/** Неизменяемая история форм: обычные превращения и переходы зарегистрированных механик. */
public final class PieceBodyHistory {
    private final Map<Long, NavigableMap<Long, Class<? extends Figure>>> timelines;

    private PieceBodyHistory(Map<Long, NavigableMap<Long, Class<? extends Figure>>> timelines) {
        var frozen = new HashMap<Long, NavigableMap<Long, Class<? extends Figure>>>();
        timelines.forEach((id, timeline) -> frozen.put(id,
                Collections.unmodifiableNavigableMap(new TreeMap<>(timeline))));
        this.timelines = Map.copyOf(frozen);
    }

    /** Для героев с единственным начальным типом он берётся из описания реестра партии. */
    public static PieceBodyHistory forPieces(GameSnapshot snapshot, Set<Long> ids) {
        var registry = HeroRegistry.fromRules(snapshot.heroRules());
        var initial = new HashMap<Long, Class<? extends Figure>>();
        for (long id : ids) {
            var piece = snapshot.pieces().get(id);
            if (piece == null) throw new IllegalArgumentException("Unknown piece in body history");
            var birth = registry.mechanics().birth(snapshot, piece);
            if (birth != null) { initial.put(id, birth.body().type()); continue; }
            var initialHero = registry.mechanics().initialHero(snapshot, piece);
            var supported = registry.get(initialHero == game.heroes.HeroType.NONE ? piece.hero() : initialHero)
                    .definition().supportedBodies();
            if (supported.size() != 1)
                throw new IllegalArgumentException("Body history needs an explicit initial body for this piece");
            initial.put(id, supported.iterator().next());
        }
        return replay(snapshot, initial, registry.mechanics());
    }

    /** Явные исходные формы подходят также описаниям с несколькими допустимыми телами. */
    public static PieceBodyHistory replay(GameSnapshot snapshot, Map<Long, Class<? extends Figure>> initial) {
        return replay(snapshot, initial, HeroRegistry.fromRules(snapshot.heroRules()).mechanics());
    }

    private static PieceBodyHistory replay(GameSnapshot snapshot, Map<Long, Class<? extends Figure>> initial,
                                            MechanicRegistry mechanics) {
        var bodies = new HashMap<>(Map.copyOf(initial));
        var timelines = new HashMap<Long, NavigableMap<Long, Class<? extends Figure>>>();
        initial.forEach((id, body) -> {
            if (!snapshot.pieces().containsKey(id)) throw new IllegalArgumentException("Unknown initial body identity");
            var timeline = new TreeMap<Long, Class<? extends Figure>>();
            timeline.put(0L, body);
            timelines.put(id, timeline);
        });
        int moveIndex = 0;
        for (var event : snapshot.events()) {
            if (event.kind() == GameEvent.Kind.MOVE) {
                var move = snapshot.moves().get(moveIndex++);
                if (bodies.containsKey(event.actorId()) && move.getPromotion() != null) {
                    if (bodies.get(event.actorId()) != Pawn.class)
                        throw new IllegalArgumentException("Only a pawn can promote on the last rank");
                    apply(new PieceBodyChange(event.actorId(), Pawn.class, move.getPromotion().getFigureType()),
                            event.sequence(), bodies, timelines);
                }
            }
            var changed = new HashSet<Long>();
            for (var change : mechanics.bodyChanges(event, Map.copyOf(bodies))) {
                if (!bodies.containsKey(change.pieceId())) continue;
                if (!changed.add(change.pieceId())) throw new IllegalArgumentException("Conflicting body changes");
                apply(change, event.sequence(), bodies, timelines);
            }
        }
        return new PieceBodyHistory(timelines);
    }

    private static void apply(PieceBodyChange change, long sequence, Map<Long, Class<? extends Figure>> bodies,
            Map<Long, NavigableMap<Long, Class<? extends Figure>>> timelines) {
        if (bodies.get(change.pieceId()) != change.before())
            throw new IllegalArgumentException("Body change differs from preceding form");
        bodies.put(change.pieceId(), change.after());
        timelines.get(change.pieceId()).put(sequence, change.after());
    }

    public Class<? extends Figure> before(long pieceId, long eventSequence) {
        var timeline = timelines.get(pieceId);
        if (timeline == null || eventSequence <= 0) throw new IllegalArgumentException("Unknown body history position");
        return timeline.lowerEntry(eventSequence).getValue();
    }

    public Class<? extends Figure> latest(long pieceId) {
        var timeline = timelines.get(pieceId);
        if (timeline == null) throw new IllegalArgumentException("Unknown body history identity");
        return timeline.lastEntry().getValue();
    }
}
