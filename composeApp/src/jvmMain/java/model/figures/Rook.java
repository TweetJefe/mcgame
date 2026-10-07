package model.figures;

import model.*;

import java.util.ArrayList;

public final class Rook extends Figure {
    public Rook(Position position, FigureColor color, Field field) {
        super(position, color, field, "Rook", 3);
        fillMoves();
    }

    public Rook(FigureColor color){
        super(null, color, null, "Rook", 3);
    }


    @Override
    public synchronized void fillMoves() {
        moves = new Position[]{
                new Position(0, -1),
                new Position(0, 1),
                new Position(1, 0),
                new Position(-1, 0),

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
