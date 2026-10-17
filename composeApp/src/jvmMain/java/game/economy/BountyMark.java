package game.economy;

/** A mark follows the target's persistent identity, including swallowing and promotion. */
public record BountyMark(long sourceId, long targetId, long expiresAfterTurn) {
    public BountyMark {
        if (sourceId <= 0 || targetId <= 0 || sourceId == targetId || expiresAfterTurn <= 0)
            throw new IllegalArgumentException("Invalid bounty mark");
    }
}
