package game.core;

import game.economy.EconomyState;
import game.heroes.Abilities;
import game.heroes.AbilityRequest;
import game.heroes.Hero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroRegistry;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.clownganger.ClownState;
import game.heroes.faun.FaunState;
import game.heroes.AbilityUse;
import game.heroes.ginseng.SpellState;
import game.heroes.moleminer.MineState;
import game.heroes.zeus.LightningState;
import game.mechanics.BuiltInStates;
import game.mechanics.MechanicEventDraft;
import game.mechanics.MechanicPhase;
import game.mechanics.MechanicSession;
import game.mechanics.MechanicStates;
import game.mechanics.MoveIntent;
import game.moves.AbilityRejection;
import game.moves.AvailableMoves;
import game.moves.Capture;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Promotion;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.CastlingRights;
import game.state.GameMode;
import game.state.PieceState;
import game.summoning.SummonState;
import game.terrain.TerrainState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.Empty;
import model.figures.King;
import model.figures.Pawn;
import model.figures.Rook;
import rules.moba.AttackRules;
import rules.standard.MoveValidator;

/**
 * Pure previews. No live Game, Player, clock, or history is modified here.
 */
final class ActionEngine {
    record Context(Field field, Map<Long, PieceState> pieces, CastlingRights rights, Position ep,
                   long turn, MechanicStates mechanics, HeroRegistry heroes) {
        Context {
            pieces = Map.copyOf(pieces);
            Objects.requireNonNull(heroes, "heroes");
            Objects.requireNonNull(mechanics, "mechanics");
        }
        Context(Field field, Map<Long, PieceState> pieces, CastlingRights rights, Position ep, EconomyState economy,
                long turn, TerrainState terrain, SummonState summons, SpellState spells, FaunState faun,
                ClownState clown, MineState mines, LightningState lightning, HeroRegistry heroes) {
            this(field, pieces, rights, ep, turn, BuiltInStates.states(economy, terrain, summons, spells,
                    faun, clown, mines, lightning), heroes);
        }
        HeroActionContext heroContext() { return new HeroActionContext(field, pieces, turn, mechanics); }
        EconomyState economy() { return heroContext().economy(); }
        TerrainState terrain() { return heroContext().terrain(); }
        SummonState summons() { return heroContext().summons(); }
        SpellState spells() { return heroContext().spells(); }
        FaunState faun() { return heroContext().faun(); }
        ClownState clown() { return heroContext().clown(); }
        MineState mines() { return heroContext().mines(); }
        LightningState lightning() { return heroContext().lightning(); }
        PieceState at(Position square) { return heroContext().pieceAt(square); }
    }

    record Preview(MoveResult result, Context next, List<Capture> captures,
                   List<MechanicEventDraft> events) {
        Preview(MoveResult result, Context next, List<Capture> captures) { this(result, next, captures, List.of()); }
        Preview {
            captures = List.copyOf(captures);
            events = List.copyOf(events);
        }

        static Preview rejected(MoveResult result) {
            return new Preview(result, null, List.of());
        }
    }

    private ActionEngine() {
    }

