package game.moves;


import model.Position;

import java.util.List;

/** Legal targets for the player whose turn it is. */
public final class AvailableMoves {
    private final List<Position> canMove;
    private final List<Position> canAttack;

    public AvailableMoves(List<Position> canMove, List<Position> canAttack) {
        this.canMove = List.copyOf(canMove);
        this.canAttack = List.copyOf(canAttack);
    }

    public List<Position> getCanMove() {
        return canMove;
    }

    public List<Position> getCanAttack() {
        return canAttack;
    }
}
