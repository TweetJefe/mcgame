package rules.standard;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.AvailableMoves;
import rules.moba.TerrainGeometry;
import game.moves.MoveResult;
import game.moves.Promotion;
import game.state.CastlingRights;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** All previews use a virtual map and detached figures, never live positions or move caches. */
public final class MoveValidator {
    private MoveValidator() {}

    public static MoveResult validate(Field field, FigureColor color, Position from, Position to,
                                      CastlingRights rights, Position enPassant, Promotion promotion) {
        return validate(field, color, from, to, rights, enPassant, promotion, Set.of());
    }
    public static MoveResult validate(Field field, FigureColor color, Position from, Position to,
                                      CastlingRights rights, Position enPassant, Promotion promotion, Set<Position> blinded) {
        return validate(field, color, from, to, rights, enPassant, promotion, blinded, Set.of(), true);
    }
    /** Moba resolves captures/reactions before its final king-safety test. */
    public static MoveResult validate(Field field, FigureColor color, Position from, Position to,
                                      CastlingRights rights, Position enPassant, Promotion promotion,
                                      Set<Position> blinded, Set<Position> mud, boolean finalKingSafety) {
        return validate(field, color, from, to, rights, enPassant, promotion, blinded, mud, mud, finalKingSafety);
    }
    public static MoveResult validate(Field field, FigureColor color, Position from, Position to,
                                      CastlingRights rights, Position enPassant, Promotion promotion,
                                      Set<Position> blinded, Set<Position> mud, Set<Position> attackMud, boolean finalKingSafety) {
        if (!inside(field, from) || !inside(field, to)) return MovementRejection.OUT_OF_BOUNDS;
        if (from.equals(to)) return MovementRejection.SAME_SQUARE;
        Figure figure = field.getFigureByPosition(from);
        Figure target = field.getFigureByPosition(to);
        if (figure == null || target == null) return MovementRejection.INVALID_BOARD;
        if (figure.getColor() == FigureColor.NONE) return MovementRejection.EMPTY_SOURCE;
        if (figure.getColor() != color) return MovementRejection.WRONG_FIGURE_COLOR;
        if (target.getColor() == color) return MovementRejection.OWN_PIECE_AT_TARGET;
        if (target instanceof King) return MovementRejection.KING_CAPTURE_NOT_ALLOWED;

        AvailableMoves candidates = candidates(field, figure, rights, enPassant);
        if (!candidates.getCanMove().contains(to) && !candidates.getCanAttack().contains(to)) {
            return MovementRejection.ILLEGAL_MOVEMENT;
        }
        if (TerrainGeometry.blocks(from, to, figure.getClass(), mud)) return AbilityRejection.MUD_BLOCKED;
        if (figure instanceof King && mud.contains(to)) return StatusRejection.KING_IMMUNE;
        if (isCastling(figure, from, to) && TerrainGeometry.blocksRay(rookSource(from, to), rookDestination(from, to), mud)) return AbilityRejection.MUD_BLOCKED;
        boolean promotes = isPromotion(figure, to, field.getSize());
        if (promotes && promotion == null) return MovementRejection.PROMOTION_REQUIRED;
        if (!promotes && promotion != null) return MovementRejection.INVALID_PROMOTION;

        if (isCastling(figure, from, to)) {
            Map<Position, Figure> board = field.getGameField();
            MoveResult initialSafety = kingSafety(board, color, blinded, attackMud);
            if (!initialSafety.isSuccess()) return initialSafety;
            Position transit = new Position(from.getRow(), from.getColumn() + Integer.signum(to.getColumn() - from.getColumn()));
            Map<Position, Figure> through = new HashMap<>(board);
            through.remove(from);
            through.put(transit, figure);
            MoveResult transitSafety = kingSafety(through, color, blinded, attackMud);
            if (!transitSafety.isSuccess()) return transitSafety;
        }
        return finalKingSafety ? kingSafety(afterMove(field, from, to, enPassant, promotion), color, blinded, attackMud) : Success.SUCCESS;
    }

