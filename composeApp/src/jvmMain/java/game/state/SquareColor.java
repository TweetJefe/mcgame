package game.state;

import model.Position;

public enum SquareColor {
    DARK, LIGHT;
    public static SquareColor at(Position square) {
        return (square.getRow() + square.getColumn()) % 2 == 0 ? DARK : LIGHT;
    }
}
