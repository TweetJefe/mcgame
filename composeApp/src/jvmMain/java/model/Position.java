package model;

import java.util.Objects;

/**
 * Immutable board coordinate; safe as a map key and in API results.
 */
public final class Position {
    private final int row;
    private final int column;

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public Position addPosition(Position pos) {
        return new Position(row + pos.row, column + pos.column);
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Position pos && row == pos.row && column == pos.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + column + ")";
    }
}
