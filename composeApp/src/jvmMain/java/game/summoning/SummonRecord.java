package game.summoning;

import model.Position;

/**
 * A generated identity remains in the ledger after transformation, capture or replacement.
 */
public record SummonRecord(long id, long sourceId, long bornTurn, long transformsAfterTurn,
                           Position bornAt, Cause cause, boolean transformed) {
    public enum Cause {DEATH, ULTIMATE}

    public SummonRecord {
        if (id <= 0 || sourceId <= 0 || id <= sourceId || bornTurn < 1 || transformsAfterTurn <= bornTurn
                || bornAt == null || bornAt.getRow() < 0 || bornAt.getRow() > 7 || bornAt.getColumn() < 0 || bornAt.getColumn() > 7 || cause == null)
            throw new IllegalArgumentException("Invalid summon origin");
    }

    public SummonRecord mature() {
        return new SummonRecord(id, sourceId, bornTurn, transformsAfterTurn, bornAt, cause, true);
    }
}
