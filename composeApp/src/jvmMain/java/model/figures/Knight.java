package model.figures;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

import java.util.ArrayList;


public final class Knight extends Figure {
    public Knight(Position position, FigureColor color, Field field) {
        super(position, color, field, "Knight", 3);
        fillMoves();
    }

    public Knight(FigureColor color) {
        super(null, color, null, "Knight", 3);
    }


    @Override
    public synchronized void fillMoves() {
        moves = new Position[]{
                new Position(2, -1).addPosition(this.position),
                new Position(2, 1).addPosition(this.position),
                new Position(-2, -1).addPosition(this.position),
                new Position(-2, 1).addPosition(this.position),
                new Position(1, -2).addPosition(this.position),
                new Position(-1, -2).addPosition(this.position),
                new Position(1, 2).addPosition(this.position),
                new Position(-1, 2).addPosition(this.position)
        };
    }

    @Override
    public synchronized void computeFutureMovesAndAttacks() {
        canMove = new ArrayList<>();
        canAttack = new ArrayList<>();
        for (var move : moves) {
            if (!checkMovePosition(color, move, field)) continue;
            var cellColor = field.getFigureByPosition(move).getColor();
            if (cellColor.equals(FigureColor.NONE)) {
                canMove.add(move);
            } else {
                canAttack.add(move);
            }
        }
    }
}
