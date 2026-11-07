package game.creation;

import model.Figure;
import model.figures.Archbishop;
import model.figures.Bishop;
import model.figures.Egg;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Queen;
import model.figures.Rook;
import model.figures.Ward;

/** Явная схема тела, без reflection и загрузки Java-классов из файла. */
public enum CreatedBody {
    PAWN(Pawn.class), KNIGHT(Knight.class), BISHOP(Bishop.class), ROOK(Rook.class), QUEEN(Queen.class),
    EGG(Egg.class), ARCHBISHOP(Archbishop.class), WARD(Ward.class);
    private final Class<? extends Figure> type;
    CreatedBody(Class<? extends Figure> type) { this.type = type; }
    public Class<? extends Figure> type() { return type; }
    public static CreatedBody of(Class<? extends Figure> type) {
        for (var body : values()) if (body.type == type) return body;
        throw new IllegalArgumentException("Unsupported created body");
    }
}
