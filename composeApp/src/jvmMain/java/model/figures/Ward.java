package model.figures;

import java.util.List;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

/** Устанавливаемый объект: занимает клетку, не ходит и не атакует, допускает взятие. */
public final class Ward extends Figure {
    public Ward(Position position, FigureColor color, Field field) {
        super(position, color, field, "Ward", 1);
    }
    @Override public void fillMoves() {}
    @Override public synchronized void computeFutureMovesAndAttacks() { canMove = List.of(); canAttack = List.of(); }
    @Override public boolean canBeDisplaced() { return false; }
    @Override public boolean canUseAbilities() { return false; }
    @Override public boolean isClassicBody() { return false; }
    @Override public boolean canBeResurrected() { return false; }
}
