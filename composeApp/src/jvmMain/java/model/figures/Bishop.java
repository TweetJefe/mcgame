package model.figures;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

import java.util.ArrayList;


public final class Bishop extends Figure {

    public Bishop(Position position, FigureColor color, Field field) {
        super(position, color, field, "Bishop", 5);
        fillMoves();
    }

    public Bishop(FigureColor color){
        super(null, color, null, "Bishop", 5);
    }


    @Override
    public synchronized void fillMoves() {
        moves = new Position[]{
                new Position(1, 1),
                new Position(1, -1),
                new Position(-1, 1),
                new Position(-1, -1)
        };
    }

    @Override
    public synchronized void computeFutureMovesAndAttacks() {
        canMove = new ArrayList<>();
        canAttack = new ArrayList<>();
        for(var move : moves){
            var positionToUpdate = this.position;
            while(true) {
                positionToUpdate = positionToUpdate.addPosition(move);
                if(!checkMovePosition(this.color, positionToUpdate, this.field)){
                    break;
                }else{
                    if(field.getFigureByPosition(positionToUpdate).getColor().equals(FigureColor.NONE)){
                        canMove.add(positionToUpdate);
                    }else{
                        canAttack.add(positionToUpdate);
                        break;
                    }
                }

            }
        }
    }
}
