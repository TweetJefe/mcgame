package game.heroes.faun;
import game.state.*;
import game.history.GameEvent;
import game.moves.*;
import model.*;
import model.figures.*;
import rules.standard.MoveValidator;
import rules.moba.AttackRules;
import java.util.*;

/** Local player's presentation. Full GameSnapshot remains the trusted engine/save state. */
public final class FaunView {
    private FaunView(){}
    public static boolean visible(GameSnapshot s,PieceState piece){return !s.faun().hidden(piece,s.active());}
    public static boolean hasHiddenEnemy(GameSnapshot s){return s.pieces().values().stream().anyMatch(p->p.position()!=null && !visible(s,p));}
    public static AvailableMoves moves(GameSnapshot s,Position from) {
        if(s.turn().moveUsed() || s.outcome().isFinished())return new AvailableMoves(List.of(),List.of());
        var p=s.pieceAt(from);if(p==null || p.color()!=s.active() || p.isStunned())return new AvailableMoves(List.of(),List.of());
        var board=new HashMap<>(s.field().getGameField());
        for(var hidden:s.pieces().values())if(hidden.position()!=null && !visible(s,hidden))board.put(hidden.position(),s.field().createFigure(Empty.class,hidden.position(),FigureColor.NONE));
        Field projected=Field.fromMobaPosition(board);var candidate=MoveValidator.candidates(projected,projected.getFigureByPosition(from),s.castlingRights(),s.enPassantTarget());
        java.util.function.Predicate<Position> valid=to->(!MoveValidator.isCastling(projected.getFigureByPosition(from),from,to) || !s.pieceAt(MoveValidator.rookSource(from,to)).isStunned()) && MoveValidator.validate(projected,s.active(),from,to,s.castlingRights(),s.enPassantTarget(),
            MoveValidator.isPromotion(projected.getFigureByPosition(from),to,8)?Promotion.QUEEN:null,AttackRules.blindSquares(s.pieces()),
            s.terrain().hostileSquares(s.pieces(),s.active()),s.terrain().hostileSquares(s.pieces(),MoveValidator.opposite(s.active())),true).isSuccess();
        return new AvailableMoves(candidate.getCanMove().stream().filter(valid).toList(),p.isBlinded()?List.of():candidate.getCanAttack().stream().filter(valid).toList());
    }
    public static String message(GameSnapshot s,GameEvent event) {
        for(var entry:s.faun().invisible().entrySet()) {
            PieceState p=s.pieces().get(entry.getKey());
            if(p!=null && p.color()!=s.active() && event.turn()>=entry.getValue().castTurn()
                && (Objects.equals(event.actorId(),p.id()) || Objects.equals(event.targetId(),p.id())))return "Действие скрытой фигуры";
        }
        return event.message();
    }
}
