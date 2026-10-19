package rules.moba;

import game.state.PieceState;
import game.terrain.TerrainState;
import model.Figure;
import model.FigureColor;
import model.Position;
import rules.standard.MoveValidator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Blind pieces remain physical blockers but do not attack squares. */
public final class AttackRules {
    private AttackRules() {}
    public static Set<Position> blindSquares(Map<Long, PieceState> pieces) {
        return pieces.values().stream().filter(p -> p.status() == PieceState.Status.BOARD && (p.isBlinded() || p.isStunned()))
                .map(PieceState::position).collect(Collectors.toUnmodifiableSet());
    }
    public static Map<Long,PieceState> expireStuns(Map<Long,PieceState> pieces,long endedTurn) {
        var next=new java.util.HashMap<>(pieces);
        for(var p:pieces.values())if(p.isStunned() && p.stunnedThroughTurn()<=endedTurn)next.put(p.id(),p.withStun(0));
        return Map.copyOf(next);
    }
    public static boolean isInCheck(Map<Position, Figure> board, Map<Long, PieceState> pieces, FigureColor side) {
        return isInCheck(board, pieces, side, TerrainState.empty());
    }
    public static boolean isInCheck(Map<Position, Figure> board, Map<Long, PieceState> pieces, FigureColor side, TerrainState terrain) {
        return MoveValidator.isInCheck(board, side, blindSquares(pieces), terrain.hostileSquares(pieces, MoveValidator.opposite(side)));
    }
}
