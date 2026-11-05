package game.core;

import game.draft.Draft;
import game.draft.DraftSnapshot;
import game.economy.EconomyState;
import game.heroes.Abilities;
import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroRegistry;
import game.heroes.HeroPieceFactory;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.ares.AresRules;
import game.heroes.bolotnik.BolotnikRules;
import game.heroes.bountycrow.BountyCrowRules;
import game.heroes.chronomlin.ChronomlinRules;
import game.heroes.clownganger.ClowngangerRules;
import game.heroes.faun.FaunRules;
import game.heroes.faun.FaunState;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.GinsengRules;
import game.heroes.ginseng.SpellState;
import game.heroes.izba.IzbaRules;
import game.heroes.kikimora.KikimoraRules;
import game.heroes.lumeshard.LumeshardRules;
import game.heroes.moleminer.MoleminerRules;
import game.heroes.pudge.PudgeRules;
import game.heroes.zeus.ZeusRules;
import game.history.GameEvent;
import game.history.MadeMove;
import game.mechanics.BuiltInStates;
import game.mechanics.MechanicPhase;
import game.mechanics.MechanicSession;
import game.mechanics.MechanicStates;
import game.moves.AbilityRejection;
import game.moves.AvailableMoves;
import game.moves.Capture;
import game.moves.DrawRejection;
import game.moves.ItemRejection;
import game.moves.MoveKind;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Promotion;
import game.moves.Success;
import game.moves.TurnRejection;
import game.state.CastlingRights;
import game.state.ClockSnapshot;
import game.state.GameMode;
import game.state.GameOutcome;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.state.Termination;
import game.state.TurnState;
import game.summoning.SummonState;
import game.terrain.TerrainState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.Bishop;
import model.figures.Empty;
import model.figures.King;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Queen;
import model.figures.Rook;
import rules.moba.AttackRules;
import rules.standard.MaterialRules;
import rules.standard.MoveValidator;
import rules.standard.PositionValidator;

public final class Game {
    private Field gameField;
    private final GameMode mode;
    private final HeroRules heroRules;
    private final HeroRegistry heroRegistry;
    private MechanicStates mechanics = MechanicStates.empty();
    private DraftSnapshot draft;
    private Map<Long, PieceState> pieces = Map.of();
    private Field pieceStateField;
    private long nextPieceId = 1, version, turnNumber = 1;
    private boolean moveUsed, abilityUsed;
    private final List<GameEvent> events = new ArrayList<>();
    private final Player whitePlayer = new Player(true), blackPlayer = new Player(false);
    private final LongSupplier nanoTime;
    private final ChessClock clock = new ChessClock();
    private final List<MadeMove> moves = new ArrayList<>();
    private final Map<String, Integer> repetitions = new HashMap<>();
    private Player currentPlayer = whitePlayer;
    private Player drawOffer;
    private GameOutcome outcome = GameOutcome.ongoing();
    private CastlingRights castlingRights = CastlingRights.initial();
    private Position enPassantTarget;
    private int halfmoveClock;

    /**
     * Untimed game. The UI starts the clock when the user starts playing.
     */
    public Game(int n) {
        this(n, System::nanoTime);
    }

    public Game(int n, LongSupplier nanoTime) {
        this(n, GameMode.CLASSIC, nanoTime, HeroRules.withPudge(new PudgeRules(0, 0, true, false)));
    }

    private Game(int n, GameMode mode, LongSupplier nanoTime, HeroRules rules) {
        gameField = new Field(n);
        this.mode = Objects.requireNonNull(mode);
        if (mode == GameMode.MOBA) mechanics = mechanics.with(BuiltInStates.ECONOMY, EconomyState.initial());
        this.heroRules = Objects.requireNonNull(rules);
        this.heroRegistry = HeroRegistry.fromRules(rules);
        this.nanoTime = Objects.requireNonNull(nanoTime);
        rebuildPieces(Map.of());
        repetitions.put(positionKey(gameField.getGameField(), currentPlayer.color(), castlingRights, null), 1);
    }

    /**
     * Standard prototype: Pudge on a1/a8, Lumeshard on c1/c8; other pieces retain their normal type.
     */
    public static Game mobaPrototype(PudgeRules rules, int seconds) {
        return mobaPrototype(HeroRules.withPudge(rules), seconds);
    }

    public static Game mobaPrototype(HeroRules rules, int seconds) {
        Game game = new Game(8, GameMode.MOBA, System::nanoTime, rules);
        game.rebuildPieces(Map.of(new Position(0, 0), HeroType.PUDGE, new Position(7, 0), HeroType.PUDGE,
                new Position(0, 2), HeroType.LUMESHARD, new Position(7, 2), HeroType.LUMESHARD,
                new Position(1, 1), HeroType.BOUNTY_CROW, new Position(6, 1), HeroType.BOUNTY_CROW,
                new Position(0, 6), HeroType.BOLOTNIK, new Position(7, 6), HeroType.BOLOTNIK));
        game.startClock(seconds);
        return game;
    }

    /**
     * Create the initial board from an audited completed draft, then start the game clock.
     */
    public static Game mobaFromDraft(DraftSnapshot draft, HeroRules rules, int seconds) {
        return mobaFromDraft(draft, rules, seconds, System::nanoTime);
    }

    public static Game mobaFromDraft(DraftSnapshot draft, HeroRules rules, int seconds, LongSupplier time) {
        DraftSnapshot verified = Draft.fromSnapshot(draft, rules).getSnapshot();
        if (!verified.isComplete()) throw new IllegalArgumentException("Complete the draft before starting the game");
        Game game = new Game(8, GameMode.MOBA, time, rules);
        game.rebuildPieces(verified.assignments());
        game.draft = verified;
        game.startClock(seconds);
        return game;
    }

