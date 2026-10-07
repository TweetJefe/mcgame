package model;

import model.figures.*;
import rules.field_filling.filling_rules.FillingRule8x8;
import rules.standard.PositionValidator;
import rules.standard.TriFunction;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable 8x8 board snapshot. Every square has an immutable placed piece or Empty. */
public final class Field implements FieldMethods {
    private final Map<Position, Figure> field;
    private static final Map<Class<? extends Figure>, TriFunction<Position, FigureColor, Field, Figure>> CONSTRUCTORS = Map.of(
            Pawn.class, Pawn::new, Knight.class, Knight::new, Bishop.class, Bishop::new,
            King.class, King::new, Queen.class, Queen::new, Rook.class, Rook::new, Empty.class, Empty::new);

    public Field(int size) {
        if (size != 8) throw new IllegalArgumentException("Only the standard 8x8 board is supported; requested " + size);
        var board = new HashMap<Position, Figure>();
        for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++) {
            Position position = new Position(row, column);
            board.put(position, createFigure(Empty.class, position, FigureColor.NONE));
        }
        var rules = new FillingRule8x8();
        rules.getWhiteRules().forEach((type, positions) -> positions.forEach(position ->
                board.put(position, createFigure(type.getClass(), position, type.getColor()))));
        rules.getBlackRules().forEach((type, positions) -> positions.forEach(position ->
                board.put(position, createFigure(type.getClass(), position, type.getColor()))));
        PositionValidator.validateBoard(board);
        field = Map.copyOf(board);
    }

    private Field(Map<Position, Figure> source, boolean classic) {
        if (source == null) throw new IllegalArgumentException("Board is required");
        var input = new HashMap<>(source);
        PositionValidator.validateBoard(input, classic);
        var board = new HashMap<Position, Figure>();
        input.forEach((position, piece) -> board.put(position, createFigure(piece.getClass(), position, piece.getColor())));
        field = Map.copyOf(board);
    }

    /** Validates a complete board and rebinds pieces to this new snapshot. */
    public static Field fromPosition(Map<Position, Figure> source) { return new Field(source, true); }
    /** Same structural guarantees, allowing pawns displaced onto edge ranks by Moba effects. */
    public static Field fromMobaPosition(Map<Position, Figure> source) { return new Field(source, false); }
    public static boolean supports(Class<? extends Figure> type) { return type != null && CONSTRUCTORS.containsKey(type); }

    /** Safe detached-piece factory. This never modifies the board. */
    public Figure createFigure(Class<? extends Figure> type, Position position, FigureColor color) {
        if (type == null) throw new IllegalArgumentException("Figure type is required");
        var constructor = CONSTRUCTORS.get(type);
        if (constructor == null) throw new IllegalArgumentException("Unregistered figure type: " + type);
        if (position == null) throw new IllegalArgumentException("Piece position is required");
        if (position.getRow() < 0 || position.getRow() >= 8 || position.getColumn() < 0 || position.getColumn() >= 8)
            throw new IllegalArgumentException("Square outside 8x8 board: " + position);
        if (color == null || (type == Empty.class) != (color == FigureColor.NONE))
            throw new IllegalArgumentException("Invalid piece color for " + type.getSimpleName());
        return constructor.apply(position, color, this);
    }
    public Map<Position, Figure> getGameField() { return field; }
    public int getSize() { return 8; }
    public Figure getFigureByPosition(Position position) { return position == null ? null : field.get(position); }
    @Override public void compute(Figure figure) {
        Objects.requireNonNull(figure, "figure");
        if (figure.field != this || figure.position == null) throw new IllegalArgumentException("Piece must belong to this snapshot");
        synchronized (figure) { figure.fillMoves(); figure.computeFutureMovesAndAttacks(); }
    }
}
