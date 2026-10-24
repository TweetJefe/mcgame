package game.history;

/** Meaning of targetId in a successful ability's history entry, independent of targeting geometry. */
public enum AbilityEventTarget {
    REQUIRED, OPTIONAL, NONE;

    public boolean accepts(Long targetId) {
        return switch (this) {
            case REQUIRED -> targetId != null;
            case OPTIONAL -> true;
            case NONE -> targetId == null;
        };
    }
}
