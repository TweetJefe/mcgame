package game.core;

import game.history.MadeMove;

import model.Figure;
import model.FigureColor;

import java.util.ArrayList;
import java.util.List;

public final class Player {
    private final List<Figure> attackedFigures = new ArrayList<>();
    private final List<MadeMove> madeMoves = new ArrayList<>();
    private final boolean isWhite;
    private long points;

    public Player(boolean isWhite) {
        this.isWhite = isWhite;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public FigureColor color() {
        return isWhite ? FigureColor.WHITE : FigureColor.BLACK;
    }

    public synchronized long getPoints() {
        return points;
    }

    public synchronized List<MadeMove> getMadeMoves() {
        return List.copyOf(madeMoves);
    }

    public synchronized List<Figure> getAttackedFigures() {
        return List.copyOf(attackedFigures);
    }

    synchronized void recordCapture(Figure figure) {
        attackedFigures.add(figure);
        points += figure.getPoints();
    }

    synchronized void recordMove(MadeMove move) {
        madeMoves.add(move);
    }
}
