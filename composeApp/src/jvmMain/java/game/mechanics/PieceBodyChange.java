package game.mechanics;

import java.util.Objects;
import model.Figure;

/** Подтверждённое событием изменение формы, независимо от имени механики. */
public record PieceBodyChange(long pieceId, Class<? extends Figure> before, Class<? extends Figure> after) {
    public PieceBodyChange {
        if (pieceId < 1) throw new IllegalArgumentException("Invalid body change identity");
        Objects.requireNonNull(before); Objects.requireNonNull(after);
    }
}
