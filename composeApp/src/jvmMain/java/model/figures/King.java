package model.figures;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

import java.util.ArrayList;

public final class King extends Figure {

    public King(Position position, FigureColor color, Field field) {
        super(position, color, field, "King", 10);
        fillMoves();
    }

    public King( FigureColor color){
        super(null, color, null, "King", 10);
    }



    @Override
    public synchronized void fillMoves() {
        moves = new Position[]{
                new Position(0, -1),
                new Position(0, 1),
                new Position(1, 0),
                new Position(-1, 0),
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
            positionToUpdate = positionToUpdate.addPosition(move);
            if(!checkMovePosition(this.color, positionToUpdate, this.field)){
                continue;
            }else{
                if(field.getFigureByPosition(positionToUpdate).getColor().equals(FigureColor.NONE)){
                    canMove.add(positionToUpdate);
                }else{
                    canAttack.add(positionToUpdate);
                    continue;
                }
            }


        }
    }
}
