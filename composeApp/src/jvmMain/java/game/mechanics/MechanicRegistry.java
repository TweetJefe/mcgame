package game.mechanics;

import game.creation.PieceBirth;
import game.visibility.Concealment;
import game.heroes.HeroActionContext;
import game.history.GameEvent;
import model.Figure;
import model.FigureColor;
import game.moves.MoveResult;
import game.moves.Success;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Проверяет конфигурацию один раз; порядок не зависит от порядка регистрации героев. */
public final class MechanicRegistry {
    public static final MechanicRegistry EMPTY = new MechanicRegistry(List.of());
    private final List<MechanicDefinition<?>> definitions;
    private final Map<MechanicPhase, List<MechanicStep>> phases;
    private final Set<Class<?>> operations;
    private final Map<Class<?>, MechanicHandler<?>> immediateHandlers;
    private final Map<String, MechanicEventType<?>> events;
    private final List<MechanicCaptureReaction> captureReactions;

    public MechanicRegistry(List<? extends MechanicDefinition<?>> modules) {
        var all = new LinkedHashMap<String, MechanicDefinition<?>>();
        for (var module : modules.stream().sorted(Comparator.comparing(d -> d.key().id())).toList())
            collect(module, all, new HashSet<>());
        definitions = List.copyOf(all.values());
        var handlers = new HashSet<Class<?>>();
        var immediate = new HashMap<Class<?>, MechanicHandler<?>>();
        var eventTypes = new HashMap<String, MechanicEventType<?>>();
        var steps = new HashMap<String, MechanicStep>();
        var reactions = new HashMap<String, MechanicCaptureReaction>();
        for (var module : definitions) {
            for (var handler : module.handlers()) {
                if (!handlers.add(handler.type())) throw new IllegalArgumentException("Duplicate operation handler");
                immediate.put(handler.type(), handler);
                addStep(steps, handler.step());
            }
            for (var step : module.steps()) addStep(steps, step);
            for (var reaction : module.captureReactions()) {
                if (reactions.putIfAbsent(reaction.id(), reaction) != null)
                    throw new IllegalArgumentException("Duplicate capture reaction: " + reaction.id());
            }
            for (var event : module.events())
                if (eventTypes.putIfAbsent(event.id(), event) != null)
                    throw new IllegalArgumentException("Duplicate mechanic event ID: " + event.id());
        }
        operations = Set.copyOf(handlers);
        immediateHandlers = Map.copyOf(immediate);
        events = Map.copyOf(eventTypes);
        var sorted = new EnumMap<MechanicPhase, List<MechanicStep>>(MechanicPhase.class);
        for (var phase : MechanicPhase.values()) sorted.put(phase, order(phase, steps));
        phases = Map.copyOf(sorted);
        captureReactions = orderNodes("capture reactions", reactions,
                MechanicCaptureReaction::before, MechanicCaptureReaction::after);
    }
    private static void collect(MechanicDefinition<?> module, Map<String, MechanicDefinition<?>> all, Set<String> visiting) {
        String id = module.key().id();
        if (!visiting.add(id)) throw new IllegalArgumentException("Cyclic mechanic dependencies: " + id);
        var existing = all.get(id);
        if (existing != null && existing != module) throw new IllegalArgumentException("Conflicting mechanic: " + id);
        if (existing == null) {
            for (var dependency : module.dependencies().stream().sorted(Comparator.comparing(d -> d.key().id())).toList())
                collect(dependency, all, visiting);
            all.put(id, module);
        }
        visiting.remove(id);
    }
    private static void addStep(Map<String, MechanicStep> steps, MechanicStep step) {
        if (steps.putIfAbsent(step.id(), step) != null) throw new IllegalArgumentException("Duplicate step: " + step.id());
    }
    private static List<MechanicStep> order(MechanicPhase phase, Map<String, MechanicStep> all) {
        var steps = new HashMap<String, MechanicStep>();
        for (var step : all.values()) if (step.phase() == phase) steps.put(step.id(), step);
        return orderNodes(phase.toString(), steps, MechanicStep::before, MechanicStep::after);
    }