    public static Game fromMobaPosition(GamePosition position, Map<Position, HeroType> heroes, PudgeRules rules) {
        return fromMobaPosition(position, heroes, rules, System::nanoTime);
    }

    public static Game fromMobaPosition(GamePosition position, Map<Position, HeroType> heroes, PudgeRules rules, LongSupplier time) {
        return fromMobaPosition(position, heroes, HeroRules.withPudge(rules), time);
    }

    public static Game fromMobaPosition(GamePosition position, Map<Position, HeroType> heroes, HeroRules rules) {
        return fromMobaPosition(position, heroes, rules, System::nanoTime);
    }

    public static Game fromMobaPosition(GamePosition position, Map<Position, HeroType> heroes, HeroRules rules, LongSupplier time) {
        if (position == null || heroes == null)
            throw new IllegalArgumentException("Position and hero assignments are required");
        Field field = Field.fromMobaPosition(position.board());
        for (FigureColor color : List.of(FigureColor.WHITE, FigureColor.BLACK))
            if (field.getGameField().values().stream().filter(p -> p.getColor() == color).count() > 16)
                throw new IllegalArgumentException("Extra pieces require a snapshot with summon origins");
        PositionValidator.validateState(field, position.sideToMove(), position.castlingRights(), position.enPassantTarget(), position.halfmoveClock());
        Game game = new Game(8, GameMode.MOBA, time, rules);
        game.gameField = field;
        game.currentPlayer = position.sideToMove() == FigureColor.WHITE ? game.whitePlayer : game.blackPlayer;
        game.castlingRights = position.castlingRights();
        game.enPassantTarget = position.enPassantTarget();
        game.halfmoveClock = position.halfmoveClock();
        game.rebuildPieces(new HashMap<>(heroes));
        game.repetitions.clear();
        game.repetitions.put(game.getPositionKey(), 1);
        game.evaluateOutcome();
        return game;
    }

    public Game(int n, int seconds) {
        this(n);
        startClock(seconds);
    }

    /**
     * Restore a complete validated local game. Time spent outside this Game is not charged.
     */
    public static Game fromSnapshot(GameSnapshot saved) {
        return fromSnapshot(saved, System::nanoTime);
    }

    public static Game fromSnapshot(GameSnapshot saved, LongSupplier time) {
        SnapshotValidator.validate(saved);
        Game game = new Game(8, saved.mode(), time, saved.heroRules());
        game.gameField = saved.mode() == GameMode.MOBA ? Field.fromMobaPosition(saved.field().getGameField())
                : Field.fromPosition(saved.field().getGameField());
        game.pieces = saved.pieces();
        game.pieceStateField = game.gameField;
        game.mechanics = saved.mechanics();
        game.draft = saved.draft();
        game.nextPieceId = saved.pieces().keySet().stream().mapToLong(Long::longValue).max().orElse(0L) + 1;
        game.version = saved.version();
        game.turnNumber = saved.turn().number();
        game.moveUsed = saved.turn().moveUsed();
        game.abilityUsed = saved.turn().abilityUsed();
        game.currentPlayer = saved.active() == FigureColor.WHITE ? game.whitePlayer : game.blackPlayer;
        game.castlingRights = saved.castlingRights();
        game.enPassantTarget = saved.enPassantTarget();
        game.halfmoveClock = saved.halfmoveClock();
        game.outcome = saved.outcome();
        game.repetitions.clear();
        game.repetitions.putAll(saved.repetitions());
        game.moves.addAll(saved.moves());
        game.events.addAll(saved.events());
        for (MadeMove move : saved.moves())
            (move.getColor() == FigureColor.WHITE ? game.whitePlayer : game.blackPlayer).recordMove(move);
        saved.whiteCaptures().forEach(game.whitePlayer::recordCapture);
        saved.blackCaptures().forEach(game.blackPlayer::recordCapture);
        game.drawOffer = saved.drawOffer() == FigureColor.NONE ? null
                : saved.drawOffer() == FigureColor.WHITE ? game.whitePlayer : game.blackPlayer;
        game.clock.restore(saved.clock(), time.getAsLong());
        return game;
    }

    /**
     * Starts a new untimed game from a validated position, with empty history.
     */
    public static Game fromPosition(GamePosition position) {
        return fromPosition(position, System::nanoTime);
    }

    public static Game fromPosition(GamePosition position, LongSupplier nanoTime) {
        if (position == null) throw new IllegalArgumentException("Position is required");
        Field field = Field.fromPosition(position.board());
        PositionValidator.validateState(field, position.sideToMove(), position.castlingRights(),
                position.enPassantTarget(), position.halfmoveClock());
        Game game = new Game(8, nanoTime);
        game.gameField = field;
        game.currentPlayer = position.sideToMove() == FigureColor.WHITE ? game.whitePlayer : game.blackPlayer;
        game.castlingRights = position.castlingRights();
        game.enPassantTarget = position.enPassantTarget();
        game.halfmoveClock = position.halfmoveClock();
        game.rebuildPieces(Map.of());
        game.repetitions.clear();
        game.repetitions.put(game.getPositionKey(), 1);
        game.evaluateOutcome();
        return game;
    }

    /**
     * One coherent position snapshot. Histories and clocks are not a saved-game format.
     */
    public synchronized GamePosition getPosition() {
        return new GamePosition(gameField.getGameField(), currentPlayer.color(), castlingRights, enPassantTarget, halfmoveClock);
    }

    public synchronized void startClock(int seconds) {
        if (outcome.isFinished() || !moves.isEmpty() || !events.isEmpty())
            throw new IllegalStateException("Configure the clock before playing");
        clock.start(seconds, currentPlayer.color(), nanoTime.getAsLong());
        version++;
    }

