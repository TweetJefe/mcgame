package game.core;

import game.heroes.HeroRules;
import game.history.GameEvent;
import game.mechanics.MechanicPhase;
import game.mechanics.MechanicSession;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.GameMode;
import game.state.PieceState;
import java.util.HashMap;
import java.util.List;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.Empty;
import rules.standard.MoveValidator;

/** Атомарная подготовка завершения хода и автоматических пропусков без публикации в Game. */
final class TurnFlowEngine {
    record Transition(MoveResult result, ActionEngine.Context context, FigureColor side, List<GameEvent> history) {}
    private TurnFlowEngine() {}

    static Transition end(ActionEngine.Context before, FigureColor side, HeroRules rules, List<GameEvent> history) {
        var finished = finish(before, side, rules, history, false);
        if (!finished.result().isSuccess()) return finished;
        for (int count = 0; count < 32; count++) {
            var context = finished.context();
            var beginning = new MechanicSession(context.heroContext(), context.pieces(), rules, context.heroes(),
                    finished.side(), null, List.of(), finished.history());
            MoveResult result = beginning.run(MechanicPhase.BEGIN_TURN);
            if (!result.isSuccess()) return rejected(result, before, side, history);
            beginning.requireComplete();
            appendProposed(beginning);
            context = context(context, beginning, context.turn(), context.ep());
            var ready = new Transition(Success.SUCCESS, context, finished.side(), beginning.history());
            if (!context.heroes().mechanics().skipsTurn(context.heroContext(), ready.side())
                    || !ActionEngine.hasTurn(context, ready.side(), rules)) return ready;
            finished = finish(context, ready.side(), rules, ready.history(), true);
            if (!finished.result().isSuccess()) return finished;
        }
        return rejected(MovementRejection.INVALID_BOARD, before, side, history);
    }

    private static Transition finish(ActionEngine.Context before, FigureColor side, HeroRules rules,
                                      List<GameEvent> history, boolean skipped) {
        var session = new MechanicSession(before.heroContext(), before.pieces(), rules, before.heroes(), side, null, List.of(), history);
        if (skipped) {
            MoveResult skip = session.run(MechanicPhase.SKIP_TURN);
            if (!skip.isSuccess()) return rejected(skip, before, side, history);
        }
        MoveResult end = session.run(MechanicPhase.END_TURN);
        if (!end.isSuccess()) return rejected(end, before, side, history);
        MoveResult settled = session.run(MechanicPhase.END_TURN_SETTLED);
        if (!settled.isSuccess()) return rejected(settled, before, side, history);
        session.requireComplete();
        MoveResult journal = session.run(MechanicPhase.END_HISTORY);
        if (!journal.isSuccess()) return rejected(journal, before, side, history);
        appendProposed(session);
        session.emit(new GameEvent(session.history().size() + 1L, before.turn(), GameEvent.Kind.END_TURN,
                side, null, null, null, null, "Ход завершён"));
        FigureColor incoming = MoveValidator.opposite(side);
        var nextPieces = new HashMap<>(session.pieces());
        for (var piece : session.pieces().values())
            if (piece.color() == incoming && piece.cooldown() > 0 && !piece.isRemoved())
                nextPieces.put(piece.id(), piece.withAbilityState(piece.cooldown() - 1,
                        piece.ultimateCharge(), piece.storedPieceId()));
        before.heroes().validateStates(nextPieces);
        session.pieces(nextPieces);
        var next = context(before, session, Math.addExact(before.turn(), 1), skipped ? null : before.ep());
        return new Transition(Success.SUCCESS, next, incoming, session.history());
    }

    private static void appendProposed(MechanicSession session) {
        for (var event : session.proposedEvents()) session.emit(event.numbered(session.history().size() + 1L));
    }

    private static ActionEngine.Context context(ActionEngine.Context before, MechanicSession session, long turn, Position ep) {
        var board = new HashMap<Position, Figure>();
        for (Position square : before.field().getGameField().keySet())
            board.put(square, before.field().createFigure(Empty.class, square, FigureColor.NONE));
        for (PieceState piece : session.pieces().values()) if (piece.position() != null)
            board.put(piece.position(), before.field().createFigure(piece.type(), piece.position(), piece.color()));
        return new ActionEngine.Context(Field.fromMobaPosition(board), session.pieces(), before.rights(), ep,
                turn, session.states(), before.heroes());
    }
    private static Transition rejected(MoveResult result, ActionEngine.Context context, FigureColor side, List<GameEvent> history) {
        return new Transition(result, context, side, history);
    }
}
