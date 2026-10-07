package game;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.*;
import rules.standard.MaterialRules;
import rules.standard.MoveValidator;
import rules.standard.PositionValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

public final class Game {
    private Field gameField;
    private final GameMode mode;
    private final PudgeRules pudgeRules;
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

    /** Untimed game. The UI starts the clock when the user starts playing. */
    public Game(int n) { this(n, System::nanoTime); }
    public Game(int n, LongSupplier nanoTime) { this(n, GameMode.CLASSIC, nanoTime, new PudgeRules(0, 0, true, false)); }
    private Game(int n, GameMode mode, LongSupplier nanoTime, PudgeRules rules) {
        gameField = new Field(n);
        this.mode = java.util.Objects.requireNonNull(mode);
        this.pudgeRules = java.util.Objects.requireNonNull(rules);
        this.nanoTime = java.util.Objects.requireNonNull(nanoTime);
        rebuildPieces(Map.of());
        repetitions.put(positionKey(gameField.getGameField(), currentPlayer.color(), castlingRights, null), 1);
    }

    /** Fixed first prototype: one Pudge on a1/a8 for each side, all other pieces standard. */
    public static Game mobaPrototype(PudgeRules rules, int seconds) {
        Game game = new Game(8, GameMode.MOBA, System::nanoTime, rules);
        game.rebuildPieces(Map.of(new Position(0, 0), HeroType.PUDGE_ROOK, new Position(7, 0), HeroType.PUDGE_ROOK));
        game.startClock(seconds);
        return game;
    }

    public static Game fromMobaPosition(GamePosition position, Map<Position, HeroType> heroes, PudgeRules rules) {
        return fromMobaPosition(position, heroes, rules, System::nanoTime);
    }
    public static Game fromMobaPosition(GamePosition position, Map<Position, HeroType> heroes, PudgeRules rules, LongSupplier time) {
        if (position == null || heroes == null) throw new IllegalArgumentException("Position and hero assignments are required");
        Field field = Field.fromMobaPosition(position.board());
        PositionValidator.validateState(field, position.sideToMove(), position.castlingRights(), position.enPassantTarget(), position.halfmoveClock());
        Game game = new Game(8, GameMode.MOBA, time, rules);
        game.gameField = field;
        game.currentPlayer = position.sideToMove() == FigureColor.WHITE ? game.whitePlayer : game.blackPlayer;
        game.castlingRights = position.castlingRights(); game.enPassantTarget = position.enPassantTarget();
        game.halfmoveClock = position.halfmoveClock(); game.rebuildPieces(new HashMap<>(heroes));
        game.repetitions.clear(); game.repetitions.put(game.getPositionKey(), 1);
        game.evaluateOutcome();
        return game;
    }
    public Game(int n, int seconds) { this(n); startClock(seconds); }

    /** Starts a new untimed game from a validated position, with empty history. */
    public static Game fromPosition(GamePosition position) { return fromPosition(position, System::nanoTime); }
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

    /** One coherent position snapshot. Histories and clocks are not a saved-game format. */
    public synchronized GamePosition getPosition() {
        return new GamePosition(gameField.getGameField(), currentPlayer.color(), castlingRights, enPassantTarget, halfmoveClock);
    }

    public synchronized void startClock(int seconds) {
        if (outcome.isFinished() || !moves.isEmpty() || !events.isEmpty()) throw new IllegalStateException("Configure the clock before playing");
        clock.start(seconds, currentPlayer.color(), nanoTime.getAsLong());
        version++;
    }

    public synchronized MoveResult moveFigure(Player player, Position from, Position to) {
        return moveFigure(player, from, to, null);
    }

