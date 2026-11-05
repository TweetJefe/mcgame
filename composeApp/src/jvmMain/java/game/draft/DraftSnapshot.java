package game.draft;

import game.heroes.HeroType;
import model.FigureColor;
import model.Position;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Objects;

/**
 * The original roster is independent of the live pieces, captures and promotions.
 */
public record DraftSnapshot(long version, DraftRules rules, DraftPhase phase, FigureColor active,
                            List<HeroType> bans, Map<FigureColor, List<HeroPick>> picks,
                            Set<FigureColor> finished, List<DraftEvent> events, DraftCatalog catalog) {
    public DraftSnapshot(long version, DraftRules rules, DraftPhase phase, FigureColor active,
                         List<HeroType> bans, Map<FigureColor, List<HeroPick>> picks,
                         Set<FigureColor> finished, List<DraftEvent> events) {
        this(version, rules, phase, active, bans, picks, finished, events, HeroCatalog.catalog(rules.catalogVersion()));
    }
    public DraftSnapshot {
        Objects.requireNonNull(rules);
        Objects.requireNonNull(catalog);
        if (2 * rules.bansPerSide() + 2 > catalog.heroes().size())
            throw new IllegalArgumentException("Bans must leave at least two heroes available");
        Objects.requireNonNull(phase);
        Objects.requireNonNull(active);
        bans = List.copyOf(bans);
        finished = Set.copyOf(finished);
        events = List.copyOf(events);
        picks = Map.of(FigureColor.WHITE, List.copyOf(picks.get(FigureColor.WHITE)), FigureColor.BLACK, List.copyOf(picks.get(FigureColor.BLACK)));
        if (version < 0 || events.size() > 100 || bans.size() > 20 || picks.values().stream().anyMatch(p -> p.size() > 14))
            throw new IllegalArgumentException("Invalid draft size");
    }

    public boolean isComplete() {
        return phase == DraftPhase.COMPLETE;
    }

    public List<HeroPick> picksFor(FigureColor side) {
        return picks.getOrDefault(side, List.of());
    }

    public boolean isPicked(HeroType hero) {
        return picks.values().stream().flatMap(List::stream).anyMatch(p -> p.hero() == hero);
    }

    public Map<Position, HeroType> assignments() {
        var result = new HashMap<Position, HeroType>();
        for (var roster : picks.values())
            for (HeroPick pick : roster) {
                if (result.put(pick.square(), pick.hero()) != null)
                    throw new IllegalArgumentException("Two heroes on one starting square");
            }
        return Map.copyOf(result);
    }
}
