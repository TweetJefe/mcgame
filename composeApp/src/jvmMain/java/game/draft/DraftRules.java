package game.draft;

/**
 * A small public draft now; the roster can grow without requiring six picks from the current small hero catalog.
 */
public record DraftRules(int bansPerSide, int maxPicksPerSide, int catalogVersion) {
    public DraftRules(int bansPerSide, int maxPicksPerSide) {
        this(bansPerSide, maxPicksPerSide, 5);
    }

    public DraftRules {
        if (catalogVersion < 1 || catalogVersion > 5)
            throw new IllegalArgumentException("Unknown hero catalog version");
        if (bansPerSide < 0 || bansPerSide > 10 || maxPicksPerSide < 1 || maxPicksPerSide > 14)
            throw new IllegalArgumentException("Invalid draft limits");
        if (2 * bansPerSide + 2 > HeroCatalog.heroes(catalogVersion).size())
            throw new IllegalArgumentException("Bans must leave at least two heroes available");
    }

    public static DraftRules prototype() {
        return new DraftRules(1, 6);
    }
}
