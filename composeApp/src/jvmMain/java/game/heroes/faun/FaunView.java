package game.heroes.faun;

import game.heroes.moleminer.MoleminerAbility;

import game.state.*;
import game.history.GameEvent;
import game.moves.*;
import model.*;
import model.figures.*;
import rules.standard.MoveValidator;
import rules.moba.AttackRules;

import java.util.*;

/**
 * Local player's presentation. Full GameSnapshot remains the trusted engine/save state.
 */
public final class FaunView {
    private FaunView() {
    }

    public static boolean visible(GameSnapshot s, PieceState piece) {
        return !s.faun().hidden(piece, s.active());
    }

    public static boolean hasHiddenEnemy(GameSnapshot s) {if(s.mines().cells().values().stream().anyMatch(m->s.pieces().get(m.ownerId()).color()!=s.active()))return true;return s.pieces().values().stream().anyMatch(p -> p.position() != null && (!visible(s, p) || p.hero()==game.heroes.HeroType.ILLUSION && p.color()!=s.active()));
    }

    public static AvailableMoves moves(GameSnapshot s, Position from) {
        if (s.outcome().isFinished()) return new AvailableMoves(List.of(), List.of());
        var p = s.pieceAt(from);
        if (p == null || p.color() != s.active() || p.isStunned() || p.isBoxed() || p.isPotted() || !game.heroes.clownganger.ClownState.canMove(s,p.id())) return new AvailableMoves(List.of(), List.of());
        var board = new HashMap<>(s.field().getGameField());
        for (var hidden : s.pieces().values())
            if (hidden.position() != null && !visible(s, hidden))
                board.put(hidden.position(), s.field().createFigure(Empty.class, hidden.position(), FigureColor.NONE));
        var visiblePieces=new HashMap<Long,PieceState>();for(var actual:s.pieces().values())visiblePieces.put(actual.id(),game.heroes.clownganger.ClownView.displayed(s,actual));
        Field projected = Field.fromMobaPosition(board);
        var candidate = MoveValidator.candidates(projected, projected.getFigureByPosition(from), s.castlingRights(), s.enPassantTarget());
        java.util.function.Predicate<Position> valid = to -> (s.pieceAt(to)==null || (!s.pieceAt(to).isBoxed() && !s.pieceAt(to).isPotted())) && (!MoveValidator.isCastling(projected.getFigureByPosition(from), from, to) || (!s.pieceAt(MoveValidator.rookSource(from, to)).isStunned() && !s.pieceAt(MoveValidator.rookSource(from,to)).isBoxed() && !s.pieceAt(MoveValidator.rookSource(from,to)).isPotted())) && MoveValidator.validate(projected, s.active(), from, to, s.castlingRights(), s.enPassantTarget(),
                MoveValidator.isPromotion(projected.getFigureByPosition(from), to, 8) ? Promotion.QUEEN : null, AttackRules.blindSquares(visiblePieces),
                s.terrain().hostileSquares(s.pieces(), s.active()), s.terrain().hostileSquares(s.pieces(), MoveValidator.opposite(s.active())),s.pieces().values().stream().noneMatch(k->k.type()==King.class && k.color()==s.active() && k.isBoxed())).isSuccess();
        return new AvailableMoves(candidate.getCanMove().stream().filter(valid).toList(),(p.isBlinded() || p.hero()==game.heroes.HeroType.ILLUSION) ? List.of() : candidate.getCanAttack().stream().filter(valid).toList());
    }

    public static String message(GameSnapshot s, GameEvent event) {
        if(event.isAbility(MoleminerAbility.PLANT_MINE) && event.color()!=s.active())return "Противник заложил мину";
        if(game.heroes.clownganger.ClownView.concealed(s,event))return "Действие двойника";
        for (var entry : s.faun().invisible().entrySet()) {
            PieceState p = s.pieces().get(entry.getKey());
            if (p != null && p.color() != s.active() && event.turn() >= entry.getValue().castTurn()
                    && (Objects.equals(event.actorId(), p.id()) || Objects.equals(event.targetId(), p.id())))
                return "Действие скрытой фигуры";
        }
        return event.message();
    }
}
