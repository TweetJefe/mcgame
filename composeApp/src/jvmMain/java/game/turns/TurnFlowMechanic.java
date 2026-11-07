package game.turns;

import game.heroes.HeroActionContext;
import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventDraft;
import game.mechanics.MechanicEventPolicy;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicStep;
import game.moves.AbilityRejection;
import game.moves.Success;
import game.state.GameSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.FigureColor;
import rules.standard.MoveValidator;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.SKIP_TURN;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.step;

/** Общий запрос на пропуск ближайшего хода: источник не задаёт правила перехода очереди. */
public final class TurnFlowMechanic implements MechanicDefinition<TurnFlowMechanic.State> {
    public record Pending(long source, long turn, FigureColor side) {
        public Pending {
            if (source < 1 || turn < 2 || side == null || side == FigureColor.NONE)
                throw new IllegalArgumentException("Invalid skipped turn request");
        }
    }
    public record State(Pending pending) {}
    public record RequestSkip(long source) implements MechanicOperation {}
    public static final TurnFlowMechanic INSTANCE = new TurnFlowMechanic();
    public static final MechanicKey<State> KEY = new MechanicKey<>("turnFlow", State.class, new State(null));
    public static final MechanicEventType<Pending> REQUESTED = type("turnFlow.requested", true);
    public static final MechanicEventType<Pending> SKIPPED = type("turnFlow.skipped", false);

    private TurnFlowMechanic() {}
    private static MechanicEventType<Pending> type(String id, boolean squares) {
        return new MechanicEventType<>(id, Pending.class,
                p -> Map.of("source", Long.toString(p.source()), "turn", Long.toString(p.turn()), "side", p.side().name()),
                data -> {
                    if (!data.keySet().equals(Set.of("source", "turn", "side")))
                        throw new IllegalArgumentException("Invalid turn flow fields");
                    return new Pending(Long.parseLong(data.get("source")), Long.parseLong(data.get("turn")),
                            FigureColor.valueOf(data.get("side")));
                }, new MechanicEventPolicy(true, true, false, squares));
    }
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicEventType<?>> events() { return List.of(REQUESTED, SKIPPED); }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(RequestSkip.class, "turnFlow.request", ABILITY_PREPARE, null, (s, op) -> {
            if (s.state(KEY).pending() != null) return AbilityRejection.INVALID_ABILITY_TARGET;
            var source = s.pieces().get(op.source());
            var pending = new Pending(source.id(), Math.addExact(s.turn(), 1), MoveValidator.opposite(source.color()));
            s.state(KEY, new State(pending));
            s.propose(new MechanicEventDraft(s.turn(), source.color(), source.id(), null, source.position(), source.position(),
                    "Следующий ход соперника будет пропущен", REQUESTED.event(pending)));
            return Success.SUCCESS;
        }));
    }
    @Override public boolean skipsTurn(HeroActionContext context, FigureColor side) {
        var pending = context.state(KEY).pending();
        return pending != null && pending.side() == side && pending.turn() == context.currentTurn();
    }
    @Override public boolean isTurnSkipEvent(GameEvent event) { return event.isEffect(SKIPPED); }
    @Override public List<MechanicStep> steps() {
        return List.of(step("turnFlow.consume", SKIP_TURN, null, s -> {
            var pending = s.state(KEY).pending();
            if (!skipsTurn(s.context(), s.side())) return AbilityRejection.INVALID_ABILITY_TARGET;
            s.state(KEY, new State(null));
            s.propose(new MechanicEventDraft(s.turn(), s.side(), pending.source(), null, null, null,
                    "Ход пропущен", SKIPPED.event(pending)));
            return Success.SUCCESS;
        }));
    }
    @Override public State restore(GameSnapshot snapshot) {
        Pending pending = null;
        for (var event : snapshot.events()) {
            if (event.isEffect(REQUESTED)) {
                if (pending != null) throw new IllegalArgumentException("Overlapping turn skips");
                pending = REQUESTED.type().cast(event.effect().data());
                if (pending.source() != event.actorId() || pending.turn() != event.turn() + 1
                        || pending.side() != MoveValidator.opposite(event.color()))
                    throw new IllegalArgumentException("Invalid turn skip timing");
            }
            if (event.isEffect(SKIPPED)) {
                var consumed = SKIPPED.type().cast(event.effect().data());
                if (!consumed.equals(pending) || event.turn() != consumed.turn() || event.color() != consumed.side()
                        || event.actorId() != consumed.source())
                    throw new IllegalArgumentException("Unrequested skipped turn");
                pending = null;
            }
        }
        return new State(pending);
    }
    @Override public void validate(GameSnapshot snapshot) {
        var state = snapshot.mechanics().get(KEY);
        if (!state.equals(restore(snapshot)))
            throw new IllegalArgumentException("Turn flow differs from history");
        if (state.pending() != null && (state.pending().turn() < snapshot.turn().number()
                || state.pending().turn() > snapshot.turn().number() + 1
                || state.pending().turn() == snapshot.turn().number() && !snapshot.outcome().isFinished()))
            throw new IllegalArgumentException("Pending skip is outside the next turn boundary");
        for (var event : snapshot.events()) if (event.isEffect(REQUESTED)) {
            boolean cast = snapshot.events().stream().anyMatch(e -> e.sequence() < event.sequence() && e.turn() == event.turn()
                    && e.kind() == GameEvent.Kind.ABILITY_USED && java.util.Objects.equals(e.actorId(), event.actorId()));
            if (!cast) throw new IllegalArgumentException("Turn skip without an active ability");
        }
    }
}