    static Preview move(Context context, FigureColor side, Position from, Position to, Promotion promotion,
                        GameMode mode, HeroRules rules) {
        PieceState source = context.at(from);
        if (source != null && source.color() == side && source.isPotted())
            return Preview.rejected(StatusRejection.POTTED);
        var reactions = new MechanicSession(context.heroContext(), context.pieces(), rules, context.heroes(), side, null, List.of());
        reactions.movement(new MoveIntent(source, from, to, false));
        if (mode == GameMode.MOBA) {
            MoveResult started = reactions.run(MechanicPhase.MOVE_START);
            if (!started.isSuccess()) return Preview.rejected(started);
        }
        if (source != null && source.color() == side && source.isBoxed())
            return Preview.rejected(StatusRejection.BOXED);
        if (source != null && source.color() == side && source.isStunned())
            return Preview.rejected(StatusRejection.STUNNED);
        MoveResult validation = MoveValidator.validate(context.field(), side, from, to, context.rights(), context.ep(), promotion, mode == GameMode.MOBA ? AttackRules.blindSquares(context.pieces()) : Set.of(),
                context.terrain().hostileSquares(context.pieces(), side), context.terrain().hostileSquares(context.pieces(), MoveValidator.opposite(side)), mode != GameMode.MOBA);
        if (!validation.isSuccess()) return Preview.rejected(validation);
        Field field = context.field();
        var oldBoard = field.getGameField();
        Figure figure = oldBoard.get(from);
        PieceState moving = context.at(from);
        boolean castle = MoveValidator.isCastling(figure, from, to);
        if (castle && context.at(MoveValidator.rookSource(from, to)).isStunned())
            return Preview.rejected(StatusRejection.STUNNED);
        reactions.movement(new MoveIntent(moving, from, to, castle));
        MoveResult path = reactions.run(MechanicPhase.MOVE_PATH);
        if (!path.isSuccess()) return Preview.rejected(path);
        boolean ep = MoveValidator.isEnPassant(oldBoard, side, from, to, context.ep());
        Position captureSquare = ep ? new Position(from.getRow(), to.getColumn()) : to;
        Figure victim = oldBoard.get(captureSquare);
        PieceState victimState = context.at(captureSquare);
        if (victimState != null && victimState.isPotted()) return Preview.rejected(StatusRejection.POTTED);
        if (castle && context.at(MoveValidator.rookSource(from, to)).isPotted())
            return Preview.rejected(StatusRejection.POTTED);
        if (victimState != null && victimState.isBoxed()) return Preview.rejected(StatusRejection.BOXED);
        MoveResult attacker = reactions.run(MechanicPhase.MOVE_ATTACKER);
        if (!attacker.isSuccess()) return Preview.rejected(attacker);
        if (castle && context.at(MoveValidator.rookSource(from, to)).isBoxed())
            return Preview.rejected(StatusRejection.BOXED);
        MoveResult contact = reactions.run(MechanicPhase.MOVE_TARGET);
        if (!contact.isSuccess()) return Preview.rejected(contact);
        if (reactions.interceptedMove()) {
            Context hit = new Context(context.field(), reactions.pieces(), context.rights(), null, context.turn(), reactions.states(), context.heroes());
            if (AttackRules.isInCheck(hit.field().getGameField(), hit.pieces(), side, hit.terrain()))
                return Preview.rejected(MovementRejection.KING_IN_CHECK);
            return recordMove(new Preview(Success.SUCCESS, hit, List.of(), reactions.proposedEvents()), moving.id());
        }
        if (mode == GameMode.MOBA && moving.isBlinded() && victimState != null)
            return Preview.rejected(StatusRejection.BLINDED_CANNOT_CAPTURE);
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
            states.put(moving.id(), context.heroes().get(moving.hero()).afterCapture(states.get(moving.id())));
            if (ep) board.put(captureSquare, field.createFigure(Empty.class, captureSquare, FigureColor.NONE));
            if (mode == GameMode.MOBA) {
                HeroEffect reaction = reactions.react(states, victimState, moving.id(), to);
                if (!reaction.result().isSuccess()) return Preview.rejected(reaction.result());
                rights = applyEffect(field, board, states, rights, reaction);
                captures.addAll(reaction.captures());
            }
        }
        if (castle) {
            Position rookFrom = MoveValidator.rookSource(from, to), rookTo = MoveValidator.rookDestination(from, to);
            PieceState rook = context.at(rookFrom);
            board.put(rookFrom, field.createFigure(Empty.class, rookFrom, FigureColor.NONE));
            board.put(rookTo, field.createFigure(Rook.class, rookTo, side));
            states.put(rook.id(), rook.placed(rookTo, Rook.class));
        }
        Position nextEp = figure instanceof Pawn && Math.abs(to.getRow() - from.getRow()) == 2
                ? new Position((from.getRow() + to.getRow()) / 2, from.getColumn()) : null;
        reactions.pieces(states);
        reactions.captures(captures);
        var preview = finish(context, reactions, rights, nextEp, side, mode);
        return mode == GameMode.MOBA ? recordMove(preview, moving.id()) : preview;
    }

    static Preview recordMove(Preview preview, long actor) {
        if (!preview.result().isSuccess()) return preview;
        var next = preview.next();
        var piece = next.pieces().get(actor);
        var session = new MechanicSession(next.heroContext(), next.pieces(), next.heroes().rules(),
                next.heroes(), piece.color(), null, preview.captures());
        session.movement(new MoveIntent(piece, piece.position(), piece.position(), false));
        MoveResult result = session.run(MechanicPhase.AFTER_MOVE);
        if (!result.isSuccess()) return Preview.rejected(result);
        next.heroes().validateStates(session.pieces());
        var events = new ArrayList<>(preview.events()); events.addAll(session.proposedEvents());
        return new Preview(Success.SUCCESS, new Context(next.field(), session.pieces(), next.rights(), next.ep(),
                next.turn(), session.states(), next.heroes()), session.captures(), events);
    }

    static MoveResult abilityReady(Context context, FigureColor side, Position actorSquare, Abilities ability, HeroRules rules) {
        return abilityReady(context, side, actorSquare, ability, rules, false);
    }

    static MoveResult abilityReady(Context context, FigureColor side, Position actorSquare, Abilities ability, HeroRules rules, boolean borrowed) {
        return abilityReady(context, side, new AbilityRequest(actorSquare, ability, null, null, borrowed), rules);
    }

    private static MoveResult abilityReady(Context context, FigureColor side, AbilityRequest request, HeroRules rules) {
        Position actorSquare = request.actor();
        Abilities ability = request.ability();
        boolean borrowed = request.borrowed();
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (!MoveValidator.inside(context.field(), actorSquare)) return MovementRejection.OUT_OF_BOUNDS;
        PieceState actor = context.at(actorSquare);
        if (actor == null) return MovementRejection.EMPTY_SOURCE;
        if (actor.color() != side) return MovementRejection.WRONG_FIGURE_COLOR;
        if (actor.isPotted()) return StatusRejection.POTTED;
        if (actor.isBoxed()) return StatusRejection.BOXED;
        if (actor.isStunned()) return StatusRejection.STUNNED;
        if (context.faun().silenced(actorSquare)) return StatusRejection.SILENCED;
        MoveResult ready;
        if (borrowed) {
            var stored = context.spells().borrowed().get(actor.id());
            if (actor.hero() != HeroType.GINSENG || stored == null || stored.ability() != ability)
                return AbilityRejection.NO_BORROWED_ABILITY;
            if (!stored.isReady(context.turn())) return AbilityRejection.BORROWED_NOT_READY;
            Hero owner = context.heroes().ownerOf(ability);
            ready = owner.availability(actor, ability, AbilityUse.of(owner.type(), true));
        } else ready = context.heroes().get(actor.hero()).availability(actor, ability);
        if (!ready.isSuccess()) return ready;
        var session = new MechanicSession(context.heroContext(), context.pieces(), rules,
                context.heroes(), side, request, List.of());
        return session.run(MechanicPhase.ABILITY_GUARD);
    }

    static Preview ability(Context context, FigureColor side, Position actorSquare, Position targetSquare,
                           Abilities ability, HeroRules rules) {
        return ability(context, side, new AbilityRequest(actorSquare, ability, targetSquare), rules);
    }

    static Preview ability(Context context, FigureColor side, AbilityRequest request, HeroRules rules) {
        if (request == null) return Preview.rejected(AbilityRejection.INVALID_ABILITY);
        Position actorSquare = request.actor();
        Abilities ability = request.ability();
        MoveResult ready = abilityReady(context, side, request, rules);
        if (!ready.isSuccess()) return Preview.rejected(ready);
        PieceState actor = context.at(actorSquare);
        Hero behavior = request.borrowed() ? context.heroes().ownerOf(ability) : context.heroes().get(actor.hero());
        HeroActionContext heroContext = context.heroContext();
        HeroEffect effect = behavior.preview(heroContext, request);
        if (!effect.result().isSuccess()) return Preview.rejected(effect.result());
        var states = new HashMap<>(context.pieces());
        var board = new HashMap<>(context.field().getGameField());
        CastlingRights rights = applyEffect(context.field(), board, states, context.rights(), effect);
        var session = new MechanicSession(heroContext, states, rules, context.heroes(), side, request, effect.captures());
        session.enqueue(effect.operations());
        MoveResult prepared = session.run(MechanicPhase.ABILITY_PREPARE);
        if (!prepared.isSuccess()) return Preview.rejected(prepared);
        return finish(context, session, rights, context.ep(), side, GameMode.MOBA);
    }

    private static Preview finish(Context before, MechanicSession session,
                                  CastlingRights rights, Position ep, FigureColor side, GameMode mode) {
        MoveResult resolved = session.settle();
        if (!resolved.isSuccess()) return Preview.rejected(resolved);
        MoveResult settled = session.run(MechanicPhase.SETTLED);
        if (!settled.isSuccess()) return Preview.rejected(settled);
        var board = new HashMap<Position, Figure>();
        for (Position square : before.field().getGameField().keySet())
            board.put(square, before.field().createFigure(Empty.class, square, FigureColor.NONE));
        for (PieceState piece : session.pieces().values())
            if (piece.status() == PieceState.Status.BOARD)
                board.put(piece.position(), before.field().createFigure(piece.type(), piece.position(), piece.color()));
        for (PieceState old : before.pieces().values()) {
            PieceState next = session.pieces().get(old.id());
            if (old.position() != null && (next.position() == null || !old.position().equals(next.position()) || next.isPotted()))
                rights = removedRookRights(rights, old, old.position());
        }
        var kings = session.pieces().values().stream().filter(p -> p.position() != null && p.type() == King.class).toList();
        if (kings.size() == 2 && Math.max(Math.abs(kings.get(0).position().getRow() - kings.get(1).position().getRow()), Math.abs(kings.get(0).position().getColumn() - kings.get(1).position().getColumn())) <= 1)
            return Preview.rejected(MovementRejection.KING_IN_CHECK);
        if (AttackRules.isInCheck(board, session.pieces(), side, session.context().terrain()))
            return Preview.rejected(MovementRejection.KING_IN_CHECK);
        Field next = mode == GameMode.MOBA ? Field.fromMobaPosition(board) : Field.fromPosition(board);
        MoveResult rewarded = session.run(MechanicPhase.AFTER_SAFETY);
        if (!rewarded.isSuccess()) return Preview.rejected(rewarded);
        MoveResult action = session.run(MechanicPhase.AFTER_ACTION);
        if (!action.isSuccess()) return Preview.rejected(action);
        session.requireComplete();
        before.heroes().validateStates(session.pieces());
        return new Preview(Success.SUCCESS, new Context(next, session.pieces(), rights,
                validEp(board, ep, session.pieces()), before.turn(), session.states(), before.heroes()), session.captures(), session.proposedEvents());
    }

    static boolean canEnd(Context context, FigureColor side) {
        var session = new MechanicSession(context.heroContext(), context.pieces(),
                context.heroes().rules(), context.heroes(), side, null, List.of());
        if (!session.run(MechanicPhase.CAN_END).isSuccess()) return false;
        var pieces = session.pieces();
        var board = new HashMap<>(context.field().getGameField());
        for (var old : context.pieces().values())
            if (old.position() != null && pieces.get(old.id()).position() == null)
                board.put(old.position(), context.field().createFigure(Empty.class, old.position(), FigureColor.NONE));
        return !AttackRules.isInCheck(board, pieces, side, session.context().terrain());
    }

    static boolean canFinishAfterMove(Context context, FigureColor side, boolean abilityRemaining, HeroRules rules) {
        if (canEnd(context, side)) return true;
        if (abilityRemaining && anyAbility(context, side, rules, next -> canFinishAfterMove(next, side, false, rules)))
            return true;
        for (var piece : context.pieces().values()) {
            if (piece.position() == null || piece.color() != side || !context.heroes().mechanics().allowsExtraMove(context.heroContext(), piece))
                continue;
            var candidates = MoveValidator.candidates(context.field(), context.field().getFigureByPosition(piece.position()), context.rights(), context.ep());
            for (var targets : List.of(candidates.getCanMove(), candidates.getCanAttack()))
                for (var to : targets) {
                    var preview = move(context, side, piece.position(), to, null, GameMode.MOBA, rules);
                    if (preview.result().isSuccess() && canFinishAfterMove(preview.next(), side, abilityRemaining, rules))
                        return true;
                }
        }
        return false;
    }

    static boolean hasMove(Context context, FigureColor side, GameMode mode, HeroRules rules) {
        return anyMove(context, side, mode, rules, next -> canFinishAfterMove(next, side, false, rules));
    }

    private static boolean anyMove(Context context, FigureColor side, GameMode mode, HeroRules rules, Predicate<Context> continuation) {
        for (var entry : context.field().getGameField().entrySet()) {
            Figure piece = entry.getValue();
            if (piece.getColor() != side) continue;
            AvailableMoves candidates = MoveValidator.candidates(context.field(), piece, context.rights(), context.ep());
            for (var targets : List.of(candidates.getCanMove(), candidates.getCanAttack()))
                for (Position target : targets) {
                    Promotion promotion = MoveValidator.isPromotion(piece, target, 8) ? Promotion.QUEEN : null;
                    Preview preview = move(context, side, entry.getKey(), target, promotion, mode, rules);
                    if (preview.result().isSuccess() && continuation.test(preview.next())) return true;
                }
        }
        return false;
    }

    static List<AbilityRequest> requests(Context context, FigureColor side, Position square, Abilities ability, boolean borrowed, HeroRules rules) {
        if (!abilityReady(context, side, square, ability, rules, borrowed).isSuccess()) return List.of();
        PieceState actor = context.at(square);
        Hero behavior = borrowed ? context.heroes().ownerOf(ability) : context.heroes().get(actor.hero());
        return behavior.requests(context.heroContext(), actor, ability, borrowed);
    }

    private static boolean anyAbility(Context context, FigureColor side, HeroRules rules, Predicate<Context> continuation) {
        for (PieceState piece : context.pieces().values()) {
            if (piece.position() == null || piece.color() != side) continue;
            for (boolean borrowed : List.of(false, true)) {
                var copy = context.spells().borrowed().get(piece.id());
                var abilities = borrowed ? (copy == null ? List.<Abilities>of() : List.of(copy.ability())) : context.heroes().get(piece.hero()).abilities();
                for (Abilities ability : abilities)
                    for (var request : requests(context, side, piece.position(), ability, borrowed, rules)) {
                        Preview preview = ability(context, side, request, rules);
                        if (preview.result().isSuccess() && continuation.test(preview.next())) return true;
                    }
            }
        }
        return false;
    }

    /**
     * A turn must still be finishable when temporary terrain expires.
     */
    static boolean hasTurn(Context context, FigureColor side, HeroRules rules) {
        if (anyMove(context, side, GameMode.MOBA, rules, next -> canFinishAfterMove(next, side, true, rules)))
            return true;
        return anyAbility(context, side, rules, next -> hasMove(next, side, GameMode.MOBA, rules));
    }

    /**
     * Apply a prepared hero patch to this action's private copies, never to the live Game.
     */
    private static CastlingRights applyEffect(Field factory, Map<Position, Figure> board, Map<Long, PieceState> states,
                                              CastlingRights rights, HeroEffect effect) {
        for (var entry : effect.updates().entrySet()) {
            PieceState old = states.get(entry.getKey()), next = entry.getValue();
            if (old.position() != null) {
                board.put(old.position(), factory.createFigure(Empty.class, old.position(), FigureColor.NONE));
                if (!old.position().equals(next.position()) || next.isPotted())
                    rights = removedRookRights(rights, old, old.position());
            }
        }
        states.putAll(effect.updates());
        for (PieceState next : effect.updates().values())
            if (next.position() != null)
                board.put(next.position(), factory.createFigure(next.type(), next.position(), next.color()));
        return rights;
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

    private static Position validEp(Map<Position, Figure> board, Position ep, Map<Long, PieceState> pieces) {
        if (ep == null) return null;
        FigureColor pawnColor = ep.getRow() == 2 ? FigureColor.WHITE : FigureColor.BLACK;
        int direction = pawnColor == FigureColor.WHITE ? 1 : -1;
        Position square = new Position(ep.getRow() + direction, ep.getColumn());
        Position origin = new Position(ep.getRow() - direction, ep.getColumn());
        Figure pawn = board.get(square);
        return board.get(ep) instanceof Empty && board.get(origin) instanceof Empty
                && pawn instanceof Pawn && pawn.getColor() == pawnColor && pieces.values().stream().noneMatch(p -> p.isPotted() && square.equals(p.position())) ? ep : null;
    }
}
