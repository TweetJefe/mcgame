package game.core;

import game.heroes.Ability;
import game.heroes.AbilityRequest;
import game.heroes.ginseng.*;
import game.heroes.faun.*;
import game.heroes.ares.AresRules;
import game.summoning.*;
import game.draft.Draft;
import game.draft.DraftSnapshot;
import game.economy.*;
import game.terrain.TerrainState;
import game.heroes.bolotnik.BolotnikRules;
import game.heroes.bountycrow.BountyCrowRules;
import game.heroes.HeroType;
import game.heroes.HeroRules;
import game.heroes.lumeshard.LumeshardRules;
import rules.moba.AttackRules;
import game.heroes.pudge.PudgeRules;
import game.history.GameEvent;
import game.history.MadeMove;
import game.moves.AvailableMoves;
import game.moves.Capture;
import game.moves.MoveKind;
import game.moves.MoveResult;
import game.moves.Promotion;
import game.state.CastlingRights;
import game.state.ClockSnapshot;
import game.state.GameMode;
import game.state.GameOutcome;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.state.Termination;
import game.state.TurnState;

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
    private final HeroRules heroRules;
    private EconomyState economy = EconomyState.disabled();
    private TerrainState terrain = TerrainState.empty();
    private DraftSnapshot draft;
    private SummonState summons = SummonState.empty();
    private SpellState spells = SpellState.empty();
    private FaunState faun = FaunState.empty();
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
        this.mode = java.util.Objects.requireNonNull(mode);
        if (mode == GameMode.MOBA) economy = EconomyState.initial();
        this.heroRules = java.util.Objects.requireNonNull(rules);
        this.nanoTime = java.util.Objects.requireNonNull(nanoTime);
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
        game.rebuildPieces(Map.of(new Position(0, 0), HeroType.PUDGE_ROOK, new Position(7, 0), HeroType.PUDGE_ROOK,
                new Position(0, 2), HeroType.LUMESHARD_BISHOP, new Position(7, 2), HeroType.LUMESHARD_BISHOP,
                new Position(1, 1), HeroType.BOUNTY_CROW_PAWN, new Position(6, 1), HeroType.BOUNTY_CROW_PAWN,
                new Position(0, 6), HeroType.BOLOTNIK_KNIGHT, new Position(7, 6), HeroType.BOLOTNIK_KNIGHT));
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
        DraftSnapshot verified = Draft.fromSnapshot(draft).getSnapshot();
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
        game.economy = saved.economy();
        game.terrain = saved.terrain();
        game.draft = saved.draft();
        game.summons = saved.summons();
        game.spells = saved.spells();
        game.faun = saved.faun();
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
        if (mode == GameMode.MOBA && moveUsed) return MoveResult.MOVE_ALREADY_USED;
        ensurePieces();
        ActionEngine.Context before = context();
        from = copy(from);
        to = copy(to);
        ActionEngine.Preview preview = ActionEngine.move(before, player.color(), from, to, promotion, mode, heroRules);
        if (!preview.result().isSuccess()) return preview.result();
        if (mode == GameMode.MOBA && !ActionEngine.canFinishAfterMove(preview.next(), player.color(), !abilityUsed, heroRules))
            return MoveResult.NO_TURN_CONTINUATION;
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        Figure figure = gameField.getFigureByPosition(from);
        PieceState actor = before.at(from);
        long actingTurn = turnNumber;
        boolean castle = MoveValidator.isCastling(figure, from, to);
        boolean ep = MoveValidator.isEnPassant(gameField.getGameField(), player.color(), from, to, enPassantTarget);
        Capture firstCapture = preview.captures().stream().filter(c -> c.cause() == Capture.Cause.MOVE && c.actorId() == actor.id()).findFirst().orElse(null);
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
        MoveKind kind = castle ? MoveKind.CASTLING : ep ? MoveKind.EN_PASSANT
                                                     : promotion != null ? MoveKind.PROMOTION : firstCapture != null ? MoveKind.CAPTURE : MoveKind.NORMAL;
        String name = actor.hero() == HeroType.NONE ? figure.getName() : actor.hero().getName();
        MadeMove made = new MadeMove(name, player.color(), from, to, kind, promotion,
                firstCapture == null ? null : firstCapture.victim().getName(), firstCapture == null ? null : firstCapture.square(),
                moves.size() + 1, clock.snapshot());
        player.recordMove(made);
        moves.add(made);
        addEvent(GameEvent.Kind.MOVE, player.color(), actor.id(), firstCapture == null ? null : firstCapture.victimId(), from, to, made.toString(), actingTurn);
        addActionEffects(before, preview);
        version++;
        return MoveResult.SUCCESS;
    }

    public synchronized MoveResult useAbility(Player player, Position actorSquare, Position target, Ability ability) {
        return useAbility(player, new AbilityRequest(actorSquare, ability, target));
    }

    public synchronized MoveResult useAbility(Player player, AbilityRequest request) {
        if (request == null) return MoveResult.INVALID_ABILITY;
        Position actorSquare = request.actor(), target = request.target();
        Ability ability = request.ability();
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode != GameMode.MOBA) return MoveResult.UNSUPPORTED_ACTION;
        if (abilityUsed) return MoveResult.ABILITY_ALREADY_USED;
        ActionEngine.Context before = context();
        ActionEngine.Preview preview = ActionEngine.ability(before, player.color(), request, heroRules);
        if (!preview.result().isSuccess()) return preview.result();
        if (!moveUsed && !ActionEngine.hasMove(preview.next(), player.color(), mode, heroRules))
            return MoveResult.NO_MOVE_AFTER_ABILITY;
        if (moveUsed && !ActionEngine.canEnd(preview.next(), player.color())) return MoveResult.NO_TURN_CONTINUATION;
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        PieceState actor = before.at(actorSquare), victim = before.at(target);
        publish(preview);
        abilityUsed = true;
        if (!preview.captures().isEmpty()) halfmoveClock = 0;
        if (drawOffer != null && drawOffer != player) drawOffer = null;
        String message = switch (ability) {
            case SPEAR -> "Ares: копьё в направлении " + cell(target);
            case CLOUD -> "Faun: туча с центром " + cell(target);
            case INVISIBILITY -> "Faun: стал невидимым";
            case COPY_SPELL ->
                    "Ginseng: скопировал " + before.spells().lastUsed().get(victim.id()).getDisplayName() + " у " + victim.hero().getName();
            case DISPLACE -> "Ginseng: сдвиг " + cell(target) + " → " + cell(request.destination());
            case SUMMON_YAGA -> "Изба: призвала Бабу-ягу";
            case HOOK -> "Пудж: хук " + cell(target) + " → " + cell(pieces.get(victim.id()).position());
            case SWALLOW -> "Пудж: проглотил " + victim.type().getSimpleName() + " на " + cell(target);
            case LASER -> "Lumeshard: ослепил " + victim.type().getSimpleName() + " на " + cell(target);
            case TELEPORT -> "Lumeshard: телепорт " + cell(actorSquare) + " → " + cell(target);
            case BOUNTY ->
                    "Bounty Crow: награда +1,5 золота за " + victim.type().getSimpleName() + " на " + cell(target);
            case MUD -> "Bolotnik: грязь на " + cell(target);
            case AMBUSH -> "Bolotnik: спрятался в болоте у " + cell(actorSquare);
            case STEAL ->
                    "Bounty Crow: украл " + GoldRules.format(economy.history().getLast().totalHalves()) + " золота";
        };
        if (request.borrowed()) message = "Ginseng, копия: " + message;
        Position eventTo = ability == Ability.DISPLACE ? request.destination() : (ability == Ability.STEAL || ability == Ability.AMBUSH || ability == Ability.SUMMON_YAGA || ability == Ability.INVISIBILITY) ? actorSquare : target;
        events.add(new GameEvent(events.size() + 1L, turnNumber, GameEvent.Kind.valueOf(ability.name()), player.color(), actor.id(), victim == null || ability == Ability.CLOUD || ability == Ability.SPEAR ? null : victim.id(), actorSquare, eventTo, message,
                ability == Ability.COPY_SPELL ? before.spells().lastUsed().get(victim.id()) : null, request.borrowed()));
        addActionEffects(before, preview);
        version++;
        return MoveResult.SUCCESS;
    }

    public synchronized MoveResult endTurn(Player player) {
        MoveResult guard = commandGuard(player);
        if (!guard.isSuccess()) return guard;
        if (mode != GameMode.MOBA) return MoveResult.UNSUPPORTED_ACTION;
        if (!moveUsed) return MoveResult.MOVE_REQUIRED;
        if (!ActionEngine.canEnd(context(), player.color())) return MoveResult.KING_IN_CHECK;
        var before = context();
        var matured = SummonResolver.mature(pieces, summons, turnNumber, player.color(), terrain);
        pieces = matured.pieces();
        summons = matured.summons();
        var board = new HashMap<Position, Figure>();
        for (Position p : gameField.getGameField().keySet())
            board.put(p, gameField.createFigure(Empty.class, p, FigureColor.NONE));
        for (PieceState p : pieces.values())
            if (p.position() != null)
                board.put(p.position(), gameField.createFigure(p.type(), p.position(), p.color()));
        gameField = Field.fromMobaPosition(board);
        pieceStateField = gameField;
        addSummonChanges(before, context());
        terrain = terrain.expire(turnNumber);
        faun = faun.expire(turnNumber);
        pieces = AttackRules.expireStuns(pieces, turnNumber);
        expireBlindness(player.color());
        economy = economy.expire(turnNumber);
        addEvent(GameEvent.Kind.END_TURN, player.color(), null, null, null, null, "Ход завершён");
        if (drawOffer != null && drawOffer != player) drawOffer = null;
        advanceTurn();
        repetitions.merge(getPositionKey(), 1, Integer::sum);
        evaluateOutcome();
        version++;
        return MoveResult.SUCCESS;
    }

    /**
     * Independent from move/skill budgets. Actual item effects await a designed catalog.
     */
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
        return getAbilityTargets(actorSquare, ability, false);
    }

    public synchronized List<Position> getAbilityTargets(Position actorSquare, Ability ability, boolean borrowed) {
        updateTime();
        if (mode != GameMode.MOBA || outcome.isFinished() || abilityUsed) return List.of();
        var before = context();
        var targets = new java.util.LinkedHashSet<Position>();
        for (var request : ActionEngine.requests(before, currentPlayer.color(), actorSquare, ability, borrowed, heroRules)) {
            if (request.target() == null || targets.contains(request.target())) continue;
            var preview = ActionEngine.ability(before, currentPlayer.color(), request, heroRules);
            if (canCompleteAbility(preview)) targets.add(request.target());
        }
        return List.copyOf(targets);
    }

    public synchronized List<Position> getDisplacementDestinations(Position actor, Position target, boolean borrowed) {
        updateTime();
        if (mode != GameMode.MOBA || outcome.isFinished() || abilityUsed) return List.of();
        var before = context();
        var result = new ArrayList<Position>();
        for (var request : ActionEngine.requests(before, currentPlayer.color(), actor, Ability.DISPLACE, borrowed, heroRules))
            if (java.util.Objects.equals(request.target(), target)) {
                if (canCompleteAbility(ActionEngine.ability(before, currentPlayer.color(), request, heroRules)))
                    result.add(request.destination());
            }
        return List.copyOf(result);
    }

    private boolean canCompleteAbility(ActionEngine.Preview preview) {
        return preview.result().isSuccess() && (moveUsed ? ActionEngine.canEnd(preview.next(), currentPlayer.color()) : ActionEngine.hasMove(preview.next(), currentPlayer.color(), mode, heroRules));
    }

    public synchronized MoveResult getAbilityAvailability(Position actorSquare, Ability ability) {
        return getAbilityAvailability(actorSquare, ability, false);
    }

    public synchronized MoveResult getAbilityAvailability(Position actorSquare, Ability ability, boolean borrowed) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        if (mode != GameMode.MOBA) return MoveResult.UNSUPPORTED_ACTION;
        if (abilityUsed) return MoveResult.ABILITY_ALREADY_USED;
        return ActionEngine.abilityReady(context(), currentPlayer.color(), actorSquare, ability, heroRules, borrowed);
    }

    public synchronized SpellState getSpells() {
        return spells;
    }

    public synchronized FaunState getFaun() {
        return faun;
    }

    public synchronized AvailableMoves getLegalMoves(Position from) {
        updateTime();
        if (outcome.isFinished() || (mode == GameMode.MOBA && moveUsed))
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
                    ? MoveResult.NO_TURN_CONTINUATION : preview.result();
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
                boolean check = mode == GameMode.MOBA ? AttackRules.isInCheck(board, pieces, color, terrain) : MoveValidator.isInCheck(board, color);
                finish(check ? Termination.CHECKMATE : Termination.STALEMATE, check ? MoveValidator.opposite(color) : FigureColor.NONE);
            }
            return;
        }
        if (!hasLegalMove(color)) {
            boolean check = mode == GameMode.MOBA ? AttackRules.isInCheck(board, pieces, color, terrain) : MoveValidator.isInCheck(board, color);
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
        if (whitePlayer.getMadeMoves().isEmpty() || blackPlayer.getMadeMoves().isEmpty())
            return MoveResult.DRAW_AGREEMENT_TOO_EARLY;
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

    /**
     * Declares an intended legal move; a successful claim ends the game without applying it.
     */
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
        if (MoveValidator.hasLegalEnPassant(board, turn, ep))
            key.append('|').append(ep.getRow()).append(',').append(ep.getColumn());
        return key.toString();
    }

    private MoveResult commandGuard(Player player) {
        updateTime();
        if (outcome.isFinished()) return MoveResult.GAME_OVER;
        return player == currentPlayer ? MoveResult.SUCCESS : MoveResult.WRONG_PLAYER;
    }

    private ActionEngine.Context context() {
        ensurePieces();
        return new ActionEngine.Context(gameField, pieces, castlingRights, enPassantTarget, economy, turnNumber, terrain, summons, spells, faun);
    }

    private void publish(ActionEngine.Preview preview) {
        gameField = preview.next().field();
        pieces = preview.next().pieces();
        pieceStateField = gameField;
        castlingRights = preview.next().rights();
        enPassantTarget = preview.next().ep();
        economy = preview.next().economy();
        terrain = preview.next().terrain();
        summons = preview.next().summons();
        spells = preview.next().spells();
        faun = preview.next().faun();
        for (var capture : preview.captures()) {
            Player credited = capture.creditedSide() == FigureColor.WHITE ? whitePlayer : blackPlayer;
            credited.recordCapture(capture.victim());
        }
    }

    private void expireBlindness(FigureColor owner) {
        var next = new HashMap<>(pieces);
        for (PieceState piece : pieces.values())
            if (piece.color() == owner && !piece.isRemoved() && piece.isBlinded())
                next.put(piece.id(), piece.withBlindness(piece.blindTurns() - 1));
        pieces = Map.copyOf(next);
    }

    private void advanceTurn() {
        currentPlayer = currentPlayer == whitePlayer ? blackPlayer : whitePlayer;
        clock.switchTo(currentPlayer.color());
        turnNumber++;
        moveUsed = abilityUsed = false;
        if (mode == GameMode.MOBA) {
            var next = new HashMap<>(pieces);
            pieces.values().stream().filter(piece -> piece.color() == currentPlayer.color() && piece.cooldown() > 0
                    && !piece.isRemoved()).forEach(piece -> next.put(piece.id(),
                    piece.withAbilityState(piece.cooldown() - 1, piece.ultimateCharge(), piece.storedPieceId())));
            pieces = Map.copyOf(next);
        }
    }

    public static Game mobaHeroesSandbox(int seconds) {
        var base = HeroRules.sandbox();
        Game g = mobaPrototype(new HeroRules(base.pudge(), base.lumeshard(), base.crow(), base.gold(), base.bolotnik(), game.heroes.izba.IzbaRules.sandbox(), GinsengRules.sandbox(), FaunRules.sandbox(), AresRules.sandbox()), seconds);
        var sandboxBoard = new HashMap<>(g.gameField.getGameField());
        for (Position p : List.of(new Position(1, 3), new Position(6, 3)))
            sandboxBoard.put(p, g.gameField.createFigure(Bishop.class, p, p.getRow() == 1 ? FigureColor.WHITE : FigureColor.BLACK));
        g.gameField = Field.fromMobaPosition(sandboxBoard);
        var heroes = new HashMap<Position, HeroType>();
        for (PieceState p : g.pieces.values()) if (p.hero() != HeroType.NONE) heroes.put(p.position(), p.hero());
        heroes.put(new Position(1, 3), HeroType.ARES_BISHOP);
        heroes.put(new Position(6, 3), HeroType.ARES_BISHOP);
        heroes.put(new Position(0, 1), HeroType.FAUN_KNIGHT);
        heroes.put(new Position(7, 1), HeroType.FAUN_KNIGHT);
        heroes.put(new Position(0, 5), HeroType.GINSENG_BISHOP);
        heroes.put(new Position(7, 5), HeroType.GINSENG_BISHOP);
        heroes.put(new Position(0, 7), HeroType.IZBA_ROOK);
        heroes.put(new Position(7, 7), HeroType.IZBA_ROOK);
        g.rebuildPieces(heroes);
        return g;
    }

    private void rebuildPieces(Map<Position, HeroType> heroes) {
        if (heroes.containsValue(HeroType.BABA_YAGA_PAWN))
            throw new IllegalArgumentException("Baba Yaga can only be summoned");
        if ((heroes.containsValue(HeroType.IZBA_ROOK) || heroes.containsValue(HeroType.GINSENG_BISHOP)) && !summons.enabled())
            summons = SummonState.initial(java.util.concurrent.ThreadLocalRandom.current().nextLong());
        for (var entry : heroes.entrySet()) {
            Figure piece = gameField.getFigureByPosition(entry.getKey());
            if (entry.getValue() == null || piece == null || !entry.getValue().supports(piece.getClass()))
                throw new IllegalArgumentException("Hero must be assigned to its matching base piece");
        }
        var next = new HashMap<Long, PieceState>();
        for (int row = 0; row < 8; row++)
            for (int column = 0; column < 8; column++) {
                Position square = new Position(row, column);
                Figure figure = gameField.getFigureByPosition(square);
                if (figure.getColor() == FigureColor.NONE) continue;
                PieceState piece = PieceState.initial(nextPieceId++, figure, heroes.getOrDefault(square, HeroType.NONE));
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

    private void addSummonChanges(ActionEngine.Context before, ActionEngine.Context after) {
        for (var r : after.summons().records().values().stream().sorted(java.util.Comparator.comparingLong(SummonRecord::id)).toList()) {
            if (before.summons().records().containsKey(r.id()) && !before.summons().records().get(r.id()).transformed() && r.transformed()) {
                PieceState p = after.pieces().get(r.id());
                Position square = p.position() == null ? r.bornAt() : p.position();
                addEvent(GameEvent.Kind.YAGA_MATURE, p.color(), p.id(), p.id(), square, square, "Баба-яга превратилась в Избу");
            }
        }
        Long summoner = after.summons().records().values().stream().filter(r -> !before.summons().records().containsKey(r.id()) && r.cause() == SummonRecord.Cause.ULTIMATE).map(SummonRecord::sourceId).findFirst().orElse(null);
        for (PieceState p : after.pieces().values().stream().sorted(java.util.Comparator.comparingLong(PieceState::id)).toList()) {
            PieceState old = before.pieces().get(p.id());
            if (p.status() == PieceState.Status.REPLACED && old != null && old.status() != PieceState.Status.REPLACED) {
                Position square = old.position() == null ? before.pieces().get(summoner).position() : old.position();
                addEvent(GameEvent.Kind.REPLACE, currentPlayer.color(), summoner, p.id(), square, square, "Фигура заменена призывом без взятия");
            }
        }
    }

    private void addActionEffects(ActionEngine.Context before, ActionEngine.Preview preview) {
        var after = preview.next();
        addSummonChanges(before, after);
        Long spearActor = events.isEmpty() ? null : events.getLast().kind() == GameEvent.Kind.SPEAR ? events.getLast().actorId() : null;
        if (spearActor != null)
            for (PieceState p : after.pieces().values().stream().sorted(java.util.Comparator.comparingLong(PieceState::id)).toList()) {
                var old = before.pieces().get(p.id());
                if (p.isStunned() && old != null && p.stunnedThroughTurn() > old.stunnedThroughTurn())
                    addEvent(GameEvent.Kind.STUN, currentPlayer.color(), spearActor, p.id(), p.position(), p.position(), "Копьё оглушило фигуру на " + cell(p.position()));
            }
        var births = after.summons().records().values().stream().filter(r -> !before.summons().records().containsKey(r.id()))
                .sorted(java.util.Comparator.comparingLong(SummonRecord::id)).toList();
        var logged = new java.util.HashSet<Long>();
        for (var birth : births)
            if (birth.cause() == SummonRecord.Cause.ULTIMATE) {
                addBirth(after, birth);
                logged.add(birth.id());
            }
        for (var capture : preview.captures())
            if (capture.reaction()) {
                // A fresh pawn can die in mud during its creation action; its birth must precede that capture in the journal.
                for (var birth : births)
                    if (birth.id() == capture.victimId() && logged.add(birth.id())) addBirth(after, birth);
                addEvent(capture.cause() == Capture.Cause.ARES ? GameEvent.Kind.ARES_REVENGE : capture.cause() == Capture.Cause.BOG ? GameEvent.Kind.BOG_CAPTURE : GameEvent.Kind.COUNTER_CAPTURE,
                        capture.creditedSide(), capture.actorId(), capture.victimId(), capture.square(), capture.square(),
                        (capture.cause() == Capture.Cause.ARES ? "Ares забрал с собой " : capture.cause() == Capture.Cause.BOG ? "Bolotnik из болота взял " : "Союзник из Пуджа взял ") + capture.victim().getName() + " на " + cell(capture.square()));
            }
        for (var birth : births) if (logged.add(birth.id())) addBirth(after, birth);
    }

    private void addBirth(ActionEngine.Context after, SummonRecord r) {
        PieceState source = after.pieces().get(r.sourceId());
        addEvent(GameEvent.Kind.YAGA_BIRTH, source.color(), r.sourceId(), r.id(), r.bornAt(), r.bornAt(), "Баба-яга призвана на " + cell(r.bornAt()));
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }

    public synchronized GameSnapshot getSnapshot() {
        updateTime();
        ensurePieces();
        return new GameSnapshot(version, mode, gameField, pieces, getTurnState(), currentPlayer.color(), castlingRights,
                enPassantTarget, halfmoveClock, repetitions, clock.snapshot(), outcome, moves, events,
                whitePlayer.getPoints(), blackPlayer.getPoints(), whitePlayer.getAttackedFigures(), blackPlayer.getAttackedFigures(),
                drawOffer == null ? FigureColor.NONE : drawOffer.color(), heroRules, economy, terrain, draft, summons, spells, faun);
    }

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
        return economy;
    }

    public synchronized SummonState getSummons() {
        return summons;
    }

    public synchronized DraftSnapshot getDraft() {
        return draft;
    }

    public synchronized TerrainState getTerrain() {
        return terrain;
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
        return mode == GameMode.MOBA ? AttackRules.isInCheck(gameField.getGameField(), pieces, currentPlayer.color(), terrain)
                : MoveValidator.isInCheck(gameField.getGameField(), currentPlayer.color());
    }

    public synchronized String getLastMoveAsString() {
        return moves.isEmpty() ? "" : moves.getLast().toString();
    }
}
