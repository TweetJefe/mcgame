package model.figures;

import java.util.List;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

/** Неподвижное тело: его можно взять, но нельзя переместить или активировать. */
public final class Egg extends Figure {
    public Egg(Position position, FigureColor color, Field field) {
        super(position, color, field, "Egg", 3);
    }
    @Override public void fillMoves() {}
    @Override public synchronized void computeFutureMovesAndAttacks() {
        canMove = List.of();
        canAttack = List.of();
    }
    @Override public boolean canBeDisplaced() { return false; }
    @Override public boolean canUseAbilities() { return false; }
    @Override public boolean isClassicBody() { return false; }
}
