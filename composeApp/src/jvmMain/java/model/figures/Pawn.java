package model.figures;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

import java.util.ArrayList;

public final class Pawn extends Figure {
    private Position[] attacks;
    public Pawn(Position position, FigureColor color, Field field) {
        super(position, color, field, "Pawn", 1);
        fillMoves();
    }

    public Pawn(FigureColor color){
        super(null, color, null, "Pawn", 1);
    }


    @Override
    public synchronized void fillMoves() {
        int factor = (color == FigureColor.WHITE) ?  1 : -1;
        attacks = new Position[]{
                new Position(factor, 1).addPosition(this.position),
                new Position(factor, -1).addPosition(this.position)
        };
        moves = new Position[]{
                new Position(factor, 0).addPosition(this.position),
                new Position(2 * factor, 0).addPosition(this.position)
        };
    }

    @Override
    public synchronized void computeFutureMovesAndAttacks() {
        canMove = new ArrayList<>();
        canAttack = new ArrayList<>();
        int startRow = color == FigureColor.WHITE ? 1 : field.getSize() - 2;
        int moveCount = position.getRow() == startRow ? 2 : 1;
        for(int i = 0; i < moveCount; ++i){
            if(!checkMovePosition(color, moves[i], field)) break;
            if(field.getFigureByPosition(moves[i]).getColor() != FigureColor.NONE) break;
            canMove.add(moves[i]);
        }
        for(var attack : attacks){
            if(!checkMovePosition(color, attack, field)) continue;
            if(!field.getFigureByPosition(attack).getColor().equals(FigureColor.NONE)){
                canAttack.add(attack);
            }
        }
    }
}
