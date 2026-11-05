package game.mechanics;

import game.heroes.AbilityRequest;
import game.heroes.CaptureReactions;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroRegistry;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.history.GameEvent;
import game.moves.AbilityRejection;
import game.moves.Capture;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import model.FigureColor;
import model.Position;
import model.figures.King;

/** Изолированная рабочая область одного предпросмотра. Никогда не хранится в стратегии. */
public final class MechanicSession {
    private final HeroActionContext before;
    private final HeroRules rules;
    private final HeroRegistry heroes;
    private final FigureColor side;
    private final AbilityRequest request;
    private Map<Long, PieceState> pieces;
    private Map<Long, PieceState> entryBefore;
    private MechanicStates states;
    private MechanicStates local = MechanicStates.empty();
    private List<Capture> captures;
    private final List<MechanicOperation> pending = new ArrayList<>();
    private final List<GameEvent> history;
    private final List<MechanicEventDraft> proposed = new ArrayList<>();
    private Map<Position, HeroType> assignments = Map.of();
    private MechanicPhase phase;
    private MoveIntent movement;
    private boolean interceptedMove;
    private int iteration;

    public MechanicSession(HeroActionContext before, Map<Long, PieceState> prepared,
                           HeroRules rules, HeroRegistry heroes, FigureColor side,
                           AbilityRequest request, List<Capture> captures) {
        this(before, prepared, rules, heroes, side, request, captures, List.of());
    }

    public MechanicSession(HeroActionContext before, Map<Long, PieceState> prepared,
                           HeroRules rules, HeroRegistry heroes, FigureColor side,
                           AbilityRequest request, List<Capture> captures, List<GameEvent> history) {
        this.before = before;
        this.entryBefore = before.pieces();
        this.pieces = Map.copyOf(prepared);
        this.states = before.mechanics();
        this.rules = rules;
        this.heroes = heroes;
        this.side = side;
        this.request = request;
        this.captures = List.copyOf(captures);
        this.history = new ArrayList<>(history);
    }

