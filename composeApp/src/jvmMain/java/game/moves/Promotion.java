package game.moves;


import model.Figure;
import model.figures.*;

public enum Promotion {
    QUEEN(Queen.class), ROOK(Rook.class), BISHOP(Bishop.class), KNIGHT(Knight.class);

    private final Class<? extends Figure> figureType;

    Promotion(Class<? extends Figure> figureType) { this.figureType = figureType; }
    public Class<? extends Figure> getFigureType() { return figureType; }
}
