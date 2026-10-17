package game.history;

import game.moves.MoveKind;
import game.moves.Promotion;
import game.state.ClockSnapshot;

import model.Position;
import model.FigureColor;

public class MadeMove {
    private final String figure;
    private final Position from, to;
    private final FigureColor color;
    private final MoveKind kind;
    private final Promotion promotion;
    private final String capturedFigure;
    private final Position captureSquare;
    private final int ply;
    private final ClockSnapshot clock;

    public MadeMove(String figure, Position from, Position to) {
        this(figure, FigureColor.NONE, from, to, MoveKind.NORMAL, null, null, null, 0, null);
    }

    public MadeMove(String figure, FigureColor color, Position from, Position to, MoveKind kind,
                    Promotion promotion, String capturedFigure, Position captureSquare, int ply, ClockSnapshot clock) {
        this.figure = figure;
        this.from = new Position(from.getRow(), from.getColumn());
        this.to = new Position(to.getRow(), to.getColumn());
        this.color = color;
        this.kind = kind;
        this.promotion = promotion;
        this.capturedFigure = capturedFigure;
        this.captureSquare = copy(captureSquare);
        this.ply = ply;
        this.clock = clock;
    }

    public String getFigure() { return figure; }
    public FigureColor getColor() { return color; }
    public Position getFrom() { return copy(from); }
    public Position getTo() { return copy(to); }
    public MoveKind getKind() { return kind; }
    public Promotion getPromotion() { return promotion; }
    public String getCapturedFigure() { return capturedFigure; }
    public Position getCaptureSquare() { return copy(captureSquare); }
    public int getPly() { return ply; }
    public ClockSnapshot getClock() { return clock; }
    private static Position copy(Position position) { return position == null ? null : new Position(position.getRow(), position.getColumn()); }

    @Override
    public String toString() {
        String suffix = kind == MoveKind.CASTLING ? (to.getColumn() == 6 ? " [O-O]" : " [O-O-O]")
                : kind == MoveKind.EN_PASSANT ? " [взятие на проходе]" : "";
        if (promotion != null) suffix += " = " + promotion.getFigureType().getSimpleName();
        return figure + ": (" + (from.getRow() + 1) + ", " + convertColumnToCharacter(from.getColumn()) + ") -> (" + (to.getRow() + 1) + ", " + convertColumnToCharacter(to.getColumn()) + ")" + suffix;
    }

    private char convertColumnToCharacter(int column){
        return (char) (column + 97);
    }
}
