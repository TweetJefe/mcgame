package rules.moba;

import game.heroes.HeroActionContext;

import game.mechanics.PieceRestriction;

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
        return pieces.values().stream().filter(p -> p.status() == PieceState.Status.BOARD && (p.isPotted() || p.isBlinded() || p.isStunned() || p.isBoxed() || p.hero()==game.heroes.HeroType.ILLUSION))
                .map(PieceState::position).collect(Collectors.toUnmodifiableSet());
    }
    public static Set<Position> blindSquares(HeroActionContext context) {
        var result = new java.util.HashSet<>(blindSquares(context.pieces()));
        for (var piece : context.pieces().values())
            if (piece.position() != null && context.blocks(piece, PieceRestriction.ATTACK))
                result.add(piece.position());
        return Set.copyOf(result);
    }
    public static boolean isInCheck(Map<Position, Figure> board, HeroActionContext context, FigureColor side) {
        if (context.pieces().values().stream().anyMatch(p -> p.position() != null && p.color() == side
                && p.type() == model.figures.King.class && p.isBoxed())) return false;
        return MoveValidator.isInCheck(board, side, blindSquares(context),
                context.terrain().hostileSquares(context.pieces(), MoveValidator.opposite(side)), context.passageObstacles());
    }
    public static Map<Long,PieceState> expireStuns(Map<Long,PieceState> pieces,long endedTurn) {
        var next=new java.util.HashMap<>(pieces);
        for(var p:pieces.values())if(p.isStunned() && p.stunnedThroughTurn()<=endedTurn)next.put(p.id(),p.withStun(0));
        for(var p:new java.util.ArrayList<>(next.values()))if(p.isBoxed() && p.boxedThroughTurn()<=endedTurn)next.put(p.id(),p.withBox(0));
        return Map.copyOf(next);
    }
    public static boolean isInCheck(Map<Position, Figure> board, Map<Long, PieceState> pieces, FigureColor side) {
        return isInCheck(board, pieces, side, TerrainState.empty());
    }
    public static boolean isInCheck(Map<Position, Figure> board, Map<Long, PieceState> pieces, FigureColor side, TerrainState terrain) {
        if(pieces.values().stream().anyMatch(p->p.position()!=null && p.color()==side && p.type()==model.figures.King.class && p.isBoxed()))return false;
        return MoveValidator.isInCheck(board, side, blindSquares(pieces), terrain.hostileSquares(pieces, MoveValidator.opposite(side)));
    }
}
