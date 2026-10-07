package rules.standard;

import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.*;

import java.util.Map;

/** Conservative material test: false means mate is ruled out, true means it is not ruled out. */
public final class MaterialRules {
    private MaterialRules() {}

    public static boolean canPossiblyMate(Map<Position, Figure> board, FigureColor color) {
        int ownKnights = 0;
        int ownBishops = 0;
        boolean lightBishop = false;
        boolean darkBishop = false;
        boolean anyPawnOrKnight = false;
        boolean opponentHelpsKnightMate = false;
        for (var entry : board.entrySet()) {
            Figure piece = entry.getValue();
            if (piece.getColor() == FigureColor.NONE || piece instanceof King) continue;
            boolean own = piece.getColor() == color;
            if (own && (piece instanceof Pawn || piece instanceof Rook || piece instanceof Queen)) return true;
            if (piece instanceof Pawn || piece instanceof Knight) anyPawnOrKnight = true;
            if (!own && !(piece instanceof Queen)) opponentHelpsKnightMate = true;
            if (piece instanceof Knight && own) ownKnights++;
            if (piece instanceof Bishop) {
                if (own) ownBishops++;
                if ((entry.getKey().getRow() + entry.getKey().getColumn()) % 2 == 0) lightBishop = true;
                else darkBishop = true;
            }
        }
        if (ownKnights > 0) return ownKnights > 1 || ownBishops > 0 || opponentHelpsKnightMate;
        if (ownBishops > 0) return anyPawnOrKnight || (lightBishop && darkBishop);
        return false;
    }

    public static boolean isInsufficient(Map<Position, Figure> board) {
        return !canPossiblyMate(board, FigureColor.WHITE) && !canPossiblyMate(board, FigureColor.BLACK);
    }
}