    /** Rejections preserve chess state; elapsed real time is still charged. */
    public synchronized MoveResult moveFigure(Player player, Position from, Position to, Promotion promotion) {
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode == GameMode.MOBA && moveUsed) return MoveResult.MOVE_ALREADY_USED;
        ensurePieces();
        ActionEngine.Context before = context();
        from = copy(from); to = copy(to);
        ActionEngine.Preview preview = ActionEngine.move(before, player.color(), from, to, promotion, mode, pudgeRules);
        if (!preview.result().isSuccess()) return preview.result();
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        Figure figure = gameField.getFigureByPosition(from);
        PieceState actor = before.at(from);
        long actingTurn = turnNumber;
        boolean castle = MoveValidator.isCastling(figure, from, to);
        boolean ep = MoveValidator.isEnPassant(gameField.getGameField(), player.color(), from, to, enPassantTarget);
        ActionEngine.Capture firstCapture = preview.captures().isEmpty() ? null : preview.captures().getFirst();
        if (moves.isEmpty() && events.isEmpty()) {
            repetitions.clear(); repetitions.put(getPositionKey(), 1);
        }
        publish(preview);
        halfmoveClock = figure instanceof Pawn || firstCapture != null ? 0 : halfmoveClock + 1;
        if (drawOffer != null && drawOffer != player) drawOffer = null;
        if (mode == GameMode.CLASSIC) {
            advanceTurn(); repetitions.merge(getPositionKey(), 1, Integer::sum); evaluateOutcome();
        } else moveUsed = true;
        MoveKind kind = castle ? MoveKind.CASTLING : ep ? MoveKind.EN_PASSANT
                : promotion != null ? MoveKind.PROMOTION : firstCapture != null ? MoveKind.CAPTURE : MoveKind.NORMAL;
        String name = actor.hero() == HeroType.NONE ? figure.getName() : actor.hero().getName();
        MadeMove made = new MadeMove(name, player.color(), from, to, kind, promotion,
                firstCapture == null ? null : firstCapture.victim().getName(), firstCapture == null ? null : firstCapture.square(),
                moves.size() + 1, clock.snapshot());
        player.recordMove(made); moves.add(made);
        addEvent(GameEvent.Kind.MOVE, player.color(), actor.id(), firstCapture == null ? null : firstCapture.victimId(), from, to, made.toString(), actingTurn);
        addReactions(preview);
        version++;
        return MoveResult.SUCCESS;
    }

    public synchronized MoveResult useAbility(Player player, Position actorSquare, Position target, Ability ability) {
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode != GameMode.MOBA) return MoveResult.UNSUPPORTED_ACTION;
        if (abilityUsed) return MoveResult.ABILITY_ALREADY_USED;
        ActionEngine.Context before = context();
        ActionEngine.Preview preview = ActionEngine.ability(before, player.color(), actorSquare, target, ability, pudgeRules);
        if (!preview.result().isSuccess()) return preview.result();
        if (!moveUsed && !ActionEngine.hasMove(preview.next(), player.color(), mode, pudgeRules))
            return MoveResult.NO_MOVE_AFTER_ABILITY;
        updateTime(); if (outcome.isFinished()) return MoveResult.GAME_OVER;
        PieceState actor = before.at(actorSquare), victim = before.at(target);
        publish(preview); abilityUsed = true;
        if (drawOffer != null && drawOffer != player) drawOffer = null;
        String message = ability == Ability.HOOK ? "Пудж: хук " + cell(target) + " → " + cell(pieces.get(victim.id()).position())
                : "Пудж: проглотил " + victim.type().getSimpleName() + " на " + cell(target);
        addEvent(ability == Ability.HOOK ? GameEvent.Kind.HOOK : GameEvent.Kind.SWALLOW, player.color(), actor.id(), victim.id(), actorSquare, target, message);
        version++;
        return MoveResult.SUCCESS;
    }

    public synchronized MoveResult endTurn(Player player) {
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode != GameMode.MOBA) return MoveResult.UNSUPPORTED_ACTION;
        if (!moveUsed) return MoveResult.MOVE_REQUIRED;
        if (MoveValidator.isInCheck(gameField.getGameField(), player.color())) return MoveResult.KING_IN_CHECK;
        addEvent(GameEvent.Kind.END_TURN, player.color(), null, null, null, null, "Ход завершён");
        if (drawOffer != null && drawOffer != player) drawOffer = null;
        advanceTurn(); repetitions.merge(getPositionKey(), 1, Integer::sum); evaluateOutcome(); version++;
        return MoveResult.SUCCESS;
    }

    /** Independent from move/skill budgets. Actual item effects await a designed catalog. */
    public synchronized MoveResult useItem(Player player, long pieceId, int slot) {
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode != GameMode.MOBA) return MoveResult.UNSUPPORTED_ACTION;
        PieceState piece = pieces.get(pieceId);
        if (piece == null || piece.status() != PieceState.Status.BOARD) return MoveResult.EMPTY_SOURCE;
        if (piece.color() != player.color()) return MoveResult.WRONG_FIGURE_COLOR;
        if (slot < 0 || slot >= 3) return MoveResult.INVALID_ITEM_SLOT;
        return piece.inventory().get(slot).isEmpty() ? MoveResult.EMPTY_ITEM_SLOT : MoveResult.ITEM_NOT_AVAILABLE;
    }

    public synchronized List<Position> getAbilityTargets(Position actorSquare, Ability ability) {
        updateTime();
        if (mode != GameMode.MOBA || outcome.isFinished() || abilityUsed) return List.of();
        var before = context(); var targets = new ArrayList<Position>();
        for (Position target : gameField.getGameField().keySet()) {
            var preview = ActionEngine.ability(before, currentPlayer.color(), actorSquare, target, ability, pudgeRules);
            if (preview.result().isSuccess() && (moveUsed || ActionEngine.hasMove(preview.next(), currentPlayer.color(), mode, pudgeRules)))
                targets.add(target);
        }
        return List.copyOf(targets);
    }

    public synchronized MoveResult getAbilityAvailability(Position actorSquare, Ability ability) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        if (mode != GameMode.MOBA) return MoveResult.UNSUPPORTED_ACTION;
        if (abilityUsed) return MoveResult.ABILITY_ALREADY_USED;
        return ActionEngine.abilityReady(context(), currentPlayer.color(), actorSquare, ability, pudgeRules);
    }

    public synchronized AvailableMoves getLegalMoves(Position from) {
        updateTime();
        if (outcome.isFinished() || (mode == GameMode.MOBA && moveUsed)) return new AvailableMoves(List.of(), List.of());
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
        if (mode == GameMode.MOBA) return ActionEngine.move(context(), color, from, to, promotion, mode, pudgeRules).result();
        return MoveValidator.validate(gameField, color, from, to, castlingRights, enPassantTarget, promotion);
    }

    private boolean hasLegalMove(FigureColor color) {
        for (var entry : gameField.getGameField().entrySet()) {
            Figure figure = entry.getValue();
            if (figure.getColor() != color) continue;
            AvailableMoves candidates = MoveValidator.candidates(gameField, figure, castlingRights, enPassantTarget);
            for (Position to : candidates.getCanMove()) if (legalTarget(figure, entry.getKey(), to, color)) return true;
            for (Position to : candidates.getCanAttack()) if (legalTarget(figure, entry.getKey(), to, color)) return true;
        }
        return false;
    }

    private void evaluateOutcome() {
        FigureColor color = currentPlayer.color();
        var board = gameField.getGameField();
        if (mode == GameMode.MOBA) {
            if (!ActionEngine.hasTurn(context(), color, pudgeRules)) {
                boolean check = MoveValidator.isInCheck(board, color);
                finish(check ? Termination.CHECKMATE : Termination.STALEMATE, check ? MoveValidator.opposite(color) : FigureColor.NONE);
            }
            return;
        }
        if (!hasLegalMove(color)) {
            boolean check = MoveValidator.isInCheck(board, color);
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
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        if (!isParticipant(player)) return MoveResult.WRONG_PLAYER;
        FigureColor winner = MoveValidator.opposite(player.color());
        boolean possible = mode == GameMode.MOBA || MaterialRules.canPossiblyMate(gameField.getGameField(), winner);
        finish(possible ? Termination.RESIGNATION : Termination.RESIGNATION_DRAW, possible ? winner : FigureColor.NONE);
        return MoveResult.SUCCESS;
    }

    public synchronized MoveResult offerDraw(Player player) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        if (!isParticipant(player)) return MoveResult.WRONG_PLAYER;
        if (drawOffer != null) return MoveResult.DRAW_OFFER_PENDING;
        drawOffer = player;
        version++;
        return MoveResult.SUCCESS;
    }

    public synchronized MoveResult acceptDraw(Player player) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        if (!isParticipant(player)) return MoveResult.WRONG_PLAYER;
        if (drawOffer == null || drawOffer == player) return MoveResult.NO_DRAW_OFFER;
        if (whitePlayer.getMadeMoves().isEmpty() || blackPlayer.getMadeMoves().isEmpty()) return MoveResult.DRAW_AGREEMENT_TOO_EARLY;
        finish(Termination.DRAW_AGREEMENT, FigureColor.NONE);
        return MoveResult.SUCCESS;
    }

    public synchronized MoveResult declineDraw(Player player) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        if (!isParticipant(player)) return MoveResult.WRONG_PLAYER;
        if (drawOffer == null || drawOffer == player) return MoveResult.NO_DRAW_OFFER;
        drawOffer = null;
        version++;
        return MoveResult.SUCCESS;
    }

    public synchronized boolean canClaimDraw(Player player) {
        updateTime();
        return !outcome.isFinished() && player == currentPlayer && currentClaimReason() != null;
    }

    public synchronized MoveResult claimDraw(Player player) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        if (player != currentPlayer) return MoveResult.WRONG_PLAYER;
        Termination reason = currentClaimReason();
        if (reason == null) return MoveResult.INVALID_DRAW_CLAIM;
        finish(reason, FigureColor.NONE);
        return MoveResult.SUCCESS;
    }

    /** Declares an intended legal move; a successful claim ends the game without applying it. */
    public synchronized MoveResult claimDraw(Player player, Position from, Position to, Promotion promotion) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        if (player != currentPlayer) return MoveResult.WRONG_PLAYER;
        from = copy(from);
        to = copy(to);
        MoveResult validation = validate(player.color(), from, to, promotion);
        if (!validation.isSuccess()) return validation;
        Termination reason = prospectiveClaimReason(from, to, promotion);
        if (reason == null) return MoveResult.INVALID_DRAW_CLAIM;
        finish(reason, FigureColor.NONE);
        return MoveResult.SUCCESS;
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
        if (MoveValidator.hasLegalEnPassant(board, turn, ep)) key.append('|').append(ep.getRow()).append(',').append(ep.getColumn());
        return key.toString();
    }

    private MoveResult commandGuard(Player player) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        return player == currentPlayer ? MoveResult.SUCCESS : MoveResult.WRONG_PLAYER;
    }
    private ActionEngine.Context context() {
        ensurePieces();
        return new ActionEngine.Context(gameField, pieces, castlingRights, enPassantTarget);
    }
    private void publish(ActionEngine.Preview preview) {
        gameField = preview.next().field(); pieces = preview.next().pieces(); pieceStateField = gameField;
        castlingRights = preview.next().rights(); enPassantTarget = preview.next().ep();
        for (var capture : preview.captures()) {
            Player credited = capture.creditedSide() == FigureColor.WHITE ? whitePlayer : blackPlayer;
            credited.recordCapture(capture.victim());
        }
    }
    private void advanceTurn() {
        currentPlayer = currentPlayer == whitePlayer ? blackPlayer : whitePlayer;
        clock.switchTo(currentPlayer.color()); turnNumber++; moveUsed = abilityUsed = false;
        if (mode == GameMode.MOBA) {
            var next = new HashMap<>(pieces);
            pieces.values().stream().filter(piece -> piece.color() == currentPlayer.color() && piece.cooldown() > 0
                    && piece.status() != PieceState.Status.CAPTURED).forEach(piece -> next.put(piece.id(),
                    piece.withAbilityState(piece.cooldown() - 1, piece.ultimateCharge(), piece.storedPieceId())));
            pieces = Map.copyOf(next);
        }
    }
    private void rebuildPieces(Map<Position, HeroType> heroes) {
        for (var entry : heroes.entrySet()) {
            Figure piece = gameField.getFigureByPosition(entry.getKey());
            if (entry.getValue() == null || !(piece instanceof Rook) || entry.getValue() != HeroType.PUDGE_ROOK)
                throw new IllegalArgumentException("Pudge must be assigned to a rook square");
        }
        var next = new HashMap<Long, PieceState>();
        for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++) {
            Position square = new Position(row, column); Figure figure = gameField.getFigureByPosition(square);
            if (figure.getColor() == FigureColor.NONE) continue;
            PieceState piece = PieceState.initial(nextPieceId++, figure, heroes.getOrDefault(square, HeroType.NONE));
            next.put(piece.id(), piece);
        }
        pieces = Map.copyOf(next); pieceStateField = gameField;
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
    private void addReactions(ActionEngine.Preview preview) {
        for (var capture : preview.captures()) if (capture.reaction())
            addEvent(GameEvent.Kind.COUNTER_CAPTURE, capture.creditedSide(), capture.actorId(), capture.victimId(),
                    capture.square(), capture.square(), "Союзник из Пуджа взял " + capture.victim().getName() + " на " + cell(capture.square()));
    }
    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
    public synchronized GameSnapshot getSnapshot() {
        updateTime(); ensurePieces();
        return new GameSnapshot(version, mode, gameField, pieces, getTurnState(), currentPlayer.color(), castlingRights,
                enPassantTarget, halfmoveClock, repetitions, clock.snapshot(), outcome, moves, events,
                whitePlayer.getPoints(), blackPlayer.getPoints(), whitePlayer.getAttackedFigures(), blackPlayer.getAttackedFigures(),
                drawOffer == null ? FigureColor.NONE : drawOffer.color(), pudgeRules);
    }
    public synchronized TurnState getTurnState() { return new TurnState(turnNumber, moveUsed, abilityUsed); }
    public synchronized PieceState getPieceState(Position square) { return context().at(square); }
    public synchronized PieceState getPieceState(long id) { ensurePieces(); return pieces.get(id); }
    public synchronized List<GameEvent> getEvents() { return List.copyOf(events); }
    public GameMode getMode() { return mode; }
    public PudgeRules getPudgeRules() { return pudgeRules; }

    private int currentRepetitionCount() { return repetitions.getOrDefault(positionKey(gameField.getGameField(), currentPlayer.color(), castlingRights, enPassantTarget), 0); }
    private boolean isParticipant(Player player) { return player == whitePlayer || player == blackPlayer; }
    private static Position copy(Position position) { return position == null ? null : new Position(position.getRow(), position.getColumn()); }

    public synchronized Figure getFigureByPosition(Position position) { return gameField.getFigureByPosition(position); }
    public synchronized Field getGameField() { return gameField; }
    public Player getWhitePlayer() { return whitePlayer; }
    public Player getBlackPlayer() { return blackPlayer; }
    public synchronized Player getCurrentPlayer() { return currentPlayer; }
    public synchronized GameOutcome getOutcome() { updateTime(); return outcome; }
    public synchronized ClockSnapshot getClockSnapshot() { updateTime(); return clock.snapshot(); }
    public synchronized Player getDrawOffer() { return drawOffer; }
    public synchronized CastlingRights getCastlingRights() { return castlingRights; }
    public synchronized Position getEnPassantTarget() { return copy(enPassantTarget); }
    public synchronized int getHalfmoveClock() { return halfmoveClock; }
    public synchronized int getRepetitionCount() { return currentRepetitionCount(); }
    public synchronized String getPositionKey() { return positionKey(gameField.getGameField(), currentPlayer.color(), castlingRights, enPassantTarget); }
    public synchronized List<MadeMove> getMoves() { return List.copyOf(moves); }
    public synchronized boolean isInCheck() { return MoveValidator.isInCheck(gameField.getGameField(), currentPlayer.color()); }
    public synchronized String getLastMoveAsString() { return moves.isEmpty() ? "" : moves.getLast().toString(); }
}
