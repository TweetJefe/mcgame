package game.mechanics;

import game.heroes.HeroRegistry;
import game.heroes.HeroType;
import game.state.GameSnapshot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Личность на момент события; текущий герой не переписывает прошлые применения. */
public final class PieceHeroHistory {
    private final Map<Long, NavigableMap<Long, HeroType>> timelines;

    private PieceHeroHistory(Map<Long, NavigableMap<Long, HeroType>> timelines) {
        this.timelines = timelines;
    }
    public static PieceHeroHistory replay(GameSnapshot snapshot) {
        var mechanics = HeroRegistry.fromRules(snapshot.heroRules()).mechanics();
        var timelines = new HashMap<Long, NavigableMap<Long, HeroType>>();
        for (var piece : snapshot.pieces().values()) {
            var timeline = new TreeMap<Long, HeroType>();
            timeline.put(0L, mechanics.initialHero(snapshot, piece));
            timelines.put(piece.id(), timeline);
        }
        for (var event : snapshot.events()) {
            var changed = new HashSet<Long>();
            for (var change : mechanics.heroChanges(event)) {
                var timeline = timelines.get(change.pieceId());
                if (timeline == null || !changed.add(change.pieceId()) || timeline.lastEntry().getValue() != change.before())
                    throw new IllegalArgumentException("Hero change differs from preceding identity");
                timeline.put(event.sequence(), change.after());
            }
        }
        return new PieceHeroHistory(timelines);
    }
    public HeroType before(long id, long sequence) {
        var timeline = timelines.get(id);
        if (timeline == null || sequence < 1) throw new IllegalArgumentException("Unknown hero history");
        return timeline.lowerEntry(sequence).getValue();
    }
    public boolean hasHeld(long id, HeroType hero) { return timelines.get(id).containsValue(hero); }
    public HeroType initial(long id) { return timelines.get(id).firstEntry().getValue(); }
    public HeroType latest(long id) { return timelines.get(id).lastEntry().getValue(); }
}
