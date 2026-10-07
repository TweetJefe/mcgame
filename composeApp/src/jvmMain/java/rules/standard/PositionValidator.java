package rules.standard;

import game.CastlingRights;
import model.*;
import model.figures.*;
import java.util.Map;

/** Structural and rule-state checks, not a proof of historical reachability. */
public final class PositionValidator {
    private PositionValidator() {}

    public static void validateBoard(Map<Position, Figure> board) { validateBoard(board, true); }

    public static void validateBoard(Map<Position, Figure> board, boolean classic) {
        require(board != null && board.size() == 64, "Board must contain exactly 64 squares");
        int[] kings = new int[2], pawns = new int[2], pieces = new int[2];
        Position[] kingSquares = new Position[2];
        int[][] counts = new int[2][5]; // queens, rooks, knights, bishops on each square color
        for (var entry : board.entrySet()) {
            Position square = entry.getKey(); Figure piece = entry.getValue();
            require(inside(square), "Invalid board square: " + square);
            require(piece != null && Field.supports(piece.getClass()), "Missing or unsupported piece at " + square);
            require(square.equals(piece.getPosition()), "Piece position differs from its square: " + square);
            FigureColor color = piece.getColor();
            require(color != null && (piece instanceof Empty) == (color == FigureColor.NONE), "Invalid piece color at " + square);
            if (piece instanceof Empty) continue;
            int side = color == FigureColor.WHITE ? 0 : 1;
            pieces[side]++;
            if (piece instanceof King) { kings[side]++; kingSquares[side] = square; }
            if (piece instanceof Pawn) {
                require(!classic || (square.getRow() != 0 && square.getRow() != 7), "Unpromoted pawn on last rank: " + square);
                pawns[side]++;
            }
            if (piece instanceof Queen) counts[side][0]++;
            if (piece instanceof Rook) counts[side][1]++;
            if (piece instanceof Knight) counts[side][2]++;
            if (piece instanceof Bishop) counts[side][3 + ((square.getRow() + square.getColumn()) % 2)]++;
        }
        for (int side = 0; side < 2; side++) {
            require(kings[side] == 1, "Each side must have exactly one king");
            require(pawns[side] <= 8 && pieces[side] <= 16, "Too many pieces or pawns for one side");
            int promotions = Math.max(0, counts[side][0] - 1) + Math.max(0, counts[side][1] - 2)
                    + Math.max(0, counts[side][2] - 2) + Math.max(0, counts[side][3] - 1) + Math.max(0, counts[side][4] - 1);
            require(!classic || promotions <= 8 - pawns[side], "Extra pieces require missing pawns for promotion");
        }
        require(Math.max(Math.abs(kingSquares[0].getRow() - kingSquares[1].getRow()),
                Math.abs(kingSquares[0].getColumn() - kingSquares[1].getColumn())) > 1, "Kings cannot be adjacent");
    }

    public static void validateState(Field field, FigureColor turn, CastlingRights rights, Position ep, int halfmoveClock) {
        require(field != null, "Field is required");
        require(turn == FigureColor.WHITE || turn == FigureColor.BLACK, "Side to move must be WHITE or BLACK");
        require(rights != null, "Castling rights are required");
        require(halfmoveClock >= 0, "Halfmove clock cannot be negative");
        var board = field.getGameField();
        for (FigureColor color : new FigureColor[]{FigureColor.WHITE, FigureColor.BLACK}) {
            int row = MoveValidator.homeRow(color);
            for (boolean kingSide : new boolean[]{true, false}) if (rights.allows(color, kingSide)) {
                require(isPiece(board, new Position(row, 4), King.class, color)
                        && isPiece(board, new Position(row, kingSide ? 7 : 0), Rook.class, color),
                        "Castling right requires a same-color home king and rook");
            }
        }
        require(!MoveValidator.isInCheck(board, MoveValidator.opposite(turn)), "The side that just moved cannot be in check");
        if (ep != null) {
            int targetRow = turn == FigureColor.WHITE ? 5 : 2;
            require(inside(ep) && ep.getRow() == targetRow, "Invalid en passant target rank");
            int direction = turn == FigureColor.WHITE ? -1 : 1;
            Position pawnSquare = new Position(targetRow + direction, ep.getColumn());
            Position origin = new Position(targetRow - direction, ep.getColumn());
            require(board.get(ep) instanceof Empty && board.get(origin) instanceof Empty
                    && isPiece(board, pawnSquare, Pawn.class, MoveValidator.opposite(turn)),
                    "En passant target requires the preceding double pawn move");
            require(halfmoveClock == 0, "A double pawn move resets the halfmove clock");
            // Reconstruct the preceding board to ensure the double move did not start after a check against the other king.
            var before = new java.util.HashMap<>(board);
            before.put(origin, before.remove(pawnSquare));
            require(!MoveValidator.isInCheck(before, turn), "En passant metadata implies an illegal preceding position");
        }
    }

    private static boolean isPiece(Map<Position, Figure> board, Position square, Class<? extends Figure> type, FigureColor color) {
        Figure piece = board.get(square);
        return piece != null && piece.getClass() == type && piece.getColor() == color;
    }
    private static boolean inside(Position square) {
        return square != null && square.getRow() >= 0 && square.getRow() < 8 && square.getColumn() >= 0 && square.getColumn() < 8;
    }
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
