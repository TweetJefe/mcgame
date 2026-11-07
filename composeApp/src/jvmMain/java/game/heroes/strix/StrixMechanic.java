package game.heroes.strix;

import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.history.GameEvent;
import game.history.PositionHistoryMechanic;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventDraft;
import game.mechanics.MechanicEventPolicy;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.compat.HistoricalMechanics;
import game.moves.Success;
import game.state.GameSnapshot;
import game.turns.TurnFlowMechanic;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import model.Position;
import model.figures.King;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.Mechanics.handler;

/** Журнал возвратов отделён от общей истории позиций и общего управления очередью. */
public final class StrixMechanic implements MechanicDefinition<StrixMechanic.State> {
    public enum State { INSTANCE }
    public record Rewind(long source, long target, Position from, Position to) implements MechanicOperation {}
    public static final StrixMechanic INSTANCE = new StrixMechanic();
    private static final MechanicKey<State> KEY = new MechanicKey<>("strix", State.class, State.INSTANCE);
    public static final MechanicEventType<Rewind> REWOUND = new MechanicEventType<>("strix.rewound", Rewind.class,
            r -> Map.of("source", Long.toString(r.source()), "target", Long.toString(r.target()),
                    "fromRow", Integer.toString(r.from().getRow()), "fromColumn", Integer.toString(r.from().getColumn()),
                    "toRow", Integer.toString(r.to().getRow()), "toColumn", Integer.toString(r.to().getColumn())),
            data -> {
                if (!data.keySet().equals(Set.of("source", "target", "fromRow", "fromColumn", "toRow", "toColumn")))
                    throw new IllegalArgumentException("Invalid Strix rewind fields");
                return new Rewind(Long.parseLong(data.get("source")), Long.parseLong(data.get("target")),
                        new Position(Integer.parseInt(data.get("fromRow")), Integer.parseInt(data.get("fromColumn"))),
                        new Position(Integer.parseInt(data.get("toRow")), Integer.parseInt(data.get("toColumn"))));
            }, new MechanicEventPolicy(true, true, true, true));

    private StrixMechanic() {}
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicDefinition<?>> dependencies() {
        return List.of(HistoricalMechanics.INSTANCE, PositionHistoryMechanic.INSTANCE, TurnFlowMechanic.INSTANCE);
    }
    @Override public List<MechanicEventType<?>> events() { return List.of(REWOUND); }
    @Override public boolean requiresPositionHistory(HeroActionContext context) {
        return context.pieces().values().stream().anyMatch(p -> p.hero() == HeroType.STRIX);
    }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Rewind.class, "strix.rewind", ABILITY_PREPARE, "spells.cast", (s, op) -> {
            var source = s.before().pieceById(op.source());
            s.propose(new MechanicEventDraft(s.turn(), source.color(), op.source(), op.target(), op.from(), op.to(),
                    "Фигура вернулась на прежнюю клетку", REWOUND.event(op)));
            return Success.SUCCESS;
        }));
    }

    @Override public void validate(GameSnapshot snapshot) {
        var positions = PositionHistoryMechanic.KEY.initial();
        for (var event : snapshot.events()) {
            if (event.isEffect(PositionHistoryMechanic.FRAME))
                positions = positions.add(PositionHistoryMechanic.FRAME.type().cast(event.effect().data()));
            if (!event.isEffect(REWOUND)) continue;
            var rewind = REWOUND.type().cast(event.effect().data());
            var target = snapshot.pieces().get(rewind.target());
            Position expected = target == null || target.type() == King.class ? null
                    : positions.twoCompletedTurnsAgo(target, event.turn());
            if (rewind.source() != event.actorId() || rewind.target() != event.targetId() || expected == null
                    || !expected.equals(rewind.to()) || !rewind.from().equals(event.from())
                    || !rewind.to().equals(event.to()) || rewind.from().equals(rewind.to()))
                throw new IllegalArgumentException("Rewind differs from recorded positions");
            var cast = snapshot.events().stream().filter(e -> e.sequence() < event.sequence() && e.turn() == event.turn()
                    && e.isAbility(StrixAbility.REWIND) && Objects.equals(e.actorId(), event.actorId())
                    && Objects.equals(e.targetId(), event.targetId())).findFirst().orElseThrow();
            if (StrixHero.distance(cast.from(), rewind.from()) > 2)
                throw new IllegalArgumentException("Rewind target was outside range");
        }
        for (var event : snapshot.events()) {
            if (event.isAbility(StrixAbility.REWIND) && matching(snapshot, event, REWOUND) != 1)
                throw new IllegalArgumentException("Rewind without one recorded movement");
            if (event.isAbility(StrixAbility.TIME_SKIP) && matching(snapshot, event, TurnFlowMechanic.REQUESTED) != 1)
                throw new IllegalArgumentException("Ultimate without one turn skip request");
        }
    }

    private static long matching(GameSnapshot snapshot, GameEvent event, MechanicEventType<?> type) {
        return snapshot.events().stream().filter(e -> e.isEffect(type) && e.turn() == event.turn()
                && Objects.equals(e.actorId(), event.actorId())).count();
    }
}
