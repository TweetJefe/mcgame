package game;

import model.*;
import model.figures.*;
import rules.standard.MoveValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure previews. No live Game, Player, clock, or history is modified here. */
final class ActionEngine {
    record Context(Field field, Map<Long, PieceState> pieces, CastlingRights rights, Position ep) {
        Context { pieces = Map.copyOf(pieces); }
        PieceState at(Position square) {
            return pieces.values().stream().filter(piece -> piece.status() == PieceState.Status.BOARD
                    && piece.position().equals(square)).findFirst().orElse(null);
        }
    }
    record Capture(long actorId, long victimId, FigureColor creditedSide, Figure victim,
                   Position square, boolean reaction) {}
    record Preview(MoveResult result, Context next, List<Capture> captures) {
        Preview { captures = List.copyOf(captures); }
        static Preview rejected(MoveResult result) { return new Preview(result, null, List.of()); }
    }
    private ActionEngine() {}

    static Preview move(Context context, FigureColor side, Position from, Position to, Promotion promotion,
                        GameMode mode, PudgeRules rules) {
        MoveResult validation = MoveValidator.validate(context.field(), side, from, to, context.rights(), context.ep(), promotion);
        if (!validation.isSuccess()) return Preview.rejected(validation);
        Field field = context.field(); var oldBoard = field.getGameField();
        Figure figure = oldBoard.get(from);
        PieceState moving = context.at(from);
        boolean castle = MoveValidator.isCastling(figure, from, to);
        boolean ep = MoveValidator.isEnPassant(oldBoard, side, from, to, context.ep());
        Position captureSquare = ep ? new Position(from.getRow(), to.getColumn()) : to;
        Figure victim = oldBoard.get(captureSquare);
        PieceState victimState = context.at(captureSquare);
        var board = new HashMap<>(oldBoard);
        var states = new HashMap<>(context.pieces());
        var captures = new ArrayList<Capture>();
        CastlingRights rights = rightsAfter(context.rights(), figure, from,
                victimState == null ? null : victim, captureSquare);
        Class<? extends Figure> newType = promotion == null ? figure.getClass() : promotion.getFigureType();
        board.put(from, field.createFigure(Empty.class, from, FigureColor.NONE));
        board.put(to, field.createFigure(newType, to, side));
        states.put(moving.id(), moving.placed(to, newType));
        if (victimState != null) {
            states.put(victimState.id(), victimState.hidden(PieceState.Status.CAPTURED));
            captures.add(new Capture(moving.id(), victimState.id(), side, victim, captureSquare, false));
            addCharge(states, moving.id(), rules);
            if (ep) board.put(captureSquare, field.createFigure(Empty.class, captureSquare, FigureColor.NONE));
            // Every released ally occupies the capture square and takes the current captor.
            // If that captor was another loaded Pudge, its own passenger reacts next.
            Long passenger = mode == GameMode.MOBA ? victimState.storedPieceId() : null;
            long occupantId = moving.id();
            while (passenger != null) {
                PieceState occupant = states.get(occupantId);
                if (occupant.type() == King.class) return Preview.rejected(MoveResult.KING_IN_CHECK);
                PieceState released = states.get(passenger);
                if (released == null || released.status() != PieceState.Status.SWALLOWED)
                    return Preview.rejected(MoveResult.INVALID_BOARD);
                Long nextPassenger = occupant.storedPieceId();
                Figure eaten = field.createFigure(occupant.type(), to, occupant.color());
                captures.add(new Capture(released.id(), occupant.id(), released.color(), eaten, to, true));
                states.put(occupant.id(), occupant.hidden(PieceState.Status.CAPTURED));
                states.put(released.id(), released.placed(to, released.type()));
                addCharge(states, released.id(), rules);
                board.put(to, field.createFigure(released.type(), to, released.color()));
                rights = rightsAfter(rights, eaten, to, null, to);
                passenger = nextPassenger;
                occupantId = released.id();
            }
        }
        if (castle) {
            Position rookFrom = MoveValidator.rookSource(from, to), rookTo = MoveValidator.rookDestination(from, to);
            PieceState rook = context.at(rookFrom);
            board.put(rookFrom, field.createFigure(Empty.class, rookFrom, FigureColor.NONE));
            board.put(rookTo, field.createFigure(Rook.class, rookTo, side));
            states.put(rook.id(), rook.placed(rookTo, Rook.class));
        }
        if (MoveValidator.isInCheck(board, side)) return Preview.rejected(MoveResult.KING_IN_CHECK);
        Position nextEp = figure instanceof Pawn && Math.abs(to.getRow() - from.getRow()) == 2
                ? new Position((from.getRow() + to.getRow()) / 2, from.getColumn()) : null;
        Field next = mode == GameMode.MOBA ? Field.fromMobaPosition(board) : Field.fromPosition(board);
        return new Preview(MoveResult.SUCCESS, new Context(next, states, rights, validEp(board, nextEp)), captures);
    }

