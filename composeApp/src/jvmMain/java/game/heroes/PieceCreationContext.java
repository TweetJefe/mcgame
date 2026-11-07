package game.heroes;

import game.state.PieceState;
import model.Position;

/** Исходная клетка нового состояния и источник призыва/превращения, если он есть. */
public record PieceCreationContext(Position square, PieceState source, boolean revival) {
    public PieceCreationContext {
        if (revival && (source == null || !source.isRemoved())) throw new IllegalArgumentException("Revival needs a corpse");
    }
    public PieceCreationContext(Position square, PieceState source) { this(square, source, false); }
    public static PieceCreationContext revival(Position square, PieceState corpse) {
        return new PieceCreationContext(square, corpse, true);
    }
    public static PieceCreationContext initial(Position square) {
        return new PieceCreationContext(square, null);
    }
}
