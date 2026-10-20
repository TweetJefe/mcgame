package game.heroes.ginseng;

import game.heroes.Ability;

public record BorrowedAbility(Ability ability, long sourceId, long copiedTurn) {
    public BorrowedAbility {
        if (ability == null || sourceId <= 0 || copiedTurn < 1 || copiedTurn > Long.MAX_VALUE - 2)
            throw new IllegalArgumentException("Invalid borrowed ability");
    }

    public boolean isReady(long turn) {
        return turn >= copiedTurn + 2;
    }
}
