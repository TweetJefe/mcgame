package model;

import rules.standard.ShowMoves;
import java.util.List;
import java.util.Objects;

/** Piece identity and placement are immutable. Cached pseudo moves are derived data only. */
public abstract class Figure implements ShowMoves {
    protected final Position position;
    protected final FigureColor color;
    protected final Field field;
    protected final String name;
    protected final int points;
    protected List<Position> canMove = List.of();
    protected List<Position> canAttack = List.of();
    protected Position[] moves = new Position[0];

    protected Figure(Position position, FigureColor color, Field field, String name, int points) {
        this.position = position;
        this.color = Objects.requireNonNull(color, "color");
        this.field = field;
        this.name = name;
        this.points = points;
    }
    public final Position getPosition() { return position; }
    public final FigureColor getColor() { return color; }
    public final synchronized List<Position> getCanMove() { return List.copyOf(canMove); }
    public final synchronized List<Position> getCanAttack() { return List.copyOf(canAttack); }
    public final String getName() { return name; }
    public final int getPoints() { return points; }
}