    public static AvailableMoves candidates(Field field, Figure figure, CastlingRights rights, Position enPassant) {
        if (figure == null || figure.getColor() == FigureColor.NONE) return new AvailableMoves(List.of(), List.of());
        Position source = figure.getPosition();
        Figure probe = field.createFigure(figure.getClass(), copy(source), figure.getColor());
        field.compute(probe);
        List<Position> moves = new ArrayList<>(probe.getCanMove());
        List<Position> attacks = new ArrayList<>(probe.getCanAttack());
        if (figure instanceof King && field.getSize() == 8) {
            for (boolean kingSide : new boolean[]{true, false}) {
                Position destination = new Position(homeRow(figure.getColor()), kingSide ? 6 : 2);
                if (canCastleStructure(field.getGameField(), figure, source, destination, rights)) moves.add(destination);
            }
        }
        if (figure instanceof Pawn && enPassant != null
                && isEnPassant(field.getGameField(), figure.getColor(), source, enPassant, enPassant)) {
            attacks.add(copy(enPassant));
        }
        return new AvailableMoves(moves, attacks);
    }

    public static boolean inside(Field field, Position position) {
        return position != null && position.getRow() >= 0 && position.getRow() < field.getSize()
                && position.getColumn() >= 0 && position.getColumn() < field.getSize();
    }

    public static boolean isPromotion(Figure figure, Position to, int size) {
        return figure instanceof Pawn && to.getRow() == (figure.getColor() == FigureColor.WHITE ? size - 1 : 0);
    }

    public static boolean isCastling(Figure figure, Position from, Position to) {
        return figure instanceof King && from.getColumn() == 4 && from.getRow() == homeRow(figure.getColor())
                && to.getRow() == from.getRow() && (to.getColumn() == 2 || to.getColumn() == 6);
    }

    private static boolean canCastleStructure(Map<Position, Figure> board, Figure king, Position from,
                                               Position to, CastlingRights rights) {
        if (!isCastling(king, from, to)) return false;
        boolean kingSide = to.getColumn() == 6;
        if (!rights.allows(king.getColor(), kingSide)) return false;
        Figure rook = board.get(rookSource(from, to));
        if (!(rook instanceof Rook) || rook.getColor() != king.getColor()) return false;
        int start = kingSide ? 5 : 1;
        int end = kingSide ? 6 : 3;
        for (int column = start; column <= end; column++) {
            Figure occupant = board.get(new Position(from.getRow(), column));
            if (occupant == null || occupant.getColor() != FigureColor.NONE) return false;
        }
        return true;
    }

    public static Position rookSource(Position from, Position to) { return new Position(from.getRow(), to.getColumn() == 6 ? 7 : 0); }
    public static Position rookDestination(Position from, Position to) { return new Position(from.getRow(), to.getColumn() == 6 ? 5 : 3); }
    public static int homeRow(FigureColor color) { return color == FigureColor.WHITE ? 0 : 7; }

    public static boolean isEnPassant(Map<Position, Figure> board, FigureColor color,
                                      Position from, Position to, Position target) {
        if (target == null || to == null || !to.equals(target)) return false;
        Figure pawn = board.get(from);
        if (!(pawn instanceof Pawn) || pawn.getColor() != color) return false;
        int direction = color == FigureColor.WHITE ? 1 : -1;
        if (from.getRow() != (color == FigureColor.WHITE ? 4 : 3)
                || to.getRow() - from.getRow() != direction || Math.abs(to.getColumn() - from.getColumn()) != 1) return false;
        Figure destination = board.get(to);
        if (destination != null && destination.getColor() != FigureColor.NONE) return false;
        Figure victim = board.get(new Position(from.getRow(), to.getColumn()));
        return victim instanceof Pawn && victim.getColor() == opposite(color);
    }

    public static Map<Position, Figure> afterMove(Field field, Position from, Position to,
                                                 Position enPassant, Promotion promotion) {
        Map<Position, Figure> board = field.getGameField();
        Figure figure = board.get(from);
        Map<Position, Figure> result = new HashMap<>(board);
        result.remove(from);
        if (isEnPassant(board, figure.getColor(), from, to, enPassant)) result.remove(new Position(from.getRow(), to.getColumn()));
        Figure destination = promotion == null ? figure : field.createFigure(promotion.getFigureType(), copy(to), figure.getColor());
        result.put(to, destination);
        if (isCastling(figure, from, to)) {
            Position rookFrom = rookSource(from, to);
            result.put(rookDestination(from, to), result.remove(rookFrom));
        }
        return result;
    }

