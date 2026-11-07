package model.figures;

import java.util.LinkedHashSet;
import java.util.List;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

/** Составная шахматная фигура: объединение ходов слона и коня. */
public final class Archbishop extends Figure {
    public Archbishop(Position position, FigureColor color, Field field) {
        super(position, color, field, "Archbishop", 3);
    }
    @Override public void fillMoves() {}
    @Override public synchronized void computeFutureMovesAndAttacks() {
        var bishop = new Bishop(position, color, field);
        var knight = new Knight(position, color, field);
        field.compute(bishop);
        field.compute(knight);
        var free = new LinkedHashSet<>(bishop.getCanMove());
        free.addAll(knight.getCanMove());
        var attacks = new LinkedHashSet<>(bishop.getCanAttack());
        attacks.addAll(knight.getCanAttack());
        canMove = List.copyOf(free);
        canAttack = List.copyOf(attacks);
    }
    @Override public boolean isClassicBody() { return false; }
}