    public Map<Position, HeroType> assignments() { return assignments; }
    public void assignments(Map<Position, HeroType> heroes) { assignments = Map.copyOf(heroes); }
    public MoveIntent movement() { return movement; }
    public void movement(MoveIntent intent) { movement = intent; }
    public void interceptMove() { interceptedMove = true; }
    public boolean interceptedMove() { return interceptedMove; }
    public HeroActionContext before() { return before; }
    public HeroActionContext context() { return new HeroActionContext(before.field(), pieces, turn(), states); }
    public HeroRules rules() { return rules; }
    public HeroRegistry heroes() { return heroes; }
    public FigureColor side() { return side; }
    public AbilityRequest request() { return request; }
    public long turn() { return before.currentTurn(); }
    public Map<Long, PieceState> pieces() { return pieces; }
    public void pieces(Map<Long, PieceState> pieces) { this.pieces = Map.copyOf(pieces); }
    public void update(Map<Long, PieceState> updates) {
        var next = new HashMap<>(pieces);
        next.putAll(updates);
        pieces(next);
    }
    public MechanicStates states() { return states; }
    public void states(MechanicStates states) { this.states = states; }
    public <S> S state(MechanicKey<S> key) { return states.get(key); }
    public <S> void state(MechanicKey<S> key, S value) { states = states.with(key, value); }
    public <S> S local(MechanicKey<S> key) { return local.get(key); }
    public <S> void local(MechanicKey<S> key, S value) { local = local.with(key, value); }
    public List<Capture> captures() { return captures; }
    public void captures(List<Capture> captures) { this.captures = List.copyOf(captures); }
    public Map<Long, PieceState> entryBefore() { return entryBefore; }
    public int iteration() { return iteration; }
    public void propose(MechanicEventDraft event) {
        if (heroes.mechanics().event(event.effect().id()) != event.effect().type())
            throw new IllegalArgumentException("Unregistered mechanic event");
        if (phase == MechanicPhase.HISTORY || phase == MechanicPhase.END_HISTORY)
            emit(event.numbered(history.size() + 1L));
        else proposed.add(event);
    }
    public List<MechanicEventDraft> proposedEvents() { return List.copyOf(proposed); }
    public List<GameEvent> history() { return List.copyOf(history); }
    public void emit(GameEvent event) {
        if (event.sequence() != history.size() + 1L) throw new IllegalArgumentException("Invalid event sequence");
        history.add(event);
    }
    public void enqueue(MechanicOperation operation) {
        heroes.mechanics().checkOperation(operation);
        pending.add(operation);
    }
    public void enqueue(List<MechanicOperation> operations) { operations.forEach(this::enqueue); }
    <O extends MechanicOperation> MoveResult handle(Class<O> type, BiFunction<MechanicSession, O, MoveResult> action) {
        var selected = pending.stream().filter(type::isInstance).map(type::cast).toList();
        pending.removeIf(type::isInstance);
        for (O operation : selected) {
            MoveResult result = action.apply(this, operation);
            if (!result.isSuccess()) return result;
        }
        return Success.SUCCESS;
    }
    public MoveResult run(MechanicPhase nextPhase) {
        var previous = phase;
        phase = nextPhase;
        try { return heroes.mechanics().run(nextPhase, this); }
        finally { phase = previous; }
    }
    /** Общее взятие из операции: удаление, заряд и полная очередь ответных реакций. */
    public MoveResult capture(long actorId, long victimId) {
        PieceState actor = pieces.get(actorId), victim = pieces.get(victimId);
        if (actor == null || victim == null || victim.position() == null || actorId == victimId)
            return AbilityRejection.INVALID_ABILITY_TARGET;
        if (victim.type() == King.class) return StatusRejection.KING_IMMUNE;
        var captured = new HashMap<>(pieces);
        captured.put(victim.id(), victim.hidden(victim.color() == actor.color()
                ? PieceState.Status.DESTROYED : PieceState.Status.CAPTURED));
        if (victim.color() != actor.color()) captured.put(actor.id(), heroes.get(actor.hero()).afterCapture(actor));
        var all = new ArrayList<>(captures);
        all.add(new Capture(actor.id(), victim.id(), actor.color(),
                before.field().createFigure(victim.type(), victim.position(), victim.color()), victim.position(), true, Capture.Cause.MECHANIC));
        var reaction = react(captured, victim, actor.id(), victim.position());
        if (!reaction.result().isSuccess()) return reaction.result();
        captured.putAll(reaction.updates());
        all.addAll(reaction.captures());
        pieces(captured);
        captures(all);
        return Success.SUCCESS;
    }

    public HeroEffect react(Map<Long, PieceState> prepared, PieceState victim, long killer, Position square) {
        return CaptureReactions.resolve(new HeroActionContext(before.field(), prepared, turn(), states),
                victim, killer, square, heroes, (context, effect) -> {
            if (effect.operations().isEmpty()) return effect;
            var savedPieces = pieces;
            var savedCaptures = captures;
            pieces(context.pieces());
            update(effect.updates());
            captures(effect.captures());
            MoveResult result = heroes.mechanics().react(this, effect.operations());
            var updates = new HashMap<Long, PieceState>();
            for (var entry : pieces.entrySet())
                if (!entry.getValue().equals(context.pieces().get(entry.getKey()))) updates.put(entry.getKey(), entry.getValue());
            var applied = result.isSuccess() ? new HeroEffect(result, updates, captures) : HeroEffect.rejected(result);
            pieces = savedPieces;
            captures = savedCaptures;
            return applied;
        }, () -> states);
    }

    public MoveResult settle() {
        for (iteration = 0; iteration < pieces.size() * 2L + 64; iteration++) {
            var oldPieces = pieces;
            var oldStates = states;
            MoveResult result = run(MechanicPhase.ENTRY);
            if (!result.isSuccess()) return result;
            if (pieces.equals(oldPieces) && states.equals(oldStates)) return Success.SUCCESS;
            entryBefore = oldPieces;
        }
        return MovementRejection.INVALID_BOARD;
    }
    public void requireComplete() {
        if (!pending.isEmpty()) throw new IllegalArgumentException("Unprocessed mechanic operations");
    }
}