    /** Only an actually legal en passant capture distinguishes repeated positions. */
    public static boolean hasLegalEnPassant(Map<Position, Figure> board, FigureColor color, Position target) {
        if (target == null) return false;
        int sourceRow = target.getRow() + (color == FigureColor.WHITE ? -1 : 1);
        for (int column : new int[]{target.getColumn() - 1, target.getColumn() + 1}) {
            Position from = new Position(sourceRow, column);
            if (isEnPassant(board, color, from, target, target)) {
                Map<Position, Figure> result = new HashMap<>(board);
                result.put(target, result.remove(from));
                result.remove(new Position(from.getRow(), target.getColumn()));
                if (kingSafety(result, color).isSuccess()) return true;
            }
        }
        return false;
    }

    public static boolean isInCheck(Map<Position, Figure> board, FigureColor color) { return kingSafety(board, color) == MovementRejection.KING_IN_CHECK; }

    public static boolean isInCheck(Map<Position, Figure> board, FigureColor color, Set<Position> blinded) {
        return kingSafety(board, color, blinded) == MovementRejection.KING_IN_CHECK;
    }
    public static boolean isInCheck(Map<Position, Figure> board, FigureColor color, Set<Position> blinded, Set<Position> mud) {
        return kingSafety(board, color, blinded, mud) == MovementRejection.KING_IN_CHECK;
    }
    private static MoveResult kingSafety(Map<Position, Figure> board, FigureColor color) {
        return kingSafety(board, color, Set.of());
    }
    private static MoveResult kingSafety(Map<Position, Figure> board, FigureColor color, Set<Position> blinded) {
        return kingSafety(board, color, blinded, Set.of());
    }
    private static MoveResult kingSafety(Map<Position, Figure> board, FigureColor color, Set<Position> blinded, Set<Position> mud) {
        Position king = null;
        for (var entry : board.entrySet()) {
            if (entry.getValue() instanceof King && entry.getValue().getColor() == color) {
                if (king != null) return MovementRejection.INVALID_BOARD;
                king = entry.getKey();
            }
        }
        if (king == null) return MovementRejection.INVALID_BOARD;
        return isSquareAttacked(board, king, opposite(color), blinded, mud) ? MovementRejection.KING_IN_CHECK : Success.SUCCESS;
    }

    /** Empty pawn diagonals and attacks of pinned pieces are included. */
    private static boolean isSquareAttacked(Map<Position, Figure> board, Position square, FigureColor attacker, Set<Position> blinded, Set<Position> mud) {
        for (var entry : board.entrySet()) {
            Figure figure = entry.getValue();
            if (figure.getColor() != attacker || blinded.contains(entry.getKey())) continue;
            Position source = entry.getKey();
            int rowDelta = square.getRow() - source.getRow();
            int columnDelta = square.getColumn() - source.getColumn();
            int rows = Math.abs(rowDelta);
            int columns = Math.abs(columnDelta);
            if (rows == 0 && columns == 0) continue;
            if (TerrainGeometry.blocks(source, square, figure.getClass(), mud)) continue;
            if (figure instanceof Pawn) {
                if (rowDelta == (attacker == FigureColor.WHITE ? 1 : -1) && columns == 1) return true;
            } else if (figure instanceof Knight) {
                if ((rows == 2 && columns == 1) || (rows == 1 && columns == 2)) return true;
            } else if (figure instanceof King) {
                if (Math.max(rows, columns) == 1) return true;
            } else {
                boolean diagonal = rows == columns;
                boolean straight = rowDelta == 0 || columnDelta == 0;
                boolean ray = (figure instanceof Bishop && diagonal) || (figure instanceof Rook && straight)
                        || (figure instanceof Queen && (diagonal || straight));
                if (ray && clearRay(board, source, square)) return true;
            }
        }
        return false;
    }

    private static boolean clearRay(Map<Position, Figure> board, Position source, Position target) {
        int rowStep = Integer.signum(target.getRow() - source.getRow());
        int columnStep = Integer.signum(target.getColumn() - source.getColumn());
        Position cursor = new Position(source.getRow() + rowStep, source.getColumn() + columnStep);
        while (!cursor.equals(target)) {
            Figure blocker = board.get(cursor);
            if (blocker != null && blocker.getColor() != FigureColor.NONE) return false;
            cursor = new Position(cursor.getRow() + rowStep, cursor.getColumn() + columnStep);
        }
        return true;
    }

    public static FigureColor opposite(FigureColor color) { return color == FigureColor.WHITE ? FigureColor.BLACK : FigureColor.WHITE; }
    private static Position copy(Position position) { return new Position(position.getRow(), position.getColumn()); }
}
