package model.figures;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

public final class Empty extends Figure {
    public Empty(Position position, FigureColor color, Field field) {
        super(position, color, field, "Empty", 0);
        fillMoves();}

    public Empty(FigureColor color) {
        super(null, color, null, "Empty", 0);
    }


    @Override
    public synchronized void fillMoves() {

    }

    @Override
    public synchronized void computeFutureMovesAndAttacks() {

    }
}
