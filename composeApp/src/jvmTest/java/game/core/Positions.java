package game.core;

import game.history.MadeMove;
import game.moves.MoveResult;
import game.moves.Promotion;
import game.state.CastlingRights;
import game.state.ClockSnapshot;
import game.state.GameOutcome;

import model.Figure;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.Empty;
import model.figures.King;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import static model.FigureColor.*;
import static org.junit.Assert.*;

public final class Positions {
    public static Position pos(String name) { return new Position(name.charAt(1) - '1', name.charAt(0) - 'a'); }
    static Game game(String whiteKing, String blackKing) { return game(whiteKing, blackKing, () -> 0L); }
    static Game game(String whiteKing, String blackKing, LongSupplier time) {
        Game game = new Game(8, time);
        Map<Position, Figure> board = new HashMap<>();
        for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++) {
            Position position = new Position(row, column);
            board.put(position, new Empty(position, NONE, game.getGameField()));
        }
        board.put(pos(whiteKing), game.getGameField().createFigure(King.class, pos(whiteKing), WHITE));
        board.put(pos(blackKing), game.getGameField().createFigure(King.class, pos(blackKing), BLACK));
        set(game, "gameField", Field.fromPosition(board));
        return game;
    }
    static void put(Game game, Class<? extends Figure> type, FigureColor color, String name) {
        Position position = pos(name);
        // Test-only fixture editing. Production callers must use Game.fromPosition.
        var board = new HashMap<>(game.getGameField().getGameField());
        board.put(position, game.getGameField().createFigure(type, position, color));
        set(game, "gameField", Field.fromPosition(board));
    }
    static MoveResult move(Game game, String from, String to) { return game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to)); }
    static void play(Game game, String... moves) {
        for (String move : moves) assertEquals(move, MoveResult.SUCCESS, move(game, move.substring(0, 2), move.substring(2, 4)));
    }
    static void set(Game game, String field, Object value) {
        try {
            var declared = Game.class.getDeclaredField(field);
            declared.setAccessible(true);
            declared.set(game, value);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    static void rejected(Game game, MoveResult expected, String from, String to, Promotion promotion) {
        State before = new State(game);
        assertEquals(expected, game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to), promotion));
        before.assertUnchanged(game);
    }
    static void rejected(Game game, MoveResult expected, String from, String to) { rejected(game, expected, from, to, null); }

    static final class State {
        final Map<Position, Figure> board;
        final Map<Figure, Position> positions = new IdentityHashMap<>();
        final Map<Figure, List<Position>> moveCaches = new IdentityHashMap<>(), attackCaches = new IdentityHashMap<>();
        final Player turn;
        final GameOutcome outcome;
        final CastlingRights rights;
        final Position ep;
        final int counter, repetitions;
        final String key;
        final List<MadeMove> history;
        final List<List<MadeMove>> playerHistories;
        final List<List<Figure>> captures;
        final long whitePoints, blackPoints;
        final ClockSnapshot clock;
        final Player drawOffer;

        State(Game game) {
            board = new HashMap<>(game.getGameField().getGameField());
            board.values().forEach(piece -> {
                positions.put(piece, new Position(piece.getPosition().getRow(), piece.getPosition().getColumn()));
                moveCaches.put(piece, piece.getCanMove()); attackCaches.put(piece, piece.getCanAttack());
            });
            turn = game.getCurrentPlayer(); outcome = game.getOutcome(); rights = game.getCastlingRights();
            ep = game.getEnPassantTarget(); counter = game.getHalfmoveClock(); repetitions = game.getRepetitionCount();
            key = game.getPositionKey(); history = game.getMoves();
            playerHistories = List.of(List.copyOf(game.getWhitePlayer().getMadeMoves()), List.copyOf(game.getBlackPlayer().getMadeMoves()));
            captures = List.of(List.copyOf(game.getWhitePlayer().getAttackedFigures()), List.copyOf(game.getBlackPlayer().getAttackedFigures()));
            whitePoints = game.getWhitePlayer().getPoints(); blackPoints = game.getBlackPlayer().getPoints();
            clock = game.getClockSnapshot(); drawOffer = game.getDrawOffer();
        }
        void assertUnchanged(Game game) {
            assertEquals(board, game.getGameField().getGameField());
            board.values().forEach(piece -> {
                assertEquals(positions.get(piece), piece.getPosition());
                assertEquals(moveCaches.get(piece), piece.getCanMove()); assertEquals(attackCaches.get(piece), piece.getCanAttack());
            });
            assertSame(turn, game.getCurrentPlayer()); assertEquals(outcome, game.getOutcome());
            assertEquals(rights, game.getCastlingRights()); assertEquals(ep, game.getEnPassantTarget());
            assertEquals(counter, game.getHalfmoveClock()); assertEquals(repetitions, game.getRepetitionCount());
            assertEquals(key, game.getPositionKey()); assertEquals(history, game.getMoves());
            assertEquals(playerHistories.get(0), game.getWhitePlayer().getMadeMoves());
            assertEquals(playerHistories.get(1), game.getBlackPlayer().getMadeMoves());
            assertEquals(captures.get(0), game.getWhitePlayer().getAttackedFigures());
            assertEquals(captures.get(1), game.getBlackPlayer().getAttackedFigures());
            assertEquals(whitePoints, game.getWhitePlayer().getPoints()); assertEquals(blackPoints, game.getBlackPlayer().getPoints());
            assertEquals(clock, game.getClockSnapshot()); assertSame(drawOffer, game.getDrawOffer());
        }
    }
}
