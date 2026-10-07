package rules.standard;

import model.Field;
import model.FigureColor;
import model.Position;

import java.util.List;

public interface ShowMoves {
    void fillMoves();
    void computeFutureMovesAndAttacks();


    default boolean checkMovePosition(FigureColor color, Position position, Field field) {
        if(position.getRow() >= field.getSize() || position.getRow() < 0) return false;
        if(position.getColumn() >= field.getSize() || position.getColumn() < 0) return false;
        return !field.getFigureByPosition(position).getColor().equals(color);
    }

    default void printMovesAndAttackPositions(String figureName, Position pos, List<Position> canMove, List<Position> canAttack){
        System.out.println(figureName + " on position (" + pos.getRow() + ":" + pos.getColumn() + ")\nCan move on:");
        canMove.forEach(move -> {
            System.out.println("(" + move.getRow() + ":" + move.getColumn() + ")");
        });
        System.out.println("Can attack on:");
        canAttack.forEach(move -> {
            System.out.println("(" + move.getRow() + ":" + move.getColumn() + ")");
        });
    }
}
