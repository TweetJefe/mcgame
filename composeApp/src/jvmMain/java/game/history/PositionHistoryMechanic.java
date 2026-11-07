package game.history;

import game.heroes.HeroActionContext;
import game.heroes.HeroRegistry;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventDraft;
import game.mechanics.MechanicEventPolicy;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicStep;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.FigureColor;
import model.Position;

import static game.mechanics.MechanicPhase.BEGIN_TURN;
import static game.mechanics.Mechanics.update;

/** Позиции на началах последних трёх ходов каждой стороны, включая пропущенные ходы. */
public final class PositionHistoryMechanic implements MechanicDefinition<PositionHistoryMechanic.State> {
    public record Frame(long turn, FigureColor side, Map<Long, Position> positions) {
        public Frame {
            positions = Map.copyOf(positions);
            if (turn < 1 || side == null || side == FigureColor.NONE || positions.entrySet().stream().anyMatch(e ->
                    e.getKey() < 1 || e.getValue().getRow() < 0 || e.getValue().getRow() > 7
                            || e.getValue().getColumn() < 0 || e.getValue().getColumn() > 7))
                throw new IllegalArgumentException("Invalid position frame");
        }
    }
    public record State(Map<Long, Frame> frames) {
        public State { frames = Map.copyOf(frames); }
        public State add(Frame frame) {
            var next = new HashMap<>(frames);
            if (next.putIfAbsent(frame.turn(), frame) != null)
                throw new IllegalArgumentException("Duplicate position frame");
            for (var side : List.of(FigureColor.WHITE, FigureColor.BLACK))
                next.values().stream().filter(f -> f.side() == side).sorted(Comparator.comparingLong(Frame::turn).reversed())
                        .skip(3).map(Frame::turn).toList().forEach(next::remove);
            return new State(next);
        }
        public Position twoCompletedTurnsAgo(PieceState piece, long currentTurn) {
            var previous = frames.values().stream().filter(f -> f.side() == piece.color() && f.turn() < currentTurn)
                    .sorted(Comparator.comparingLong(Frame::turn).reversed()).limit(2).toList();
            return previous.size() < 2 ? null : previous.get(1).positions().get(piece.id());
        }
    }
    public static final PositionHistoryMechanic INSTANCE = new PositionHistoryMechanic();
    public static final MechanicKey<State> KEY = new MechanicKey<>("positionHistory", State.class, new State(Map.of()));
    public static final MechanicEventType<Frame> FRAME = new MechanicEventType<>("positionHistory.frame", Frame.class,
            frame -> {
                var data = new HashMap<String, String>();
                data.put("turn", Long.toString(frame.turn())); data.put("side", frame.side().name());
                frame.positions().forEach((id, p) -> data.put("p." + id, p.getRow() + "," + p.getColumn()));
                return Map.copyOf(data);
            }, data -> {
                var positions = new HashMap<Long, Position>();
                for (var entry : data.entrySet()) {
                    if (entry.getKey().equals("turn") || entry.getKey().equals("side")) continue;
                    if (!entry.getKey().startsWith("p.")) throw new IllegalArgumentException("Unknown position frame field");
                    String[] cell = entry.getValue().split(",", -1);
                    if (cell.length != 2) throw new IllegalArgumentException("Invalid position frame square");
                    positions.put(Long.parseLong(entry.getKey().substring(2)),
                            new Position(Integer.parseInt(cell[0]), Integer.parseInt(cell[1])));
                }
                return new Frame(Long.parseLong(data.get("turn")), FigureColor.valueOf(data.get("side")), positions);
            }, new MechanicEventPolicy(true, false, false, false));

    private PositionHistoryMechanic() {}
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicEventType<?>> events() { return List.of(FRAME); }
    @Override public List<MechanicStep> steps() {
        return List.of(update("positionHistory.begin", BEGIN_TURN, null, s -> {
            if (s.state(KEY).frames().isEmpty() && !s.heroes().mechanics().requiresPositionHistory(s.context())) return;
            var positions = new HashMap<Long, Position>();
            for (var piece : s.pieces().values()) if (piece.position() != null) positions.put(piece.id(), piece.position());
            var frame = new Frame(s.turn(), s.side(), positions);
            s.state(KEY, s.state(KEY).add(frame));
            s.propose(new MechanicEventDraft(s.turn(), s.side(), null, null, null, null,
                    "Начало хода: " + (s.side() == FigureColor.WHITE ? "белые" : "чёрные"), FRAME.event(frame)));
        }));
    }
    @Override public State restore(GameSnapshot snapshot) {
        State state = KEY.initial();
        for (var event : snapshot.events()) if (event.isEffect(FRAME)) {
            var frame = FRAME.type().cast(event.effect().data());
            if (frame.turn() != event.turn() || frame.side() != event.color()
                    || !snapshot.pieces().keySet().containsAll(frame.positions().keySet()))
                throw new IllegalArgumentException("Position frame differs from its event");
            state = state.add(frame);
        }
        return state;
    }
    @Override public void validate(GameSnapshot snapshot) {
        var state = snapshot.mechanics().get(KEY);
        if (!state.equals(restore(snapshot))) throw new IllegalArgumentException("Position history differs from journal");
        var recorded = snapshot.events().stream().filter(e -> e.isEffect(FRAME)).toList();
        if (!recorded.isEmpty() && recorded.size() != snapshot.turn().number() - recorded.getFirst().turn() + 1)
            throw new IllegalArgumentException("Missing position frame between turns");
        if (!snapshot.turn().moveUsed() && !snapshot.turn().abilityUsed() && state.frames().containsKey(snapshot.turn().number())) {
            var positions = new HashMap<Long, Position>();
            for (var piece : snapshot.pieces().values()) if (piece.position() != null) positions.put(piece.id(), piece.position());
            if (!state.frames().get(snapshot.turn().number()).positions().equals(positions))
                throw new IllegalArgumentException("Current position differs from the start of its turn");
        }
        var context = new HeroActionContext(snapshot.field(), snapshot.pieces(), snapshot.turn().number(), snapshot.mechanics());
        if (HeroRegistry.fromRules(snapshot.heroRules()).mechanics().requiresPositionHistory(context)
                && !state.frames().containsKey(snapshot.turn().number()))
            throw new IllegalArgumentException("Missing current position frame");
    }
}