    private static <T> List<T> orderNodes(String phase, Map<String, T> nodes,
                                         Function<T, Set<String>> before, Function<T, Set<String>> after) {
        var pending = new HashMap<>(nodes);
        var prerequisites = new HashMap<String, Set<String>>();
        for (String id : nodes.keySet()) prerequisites.put(id, new HashSet<>());
        for (var entry : nodes.entrySet()) {
            for (String id : after.apply(entry.getValue())) {
                if (!nodes.containsKey(id)) throw new IllegalArgumentException("Unknown or cross-phase dependency: " + id);
                prerequisites.get(entry.getKey()).add(id);
            }
            for (String id : before.apply(entry.getValue())) {
                if (!nodes.containsKey(id)) throw new IllegalArgumentException("Unknown or cross-phase dependency: " + id);
                prerequisites.get(id).add(entry.getKey());
            }
        }
        var result = new ArrayList<T>();
        while (!pending.isEmpty()) {
            String ready = pending.keySet().stream().filter(id -> prerequisites.get(id).isEmpty()).sorted().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Cyclic mechanic steps in " + phase));
            result.add(pending.remove(ready));
            prerequisites.values().forEach(ids -> ids.remove(ready));
        }
        return List.copyOf(result);
    }
    public List<MechanicDefinition<?>> definitions() { return definitions; }
    public List<MechanicCaptureReaction> captureReactions() { return captureReactions; }
    public void checkOperation(MechanicOperation operation) {
        if (!operations.contains(operation.getClass())) throw new IllegalArgumentException("Unregistered operation: " + operation.getClass().getName());
    }
    /** Реакция на гибель завершается до обработки следующей гибели в очереди. */
    public MoveResult react(MechanicSession session, List<MechanicOperation> operations) {
        for (var operation : operations) {
            checkOperation(operation);
            MoveResult result = invoke(immediateHandlers.get(operation.getClass()), session, operation);
            if (!result.isSuccess()) return result;
        }
        return Success.SUCCESS;
    }
    private static <O extends MechanicOperation> MoveResult invoke(MechanicHandler<O> handler,
            MechanicSession session, MechanicOperation operation) {
        return handler.action().apply(session, handler.type().cast(operation));
    }
    public MoveResult run(MechanicPhase phase, MechanicSession session) {
        for (var step : phases.get(phase)) {
            var states = session.states();
            var pieces = session.pieces();
            var captures = session.captures();
            MoveResult result = step.action().apply(session);
            if ((phase == MechanicPhase.HISTORY || phase == MechanicPhase.END_HISTORY)
                    && (!states.equals(session.states()) || !pieces.equals(session.pieces()) || !captures.equals(session.captures())))
                throw new IllegalStateException("History step changed game state: " + step.id());
            if (!result.isSuccess()) return result;
        }
        return Success.SUCCESS;
    }
    public MechanicEventType<?> event(String id) {
        var type = events.get(id);
        if (type == null) throw new IllegalArgumentException("Unknown mechanic event: " + id);
        return type;
    }
    public PieceBirth birth(GameSnapshot snapshot, PieceState piece) {
        PieceBirth found = null;
        for (var module : definitions) {
            var value = module.birth(snapshot, piece);
            if (value == null) continue;
            if (found != null) throw new IllegalArgumentException("Conflicting births");
            found = value;
        }
        return found;
    }
    public boolean dismissed(HeroActionContext context, PieceState piece) {
        return definitions.stream().anyMatch(module -> module.dismissed(context, piece));
    }
    public boolean dismissed(GameSnapshot snapshot, PieceState piece) {
        return definitions.stream().anyMatch(module -> module.dismissed(snapshot, piece));
    }
    public Class<? extends Figure> revivalBody(HeroActionContext context, PieceState corpse) {
        Class<? extends Figure> found = null;
        for (var module : definitions) {
            var value = module.revivalBody(context, corpse);
            if (value == null) continue;
            if (found != null) throw new IllegalArgumentException("Conflicting revival forms");
            found = value;
        }
        return found;
    }
    public boolean reveals(HeroActionContext context, FigureColor viewer, model.Position square, Concealment kind) {
        return definitions.stream().anyMatch(module -> module.reveals(context, viewer, square, kind));
    }
    public PieceRestrictions restrictions(HeroActionContext context, PieceState piece) {
        if (piece == null) return PieceRestrictions.NONE;
        var result = PieceRestrictions.NONE;
        for (var module : definitions) result = result.merge(module.restrictions(context, piece));
        return result;
    }
    public Set<model.Position> passageObstacles(HeroActionContext context) {
        var result = new HashSet<model.Position>();
        for (var module : definitions) result.addAll(module.passageObstacles(context));
        return Set.copyOf(result);
    }
    public boolean allowsExtraMove(HeroActionContext context, PieceState actor) {
        return actor != null && definitions.stream().anyMatch(module -> module.allowsExtraMove(context, actor));
    }
    public game.heroes.HeroType initialHero(GameSnapshot snapshot, PieceState piece) {
        if (birth(snapshot, piece) != null) return game.heroes.HeroType.NONE;
        game.heroes.HeroType origin = null;
        for (var module : definitions) {
            var candidate = module.initialHero(snapshot, piece);
            if (candidate == null) continue;
            if (origin != null) throw new IllegalArgumentException("Conflicting piece origins");
            origin = candidate;
        }
        return origin == null ? piece.hero() : origin;
    }
    public List<PieceHeroChange> heroChanges(GameEvent event) {
        return definitions.stream().flatMap(module -> module.heroChanges(event).stream()).toList();
    }
    public List<PieceBodyChange> bodyChanges(GameEvent event) {
        return definitions.stream().flatMap(module -> module.bodyChanges(event).stream()).toList();
    }
    public List<PieceBodyChange> bodyChanges(GameEvent event, Map<Long, Class<? extends Figure>> before) {
        Map<Long, Class<? extends Figure>> immutable = Map.copyOf(before);
        return definitions.stream().flatMap(module -> module.bodyChanges(event, immutable).stream()).toList();
    }
    public boolean skipsTurn(HeroActionContext context, FigureColor side) {
        return definitions.stream().anyMatch(module -> module.skipsTurn(context, side));
    }
    public boolean isTurnSkipEvent(GameEvent event) {
        return definitions.stream().anyMatch(module -> module.isTurnSkipEvent(event));
    }
    public boolean requiresPositionHistory(HeroActionContext context) {
        return definitions.stream().anyMatch(module -> module.requiresPositionHistory(context));
    }
    public void validate(GameSnapshot snapshot) {
        for (var key : snapshot.mechanics().keys())
            if (definitions.stream().noneMatch(d -> d.key() == key))
                throw new IllegalArgumentException("Unregistered mechanic state: " + key.id());
        for (var event : snapshot.events()) if (event.effect() != null && event(event.effect().id()) != event.effect().type())
            throw new IllegalArgumentException("Noncanonical mechanic event");
        for (var module : definitions) module.validate(snapshot);
    }
}