    public synchronized MoveResult moveFigure(Player player, Position from, Position to) {
        return moveFigure(player, from, to, null);
    }

    /**
     * Rejections preserve chess state; elapsed real time is still charged.
     */
    public synchronized MoveResult moveFigure(Player player, Position from, Position to, Promotion promotion) {
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode == GameMode.MOBA && moveUsed && (getPieceState(from)==null || !heroRegistry.mechanics().allowsExtraMove(context().heroContext(), getPieceState(from)))) return TurnRejection.MOVE_ALREADY_USED;
        ensurePieces();
        ActionEngine.Context before = context();
        from = copy(from);
        to = copy(to);
        ActionEngine.Preview preview = ActionEngine.move(before, player.color(), from, to, promotion, mode, heroRules);
        if (!preview.result().isSuccess()) return preview.result();
        if (mode == GameMode.CLASSIC) preview = ActionEngine.recordMove(preview, before.at(from).id());
        if (!preview.result().isSuccess()) return preview.result();
        if (mode == GameMode.MOBA && !ActionEngine.canFinishAfterMove(preview.next(), player.color(), !abilityUsed, heroRules))
            return TurnRejection.NO_TURN_CONTINUATION;
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        Figure figure = gameField.getFigureByPosition(from);
        PieceState actor = before.at(from);
        long actingTurn = turnNumber;
        boolean castle = MoveValidator.isCastling(figure, from, to);
        boolean ep = MoveValidator.isEnPassant(gameField.getGameField(), player.color(), from, to, enPassantTarget);
        Capture firstCapture = preview.captures().stream().filter(c -> c.cause() == Capture.Cause.MOVE && c.actorId() == actor.id()).findFirst().orElse(null);
        MoveKind kind = castle ? MoveKind.CASTLING : ep ? MoveKind.EN_PASSANT
                                                     : promotion != null ? MoveKind.PROMOTION : firstCapture != null ? MoveKind.CAPTURE : MoveKind.NORMAL;
        String name = actor.hero() == HeroType.NONE ? figure.getName() : heroRegistry.get(actor.hero()).definition().displayName();
        MadeMove made = new MadeMove(name, player.color(), from, to, kind, promotion,
                firstCapture == null ? null : firstCapture.victim().getName(), firstCapture == null ? null : firstCapture.square(),
                moves.size() + 1, clock.snapshot());
        var primary = new GameEvent(events.size() + 1L, actingTurn, GameEvent.Kind.MOVE, player.color(), actor.id(),
                firstCapture == null ? null : firstCapture.victimId(), from, to, made.toString());
        var journal = prepareActionEvents(before, preview, primary);
        if (!journal.result().isSuccess()) return journal.result();
        if (moves.isEmpty() && events.isEmpty()) {
            repetitions.clear();
            repetitions.put(getPositionKey(), 1);
        }
        publish(preview);
        halfmoveClock = figure instanceof Pawn || !preview.captures().isEmpty() ? 0 : halfmoveClock + 1;
        if (drawOffer != null && drawOffer != player) drawOffer = null;
        if (mode == GameMode.CLASSIC) {
            advanceTurn();
            repetitions.merge(getPositionKey(), 1, Integer::sum);
            evaluateOutcome();
        } else moveUsed = true;
        if (mode == GameMode.CLASSIC)
            made = new MadeMove(name, player.color(), from, to, kind, promotion,
                    firstCapture == null ? null : firstCapture.victim().getName(), firstCapture == null ? null : firstCapture.square(),
                    moves.size() + 1, clock.snapshot());
        player.recordMove(made);
        moves.add(made);
        events.addAll(journal.events());
        version++;
        return Success.SUCCESS;
    }

    public synchronized MoveResult useAbility(Player player, Position actorSquare, Position target, Abilities ability) {
        return useAbility(player, new AbilityRequest(actorSquare, ability, target));
    }

    public synchronized MoveResult useAbility(Player player, AbilityRequest request) {
        if (request == null) return AbilityRejection.INVALID_ABILITY;
        Position actorSquare = request.actor(), target = request.target();
        Abilities ability = request.ability();
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode != GameMode.MOBA) return TurnRejection.UNSUPPORTED_ACTION;
        if (abilityUsed) return TurnRejection.ABILITY_ALREADY_USED;
        ActionEngine.Context before = context();
        ActionEngine.Preview preview = ActionEngine.ability(before, player.color(), request, heroRules);
        if (!preview.result().isSuccess()) return preview.result();
        if (!moveUsed && !ActionEngine.hasMove(preview.next(), player.color(), mode, heroRules))
            return TurnRejection.NO_MOVE_AFTER_ABILITY;
        if (moveUsed && !ActionEngine.canEnd(preview.next(), player.color())) return TurnRejection.NO_TURN_CONTINUATION;
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        PieceState actor = before.at(actorSquare), victim = before.at(target);
        var behavior = request.borrowed() ? heroRegistry.ownerOf(ability) : heroRegistry.get(actor.hero());
        String message = behavior.abilityMessage(before.heroContext(), preview.next().heroContext(), actor, victim, request);
        if (request.borrowed()) message = "Ginseng, копия: " + message;
        var details = behavior.abilityEventDetails(before.heroContext(), preview.next().heroContext(), actor, victim, request);
        var primary = GameEvent.abilityUsed(events.size() + 1L, turnNumber, ability, player.color(), actor.id(),
                details.targetId(), actorSquare, details.to(), message, details.copiedAbility(), request.borrowed());
        var journal = prepareActionEvents(before, preview, primary);
        if (!journal.result().isSuccess()) return journal.result();
        publish(preview);
        abilityUsed = true;
        if (!preview.captures().isEmpty()) halfmoveClock = 0;
        if (drawOffer != null && drawOffer != player) drawOffer = null;
        events.addAll(journal.events());
        version++;
        return Success.SUCCESS;
    }

    public synchronized MoveResult endTurn(Player player) {
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode != GameMode.MOBA) return TurnRejection.UNSUPPORTED_ACTION;
        if (!moveUsed) return TurnRejection.MOVE_REQUIRED;
        if (!ActionEngine.canEnd(context(), player.color())) return MovementRejection.KING_IN_CHECK;
        var before = context();
        var session = new MechanicSession(before.heroContext(), pieces, heroRules,
                heroRegistry, player.color(), null, List.of(), events);
        MoveResult ended = session.run(MechanicPhase.END_TURN);
        if (!ended.isSuccess()) return ended;
        session.requireComplete();
        var board = new HashMap<Position, Figure>();
        for (Position square : gameField.getGameField().keySet())
            board.put(square, gameField.createFigure(Empty.class, square, FigureColor.NONE));
        for (PieceState piece : session.pieces().values())
            if (piece.position() != null)
                board.put(piece.position(), gameField.createFigure(piece.type(), piece.position(), piece.color()));
        MoveResult historyResult = session.run(MechanicPhase.END_HISTORY);
        if (!historyResult.isSuccess()) return historyResult;
        for (var event : session.proposedEvents()) session.emit(event.numbered(session.history().size() + 1L));
        Map<Long, PieceState> nextPieces = prepareNextTurnPieces(session.pieces());
        pieces = nextPieces;
        mechanics = session.states();
        gameField = Field.fromMobaPosition(board);
        pieceStateField = gameField;
        events.addAll(session.history().subList(events.size(), session.history().size()));
        addEvent(GameEvent.Kind.END_TURN, player.color(), null, null, null, null, "Ход завершён");
        if (drawOffer != null && drawOffer != player) drawOffer = null;
        advanceTurn();
        repetitions.merge(getPositionKey(), 1, Integer::sum);
        evaluateOutcome();
        version++;
        return Success.SUCCESS;
    }

    /**
     * Independent from move/skill budgets. Actual item effects await a designed catalog.
     */
    public synchronized MoveResult useItem(Player player, long pieceId, int slot) {
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode != GameMode.MOBA) return TurnRejection.UNSUPPORTED_ACTION;
        PieceState piece = pieces.get(pieceId);
        if (piece == null || piece.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (piece.color() != player.color()) return MovementRejection.WRONG_FIGURE_COLOR;
        if (slot < 0 || slot >= 3) return ItemRejection.INVALID_ITEM_SLOT;
        return piece.inventory().get(slot).isEmpty() ? ItemRejection.EMPTY_ITEM_SLOT : ItemRejection.ITEM_NOT_AVAILABLE;
    }

    public synchronized List<Position> getAbilityTargets(Position actorSquare, Abilities ability) {
        return getAbilityTargets(actorSquare, ability, false);
    }

    public synchronized List<Position> getAbilityTargets(Position actorSquare, Abilities ability, boolean borrowed) {
        updateTime();
        if (mode != GameMode.MOBA || outcome.isFinished() || abilityUsed) return List.of();
        var before = context();
        var targets = new LinkedHashSet<Position>();
        for (var request : ActionEngine.requests(before, currentPlayer.color(), actorSquare, ability, borrowed, heroRules)) {
            if (request.target() == null || targets.contains(request.target())) continue;
            var preview = ActionEngine.ability(before, currentPlayer.color(), request, heroRules);
            if (canCompleteAbility(preview)) targets.add(request.target());
        }
        return List.copyOf(targets);
    }

    public synchronized List<Position> getDisplacementDestinations(Position actor, Position target, boolean borrowed) {
        return getAbilityDestinations(actor, GinsengAbility.DISPLACE, target, borrowed);
    }

    public synchronized List<Position> getAbilityDestinations(Position actor, Abilities ability, Position target) {
        return getAbilityDestinations(actor, ability, target, false);
    }

    /** Second-stage highlights use the same complete requests and continuation checks as execution. */
    public synchronized List<Position> getAbilityDestinations(Position actor, Abilities ability, Position target,
                                                              boolean borrowed) {
        updateTime();
        if (mode != GameMode.MOBA || outcome.isFinished() || abilityUsed) return List.of();
        var before = context();
        var result = new LinkedHashSet<Position>();
        for (var request : ActionEngine.requests(before, currentPlayer.color(), actor, ability, borrowed, heroRules))
            if (request.destination() != null && Objects.equals(request.target(), target)
                    && !result.contains(request.destination())) {
                if (canCompleteAbility(ActionEngine.ability(before, currentPlayer.color(), request, heroRules)))
                    result.add(request.destination());
            }
        return List.copyOf(result);
    }

    private boolean canCompleteAbility(ActionEngine.Preview preview) {
        return preview.result().isSuccess() && (moveUsed ? ActionEngine.canEnd(preview.next(), currentPlayer.color()) : ActionEngine.hasMove(preview.next(), currentPlayer.color(), mode, heroRules));
    }

    public synchronized MoveResult getAbilityAvailability(Position actorSquare, Abilities ability) {
        return getAbilityAvailability(actorSquare, ability, false);
    }

    public synchronized MoveResult getAbilityAvailability(Position actorSquare, Abilities ability, boolean borrowed) {
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        if (mode != GameMode.MOBA) return TurnRejection.UNSUPPORTED_ACTION;
        if (abilityUsed) return TurnRejection.ABILITY_ALREADY_USED;
        return ActionEngine.abilityReady(context(), currentPlayer.color(), actorSquare, ability, heroRules, borrowed);
    }

    public synchronized SpellState getSpells() {
        return spells();
    }

    public synchronized FaunState getFaun() {
        return faun();
    }

    public synchronized AvailableMoves getLegalMoves(Position from) {
        updateTime();
        if (outcome.isFinished() || (mode == GameMode.MOBA && moveUsed && (getPieceState(from)==null || !heroRegistry.mechanics().allowsExtraMove(context().heroContext(), getPieceState(from)))))
            return new AvailableMoves(List.of(), List.of());
        return legalMoves(copy(from), currentPlayer.color());
    }

    private AvailableMoves legalMoves(Position from, FigureColor color) {
        if (!MoveValidator.inside(gameField, from)) return new AvailableMoves(List.of(), List.of());
        Figure figure = getFigureByPosition(from);
        if (figure == null || figure.getColor() != color) return new AvailableMoves(List.of(), List.of());
        AvailableMoves candidates = MoveValidator.candidates(gameField, figure, castlingRights, enPassantTarget);
        return new AvailableMoves(
                candidates.getCanMove().stream().filter(to -> legalTarget(figure, from, to, color)).toList(),
                candidates.getCanAttack().stream().filter(to -> legalTarget(figure, from, to, color)).toList());
    }

    private boolean legalTarget(Figure figure, Position from, Position to, FigureColor color) {
        return validate(color, from, to, MoveValidator.isPromotion(figure, to, gameField.getSize()) ? Promotion.QUEEN : null).isSuccess();
    }

    private MoveResult validate(FigureColor color, Position from, Position to, Promotion promotion) {
        if (mode == GameMode.MOBA) {
            var preview = ActionEngine.move(context(), color, from, to, promotion, mode, heroRules);
            return preview.result().isSuccess() && !ActionEngine.canFinishAfterMove(preview.next(), color, !abilityUsed, heroRules)
                    ? TurnRejection.NO_TURN_CONTINUATION : preview.result();
        }
        return MoveValidator.validate(gameField, color, from, to, castlingRights, enPassantTarget, promotion);
    }

    private boolean hasLegalMove(FigureColor color) {
        for (var entry : gameField.getGameField().entrySet()) {
            Figure figure = entry.getValue();
            if (figure.getColor() != color) continue;
            AvailableMoves candidates = MoveValidator.candidates(gameField, figure, castlingRights, enPassantTarget);
            for (Position to : candidates.getCanMove()) if (legalTarget(figure, entry.getKey(), to, color)) return true;
            for (Position to : candidates.getCanAttack())
                if (legalTarget(figure, entry.getKey(), to, color)) return true;
        }
        return false;
    }

    private void evaluateOutcome() {
        FigureColor color = currentPlayer.color();
        var board = gameField.getGameField();
        if (mode == GameMode.MOBA) {
            if (!ActionEngine.hasTurn(context(), color, heroRules)) {
                boolean check = mode == GameMode.MOBA ? AttackRules.isInCheck(board, pieces, color, terrain()) : MoveValidator.isInCheck(board, color);
                finish(check ? Termination.CHECKMATE : Termination.STALEMATE, check ? MoveValidator.opposite(color) : FigureColor.NONE);
            }
            return;
        }
        if (!hasLegalMove(color)) {
            boolean check = mode == GameMode.MOBA ? AttackRules.isInCheck(board, pieces, color, terrain()) : MoveValidator.isInCheck(board, color);
            finish(check ? Termination.CHECKMATE : Termination.STALEMATE,
                    check ? MoveValidator.opposite(color) : FigureColor.NONE);
        } else if (MaterialRules.isInsufficient(board)) finish(Termination.INSUFFICIENT_MATERIAL, FigureColor.NONE);
        else if (halfmoveClock >= 150) finish(Termination.SEVENTY_FIVE_MOVE_RULE, FigureColor.NONE);
        else if (currentRepetitionCount() >= 5) finish(Termination.FIVEFOLD_REPETITION, FigureColor.NONE);
    }

    private void updateTime() {
        if (outcome.isFinished()) return;
        FigureColor flagged = clock.update(nanoTime.getAsLong());
        if (flagged != FigureColor.NONE) {
            FigureColor winner = MoveValidator.opposite(flagged);
            boolean possible = mode == GameMode.MOBA || MaterialRules.canPossiblyMate(gameField.getGameField(), winner);
            finish(possible ? Termination.TIMEOUT : Termination.TIMEOUT_DRAW, possible ? winner : FigureColor.NONE);
        }
    }

    private void finish(Termination reason, FigureColor winner) {
        outcome = new GameOutcome(reason, winner);
        drawOffer = null;
        clock.stop();
        version++;
    }

    public synchronized MoveResult resign(Player player) {
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        if (!isParticipant(player)) return TurnRejection.WRONG_PLAYER;
        FigureColor winner = MoveValidator.opposite(player.color());
        boolean possible = mode == GameMode.MOBA || MaterialRules.canPossiblyMate(gameField.getGameField(), winner);
        finish(possible ? Termination.RESIGNATION : Termination.RESIGNATION_DRAW, possible ? winner : FigureColor.NONE);
        return Success.SUCCESS;
    }

    public synchronized MoveResult offerDraw(Player player) {
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        if (!isParticipant(player)) return TurnRejection.WRONG_PLAYER;
        if (drawOffer != null) return DrawRejection.DRAW_OFFER_PENDING;
        drawOffer = player;
        version++;
        return Success.SUCCESS;
    }

    public synchronized MoveResult acceptDraw(Player player) {
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        if (!isParticipant(player)) return TurnRejection.WRONG_PLAYER;
        if (drawOffer == null || drawOffer == player) return DrawRejection.NO_DRAW_OFFER;
        if (whitePlayer.getMadeMoves().isEmpty() || blackPlayer.getMadeMoves().isEmpty())
            return DrawRejection.DRAW_AGREEMENT_TOO_EARLY;
        finish(Termination.DRAW_AGREEMENT, FigureColor.NONE);
        return Success.SUCCESS;
    }

    public synchronized MoveResult declineDraw(Player player) {
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        if (!isParticipant(player)) return TurnRejection.WRONG_PLAYER;
        if (drawOffer == null || drawOffer == player) return DrawRejection.NO_DRAW_OFFER;
        drawOffer = null;
        version++;
        return Success.SUCCESS;
    }

    public synchronized boolean canClaimDraw(Player player) {
        updateTime();
        return !outcome.isFinished() && player == currentPlayer && currentClaimReason() != null;
    }

    public synchronized MoveResult claimDraw(Player player) {
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        if (player != currentPlayer) return TurnRejection.WRONG_PLAYER;
        Termination reason = currentClaimReason();
        if (reason == null) return DrawRejection.INVALID_DRAW_CLAIM;
        finish(reason, FigureColor.NONE);
        return Success.SUCCESS;
    }

    /**
     * Declares an intended legal move; a successful claim ends the game without applying it.
     */
    public synchronized MoveResult claimDraw(Player player, Position from, Position to, Promotion promotion) {
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        if (player != currentPlayer) return TurnRejection.WRONG_PLAYER;
        from = copy(from);
        to = copy(to);
        MoveResult validation = validate(player.color(), from, to, promotion);
        if (!validation.isSuccess()) return validation;
        Termination reason = prospectiveClaimReason(from, to, promotion);
        if (reason == null) return DrawRejection.INVALID_DRAW_CLAIM;
        finish(reason, FigureColor.NONE);
        return Success.SUCCESS;
    }

    public synchronized boolean canClaimDrawAfterMove(Player player, Position from, Position to, Promotion promotion) {
        updateTime();
        return !outcome.isFinished() && player == currentPlayer && validate(player.color(), from, to, promotion).isSuccess()
                && prospectiveClaimReason(from, to, promotion) != null;
    }

    private Termination currentClaimReason() {
        if (mode == GameMode.MOBA) return null;
        if (currentRepetitionCount() >= 3) return Termination.THREEFOLD_REPETITION;
        return halfmoveClock >= 100 ? Termination.FIFTY_MOVE_RULE : null;
    }

    private Termination prospectiveClaimReason(Position from, Position to, Promotion promotion) {
        if (mode == GameMode.MOBA) return null;
        var board = gameField.getGameField();
        Figure figure = board.get(from);
        boolean ep = MoveValidator.isEnPassant(board, figure.getColor(), from, to, enPassantTarget);
        Figure target = board.get(ep ? new Position(from.getRow(), to.getColumn()) : to);
        Figure captured = target.getColor() == FigureColor.NONE ? null : target;
        var after = MoveValidator.afterMove(gameField, from, to, enPassantTarget, promotion);
        CastlingRights nextRights = rightsAfter(figure, from, captured, ep ? new Position(from.getRow(), to.getColumn()) : to);
        String key = positionKey(after, MoveValidator.opposite(figure.getColor()), nextRights, enPassantAfter(figure, from, to));
        if (repetitions.getOrDefault(key, 0) >= 2) return Termination.THREEFOLD_REPETITION;
        return !(figure instanceof Pawn) && captured == null && halfmoveClock >= 99 ? Termination.FIFTY_MOVE_RULE : null;
    }

    private CastlingRights rightsAfter(Figure figure, Position from, Figure captured, Position captureSquare) {
        CastlingRights rights = castlingRights;
        if (figure instanceof King) rights = rights.withoutKing(figure.getColor());
        if (figure instanceof Rook) rights = withoutHomeRook(rights, figure.getColor(), from);
        if (captured instanceof Rook) rights = withoutHomeRook(rights, captured.getColor(), captureSquare);
        return rights;
    }

    private static CastlingRights withoutHomeRook(CastlingRights rights, FigureColor color, Position position) {
        if (position.getRow() == MoveValidator.homeRow(color)) {
            if (position.getColumn() == 0) return rights.without(color, false);
            if (position.getColumn() == 7) return rights.without(color, true);
        }
        return rights;
    }

    private static Position enPassantAfter(Figure figure, Position from, Position to) {
        return figure instanceof Pawn && Math.abs(to.getRow() - from.getRow()) == 2
                ? new Position((from.getRow() + to.getRow()) / 2, from.getColumn()) : null;
    }

    private String positionKey(Map<Position, Figure> board, FigureColor turn, CastlingRights rights, Position ep) {
        StringBuilder key = new StringBuilder();
        for (int row = 0; row < gameField.getSize(); row++) {
            for (int column = 0; column < gameField.getSize(); column++) {
                Figure figure = board.get(new Position(row, column));
                char symbol = figure instanceof Pawn ? 'p' : figure instanceof Knight ? 'n'
                                                             : figure instanceof Bishop ? 'b' : figure instanceof Rook ? 'r'
                                                                                                : figure instanceof Queen ? 'q' : figure instanceof King ? 'k' : '.';
                key.append(figure != null && figure.getColor() == FigureColor.WHITE ? Character.toUpperCase(symbol) : symbol);
            }
        }
        key.append('|').append(turn).append('|').append(rights);
        if (MoveValidator.hasLegalEnPassant(board, turn, ep))
            key.append('|').append(ep.getRow()).append(',').append(ep.getColumn());
        return key.toString();
    }

    private MoveResult commandGuard(Player player) {
        updateTime();
        if (outcome.isFinished()) return TurnRejection.GAME_OVER;
        return player == currentPlayer ? Success.SUCCESS : TurnRejection.WRONG_PLAYER;
    }

    private ActionEngine.Context context() {
        ensurePieces();
        return new ActionEngine.Context(gameField, pieces, castlingRights, enPassantTarget, turnNumber, mechanics, heroRegistry);
    }

    private void publish(ActionEngine.Preview preview) {
        heroRegistry.validateStates(preview.next().pieces());
        gameField = preview.next().field();
        pieces = preview.next().pieces();
        pieceStateField = gameField;
        castlingRights = preview.next().rights();
        enPassantTarget = preview.next().ep();
        mechanics = preview.next().mechanics();
        for (var capture : preview.captures()) {
            if(!capture.rewarded())continue;
            Player credited = capture.creditedSide() == FigureColor.WHITE ? whitePlayer : blackPlayer;
            credited.recordCapture(capture.victim());
        }
    }

    private void advanceTurn() {
        currentPlayer = currentPlayer == whitePlayer ? blackPlayer : whitePlayer;
        clock.switchTo(currentPlayer.color());
        turnNumber++;
        moveUsed = abilityUsed = false;
    }

    private Map<Long, PieceState> prepareNextTurnPieces(Map<Long, PieceState> prepared) {
        FigureColor incoming = currentPlayer == whitePlayer ? blackPlayer.color() : whitePlayer.color();
        var next = new HashMap<>(prepared);
        prepared.values().stream().filter(piece -> piece.color() == incoming && piece.cooldown() > 0
                && !piece.isRemoved()).forEach(piece -> next.put(piece.id(),
                piece.withAbilityState(piece.cooldown() - 1, piece.ultimateCharge(), piece.storedPieceId())));
        heroRegistry.validateStates(next);
        return Map.copyOf(next);
    }

    public static Game mobaHeroesSandbox(int seconds) {
        var base = HeroRules.sandbox();
        Game g = mobaPrototype(new HeroRules(base.pudge(), base.lumeshard(), base.crow(), base.gold(), base.bolotnik(), IzbaRules.sandbox(), GinsengRules.sandbox(), FaunRules.sandbox(), AresRules.sandbox(),ClowngangerRules.sandbox(),MoleminerRules.sandbox(),ZeusRules.sandbox(),ChronomlinRules.sandbox(),KikimoraRules.sandbox()), seconds);
        var sandboxBoard = new HashMap<>(g.gameField.getGameField());
        for (Position p : List.of(new Position(1, 3), new Position(6, 3)))
            sandboxBoard.put(p, g.gameField.createFigure(Bishop.class, p, p.getRow() == 1 ? FigureColor.WHITE : FigureColor.BLACK));
        for(Position p:List.of(new Position(1,2),new Position(6,2)))sandboxBoard.put(p,g.gameField.createFigure(Knight.class,p,p.getRow()==1?FigureColor.WHITE:FigureColor.BLACK));
        sandboxBoard.put(new Position(1,4),g.gameField.createFigure(Rook.class,new Position(1,4),FigureColor.WHITE));
        sandboxBoard.put(new Position(1,6),g.gameField.createFigure(Bishop.class,new Position(1,6),FigureColor.WHITE));
        g.gameField = Field.fromMobaPosition(sandboxBoard);
        var heroes = new HashMap<Position, HeroType>();
        for (PieceState p : g.pieces.values()) if (p.hero() != HeroType.NONE) heroes.put(p.position(), p.hero());
        heroes.put(new Position(1,6),HeroType.KIKIMORA);
        heroes.put(new Position(1,5),HeroType.CHRONOMLIN);
        heroes.put(new Position(1,4),HeroType.ZEUS);
        heroes.put(new Position(1,0),HeroType.MOLEMINER);heroes.put(new Position(6,0),HeroType.MOLEMINER);
        heroes.put(new Position(1,2),HeroType.CLOWNGANGER);heroes.put(new Position(6,2),HeroType.CLOWNGANGER);
        heroes.put(new Position(1, 3), HeroType.ARES);
        heroes.put(new Position(6, 3), HeroType.ARES);
        heroes.put(new Position(0, 1), HeroType.FAUN);
        heroes.put(new Position(7, 1), HeroType.FAUN);
        heroes.put(new Position(0, 5), HeroType.GINSENG);
        heroes.put(new Position(7, 5), HeroType.GINSENG);
        heroes.put(new Position(0, 7), HeroType.IZBA);
        heroes.put(new Position(7, 7), HeroType.IZBA);
        g.rebuildPieces(heroes);
        return g;
    }

    private void rebuildPieces(Map<Position, HeroType> heroes) {
        var setup = new MechanicSession(new HeroActionContext(gameField, pieces, turnNumber, mechanics),
                pieces, heroRules, heroRegistry, currentPlayer.color(), null, List.of());
        setup.assignments(heroes);
        MoveResult initialized = setup.run(MechanicPhase.SETUP);
        if (!initialized.isSuccess()) throw new IllegalArgumentException("Invalid mechanic setup: " + initialized);
        mechanics = setup.states();
        for (var entry : heroes.entrySet()) {
            Figure piece = gameField.getFigureByPosition(entry.getKey());
            if (entry.getValue() == null || piece == null || !heroRegistry.get(entry.getValue()).supports(piece.getClass()))
                throw new IllegalArgumentException("Hero must be assigned to its matching base piece");
        }
        var factory = new HeroPieceFactory(heroRegistry);
        var next = new HashMap<Long, PieceState>();
        for (int row = 0; row < 8; row++)
            for (int column = 0; column < 8; column++) {
                Position square = new Position(row, column);
                Figure figure = gameField.getFigureByPosition(square);
                if (figure.getColor() == FigureColor.NONE) continue;
                PieceState piece = factory.initial(nextPieceId++, figure, heroes.getOrDefault(square, HeroType.NONE));
                next.put(piece.id(), piece);
            }
        pieces = Map.copyOf(next);
        pieceStateField = gameField;
    }

    private void ensurePieces() {
        // Legacy test-only fixtures replace the private Field through reflection.
        // Production boards are always published together with matching piece states.
        if (pieceStateField != gameField) rebuildPieces(Map.of());
    }

    private void addEvent(GameEvent.Kind kind, FigureColor color, Long actor, Long target, Position from, Position to, String message) {
        addEvent(kind, color, actor, target, from, to, message, turnNumber);
    }

    private void addEvent(GameEvent.Kind kind, FigureColor color, Long actor, Long target, Position from, Position to, String message, long actingTurn) {
        events.add(new GameEvent(events.size() + 1L, actingTurn, kind, color, actor, target, from, to, message));
    }

    private record PreparedEvents(MoveResult result, List<GameEvent> events) {}

    private PreparedEvents prepareActionEvents(ActionEngine.Context before, ActionEngine.Preview preview, GameEvent primary) {
        var initial = new ArrayList<>(events);
        initial.add(primary);
        for (var proposed : preview.events()) initial.add(proposed.numbered(initial.size() + 1L));
        var session = new MechanicSession(before.heroContext(), preview.next().pieces(), heroRules,
                heroRegistry, primary.color(), null, preview.captures(), initial);
        session.states(preview.next().mechanics());
        MoveResult result = session.run(MechanicPhase.HISTORY);
        if (!result.isSuccess()) return new PreparedEvents(result, List.of());
        return new PreparedEvents(Success.SUCCESS, session.history().subList(events.size(), session.history().size()));
    }

    public synchronized GameSnapshot getSnapshot() {
        updateTime();
        ensurePieces();
        return new GameSnapshot(version, mode, gameField, pieces, getTurnState(), currentPlayer.color(), castlingRights,
                enPassantTarget, halfmoveClock, repetitions, clock.snapshot(), outcome, moves, events,
                whitePlayer.getPoints(), blackPlayer.getPoints(), whitePlayer.getAttackedFigures(), blackPlayer.getAttackedFigures(),
                drawOffer == null ? FigureColor.NONE : drawOffer.color(), heroRules, draft, mechanics);
    }

    private EconomyState economy() { return mechanics.get(BuiltInStates.ECONOMY); }
    private TerrainState terrain() { return mechanics.get(BuiltInStates.TERRAIN); }
    private SummonState summons() { return mechanics.get(BuiltInStates.SUMMONS); }
    private SpellState spells() { return mechanics.get(BuiltInStates.SPELLS); }
    private FaunState faun() { return mechanics.get(BuiltInStates.FAUN); }

    public synchronized TurnState getTurnState() {
        return new TurnState(turnNumber, moveUsed, abilityUsed);
    }

    public synchronized PieceState getPieceState(Position square) {
        return context().at(square);
    }

    public synchronized PieceState getPieceState(long id) {
        ensurePieces();
        return pieces.get(id);
    }

    public synchronized List<GameEvent> getEvents() {
        return List.copyOf(events);
    }

    public GameMode getMode() {
        return mode;
    }

    public PudgeRules getPudgeRules() {
        return heroRules.pudge();
    }

    public synchronized EconomyState getEconomy() {
        return economy();
    }

    public synchronized SummonState getSummons() {
        return summons();
    }

    public synchronized DraftSnapshot getDraft() {
        return draft;
    }

    public synchronized TerrainState getTerrain() {
        return terrain();
    }

    public BolotnikRules getBolotnikRules() {
        return heroRules.bolotnik();
    }

    public BountyCrowRules getCrowRules() {
        return heroRules.crow();
    }

    public HeroRules getHeroRules() {
        return heroRules;
    }

    /** Неизменяемый реестр стратегий этой партии; представление не создаёт второй набор. */
    public HeroRegistry getHeroRegistry() {
        return heroRegistry;
    }

    public LumeshardRules getLumeshardRules() {
        return heroRules.lumeshard();
    }

    private int currentRepetitionCount() {
        return repetitions.getOrDefault(positionKey(gameField.getGameField(), currentPlayer.color(), castlingRights, enPassantTarget), 0);
    }

    private boolean isParticipant(Player player) {
        return player == whitePlayer || player == blackPlayer;
    }

    private static Position copy(Position position) {
        return position == null ? null : new Position(position.getRow(), position.getColumn());
    }

    public synchronized Figure getFigureByPosition(Position position) {
        return gameField.getFigureByPosition(position);
    }

    public synchronized Field getGameField() {
        return gameField;
    }

    public Player getWhitePlayer() {
        return whitePlayer;
    }

    public Player getBlackPlayer() {
        return blackPlayer;
    }

    public synchronized Player getCurrentPlayer() {
        return currentPlayer;
    }

    public synchronized GameOutcome getOutcome() {
        updateTime();
        return outcome;
    }

    public synchronized ClockSnapshot getClockSnapshot() {
        updateTime();
        return clock.snapshot();
    }

    public synchronized Player getDrawOffer() {
        return drawOffer;
    }

    public synchronized CastlingRights getCastlingRights() {
        return castlingRights;
    }

    public synchronized Position getEnPassantTarget() {
        return copy(enPassantTarget);
    }

    public synchronized int getHalfmoveClock() {
        return halfmoveClock;
    }

    public synchronized int getRepetitionCount() {
        return currentRepetitionCount();
    }

    public synchronized String getPositionKey() {
        return positionKey(gameField.getGameField(), currentPlayer.color(), castlingRights, enPassantTarget);
    }

    public synchronized List<MadeMove> getMoves() {
        return List.copyOf(moves);
    }

    public synchronized boolean isInCheck() {
        ensurePieces();
        return mode == GameMode.MOBA ? AttackRules.isInCheck(gameField.getGameField(), pieces, currentPlayer.color(), terrain())
                : MoveValidator.isInCheck(gameField.getGameField(), currentPlayer.color());
    }

    public synchronized String getLastMoveAsString() {
        return moves.isEmpty() ? "" : moves.getLast().toString();
    }
}
