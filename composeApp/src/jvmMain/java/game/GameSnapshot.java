package game;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import java.util.List;
import java.util.Map;

/** Coherent immutable engine snapshot; file/network serialization is a separate task. */
public record GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,
                           TurnState turn, FigureColor active, CastlingRights castlingRights,
                           Position enPassantTarget, int halfmoveClock, Map<String, Integer> repetitions,
                           ClockSnapshot clock, GameOutcome outcome, List<MadeMove> moves, List<GameEvent> events,
                           long whitePoints, long blackPoints, List<Figure> whiteCaptures, List<Figure> blackCaptures,
                           FigureColor drawOffer, PudgeRules pudgeRules) {
    public GameSnapshot {
        pieces = Map.copyOf(pieces); repetitions = Map.copyOf(repetitions);
        moves = List.copyOf(moves); events = List.copyOf(events);
        whiteCaptures = List.copyOf(whiteCaptures); blackCaptures = List.copyOf(blackCaptures);
    }
    public PieceState pieceAt(Position square) {
        return pieces.values().stream().filter(piece -> piece.status() == PieceState.Status.BOARD
                && piece.position().equals(square)).findFirst().orElse(null);
    }
}
