package game.heroes;

import game.state.PieceState;
import model.Position;

/** Исходная клетка нового состояния и источник призыва/превращения, если он есть. */
public record PieceCreationContext(Position square, PieceState source) {
    public static PieceCreationContext initial(Position square) {
        return new PieceCreationContext(square, null);
    }
}
