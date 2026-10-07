package game;

import model.Figure;
import model.FigureColor;
import model.Position;
import java.util.Map;

/** Immutable position input/output; a new game starts with no history and no clock. */
public record GamePosition(Map<Position, Figure> board, FigureColor sideToMove,
                           CastlingRights castlingRights, Position enPassantTarget, int halfmoveClock) {
    public GamePosition {
        if (board == null) throw new IllegalArgumentException("Board is required");
        try { board = Map.copyOf(board); }
        catch (NullPointerException invalid) { throw new IllegalArgumentException("Board cannot contain null keys or pieces", invalid); }
    }
}