    static MoveResult abilityReady(Context context, FigureColor side, Position actorSquare, Ability ability, PudgeRules rules) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (!MoveValidator.inside(context.field(), actorSquare)) return MoveResult.OUT_OF_BOUNDS;
        PieceState actor = context.at(actorSquare);
        if (actor == null) return MoveResult.EMPTY_SOURCE;
        if (actor.color() != side) return MoveResult.WRONG_FIGURE_COLOR;
        if (actor.hero() != HeroType.PUDGE_ROOK) return MoveResult.NO_ABILITY;
        if (ability == Ability.HOOK && actor.cooldown() > 0) return MoveResult.ABILITY_COOLDOWN;
        if (ability == Ability.SWALLOW && actor.storedPieceId() != null) return MoveResult.ALREADY_SWALLOWED;
        if (ability == Ability.SWALLOW && actor.ultimateCharge() < rules.ultimateCaptures()) return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }

    static Preview ability(Context context, FigureColor side, Position actorSquare, Position targetSquare,
                           Ability ability, PudgeRules rules) {
        MoveResult ready = abilityReady(context, side, actorSquare, ability, rules);
        if (!ready.isSuccess()) return Preview.rejected(ready);
        if (!MoveValidator.inside(context.field(), targetSquare)) return Preview.rejected(MoveResult.OUT_OF_BOUNDS);
        if (actorSquare.equals(targetSquare)) return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
        PieceState actor = context.at(actorSquare), target = context.at(targetSquare);
        if (target == null) return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return Preview.rejected(MoveResult.KING_IMMUNE);
        int dr = targetSquare.getRow() - actorSquare.getRow(), dc = targetSquare.getColumn() - actorSquare.getColumn();
        int distance = Math.max(Math.abs(dr), Math.abs(dc));
        Field field = context.field(); var board = new HashMap<>(field.getGameField());
        var states = new HashMap<>(context.pieces()); CastlingRights rights = context.rights();
        if (ability == Ability.HOOK) {
            boolean straight = dr == 0 || dc == 0;
            boolean diagonal = Math.abs(dr) == Math.abs(dc);
            if (distance > 3 || (!straight && !(rules.hookDiagonals() && diagonal)))
                return Preview.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
            int rowStep = Integer.signum(dr), columnStep = Integer.signum(dc);
            for (int step = 1; step < distance; step++) {
                Position square = new Position(actorSquare.getRow() + rowStep * step, actorSquare.getColumn() + columnStep * step);
                if (!(board.get(square) instanceof Empty)) return Preview.rejected(MoveResult.ABILITY_BLOCKED);
            }
            Position landing = new Position(actorSquare.getRow() + rowStep, actorSquare.getColumn() + columnStep);
            board.put(targetSquare, field.createFigure(Empty.class, targetSquare, FigureColor.NONE));
            board.put(landing, field.createFigure(target.type(), landing, target.color()));
            states.put(target.id(), target.placed(landing, target.type()));
            states.put(actor.id(), actor.withAbilityState(rules.hookCooldown(), actor.ultimateCharge(), actor.storedPieceId()));
            if (!landing.equals(targetSquare)) rights = removedRookRights(rights, target, targetSquare);
        } else {
            if (distance != 1 || (!rules.swallowDiagonals() && Math.abs(dr) + Math.abs(dc) != 1))
                return Preview.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
            if (target.color() != side) return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
            board.put(targetSquare, field.createFigure(Empty.class, targetSquare, FigureColor.NONE));
            states.put(target.id(), target.hidden(PieceState.Status.SWALLOWED));
            states.put(actor.id(), actor.withAbilityState(actor.cooldown(), actor.ultimateCharge() - rules.ultimateCaptures(), target.id()));
            rights = removedRookRights(rights, target, targetSquare);
        }
        if (MoveValidator.isInCheck(board, side)) return Preview.rejected(MoveResult.KING_IN_CHECK);
        return new Preview(MoveResult.SUCCESS, new Context(Field.fromMobaPosition(board), states, rights, validEp(board, context.ep())), List.of());
    }

    static boolean hasMove(Context context, FigureColor side, GameMode mode, PudgeRules rules) {
        for (var entry : context.field().getGameField().entrySet()) {
            Figure piece = entry.getValue();
            if (piece.getColor() != side) continue;
            AvailableMoves candidates = MoveValidator.candidates(context.field(), piece, context.rights(), context.ep());
            for (var targets : List.of(candidates.getCanMove(), candidates.getCanAttack())) for (Position target : targets) {
                Promotion promotion = MoveValidator.isPromotion(piece, target, 8) ? Promotion.QUEEN : null;
                if (move(context, side, entry.getKey(), target, promotion, mode, rules).result().isSuccess()) return true;
            }
        }
        return false;
    }

    /** A skill can remove check or enable the mandatory normal move. Items have no catalog yet. */
    static boolean hasTurn(Context context, FigureColor side, PudgeRules rules) {
        if (hasMove(context, side, GameMode.MOBA, rules)) return true;
        for (PieceState piece : context.pieces().values()) {
            if (piece.status() != PieceState.Status.BOARD || piece.color() != side || piece.hero() != HeroType.PUDGE_ROOK) continue;
            for (Ability ability : Ability.values()) {
                if (!abilityReady(context, side, piece.position(), ability, rules).isSuccess()) continue;
                for (Position target : context.field().getGameField().keySet()) {
                    Preview preview = ability(context, side, piece.position(), target, ability, rules);
                    if (preview.result().isSuccess() && hasMove(preview.next(), side, GameMode.MOBA, rules)) return true;
                }
            }
        }
        return false;
    }

    private static void addCharge(Map<Long, PieceState> states, long actorId, PudgeRules rules) {
        PieceState actor = states.get(actorId);
        if (actor.hero() == HeroType.PUDGE_ROOK && rules.ultimateCaptures() > 0)
            states.put(actorId, actor.withAbilityState(actor.cooldown(), Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1), actor.storedPieceId()));
    }

    private static CastlingRights rightsAfter(CastlingRights rights, Figure moving, Position from, Figure captured, Position captureSquare) {
        if (moving instanceof King) rights = rights.withoutKing(moving.getColor());
        if (moving instanceof Rook) rights = withoutHomeRook(rights, moving.getColor(), from);
        if (captured instanceof Rook) rights = withoutHomeRook(rights, captured.getColor(), captureSquare);
        return rights;
    }
    private static CastlingRights removedRookRights(CastlingRights rights, PieceState piece, Position square) {
        return piece.type() == Rook.class ? withoutHomeRook(rights, piece.color(), square) : rights;
    }
    private static CastlingRights withoutHomeRook(CastlingRights rights, FigureColor color, Position square) {
        if (square.getRow() == MoveValidator.homeRow(color)) {
            if (square.getColumn() == 0) return rights.without(color, false);
            if (square.getColumn() == 7) return rights.without(color, true);
        }
        return rights;
    }
    private static Position validEp(Map<Position, Figure> board, Position ep) {
        if (ep == null) return null;
        FigureColor pawnColor = ep.getRow() == 2 ? FigureColor.WHITE : FigureColor.BLACK;
        int direction = pawnColor == FigureColor.WHITE ? 1 : -1;
        Position square = new Position(ep.getRow() + direction, ep.getColumn());
        Position origin = new Position(ep.getRow() - direction, ep.getColumn());
        Figure pawn = board.get(square);
        return board.get(ep) instanceof Empty && board.get(origin) instanceof Empty
                && pawn instanceof Pawn && pawn.getColor() == pawnColor ? ep : null;
    }
}
