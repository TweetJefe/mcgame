package game.heroes.supernova;

import game.heroes.HeroType;
import game.heroes.phoenix.PhoenixMechanic;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventDraft;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicStateCodec;
import game.mechanics.MechanicStep;
import game.moves.Success;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.summoning.SummonState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import model.Position;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.SETUP;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.update;

/** Случайные решения принадлежат снимку: подсветка и отказ не меняют последовательность. */
public final class SupernovaMechanic implements MechanicDefinition<SupernovaMechanic.State> {
    public record State(long seed, long random) {}
    public record Placement(long id, Position from, Position to) {
        public Placement {
            if (id < 1 || !inside(from) || !inside(to)) throw new IllegalArgumentException("Invalid mass movement");
        }
    }
    public record Cast(long actor, Position center, boolean shuffle, List<Long> participants,
                       List<Placement> placements) implements MechanicOperation {
        public Cast { participants = List.copyOf(participants); placements = List.copyOf(placements); }
    }
    public record Movement(List<Placement> placements, long random) {
        public Movement {
            placements = List.copyOf(placements);
            if (placements.stream().map(Placement::id).distinct().count() != placements.size()
                    || placements.stream().map(Placement::from).distinct().count() != placements.size()
                    || placements.stream().map(Placement::to).distinct().count() != placements.size())
                throw new IllegalArgumentException("Conflicting mass movements");
        }
    }
    public static final SupernovaMechanic INSTANCE = new SupernovaMechanic();
    public static final MechanicKey<State> KEY = new MechanicKey<>("supernova", State.class, new State(0, 0));
    public static final MechanicEventType<Movement> PULL = movementEvent("supernova.pull");
    public static final MechanicEventType<Movement> SHUFFLE = movementEvent("supernova.shuffle");
    private SupernovaMechanic() {}
    private static boolean inside(Position p) {
        return p != null && p.getRow() >= 0 && p.getRow() < 8 && p.getColumn() >= 0 && p.getColumn() < 8;
    }
    private static MechanicEventType<Movement> movementEvent(String id) {
        return new MechanicEventType<>(id, Movement.class, movement -> Map.of(
                "placements", movement.placements().stream().map(p -> p.id() + "," + p.from().getRow() + ","
                        + p.from().getColumn() + "," + p.to().getRow() + "," + p.to().getColumn()).collect(Collectors.joining(";")),
                "random", Long.toString(movement.random())), data -> {
                    if (!data.keySet().equals(Set.of("placements", "random")))
                        throw new IllegalArgumentException("Invalid movement fields");
                    var placements = new ArrayList<Placement>();
                    if (!data.get("placements").isEmpty()) for (String row : data.get("placements").split(";", -1)) {
                        var values = row.split(",", -1);
                        if (values.length != 5) throw new IllegalArgumentException("Invalid placement fields");
                        placements.add(new Placement(Long.parseLong(values[0]),
                                new Position(Integer.parseInt(values[1]), Integer.parseInt(values[2])),
                                new Position(Integer.parseInt(values[3]), Integer.parseInt(values[4]))));
                    }
                    return new Movement(placements, Long.parseLong(data.get("random")));
                });
    }
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicDefinition<?>> dependencies() { return List.of(PhoenixMechanic.INSTANCE); }
    @Override public List<MechanicEventType<?>> events() { return List.of(PULL, SHUFFLE); }
    @Override public MechanicStateCodec<State> codec() {
        return new MechanicStateCodec<>() {
            @Override public Map<String, String> encode(State state) {
                return Map.of("version", "1", "seed", Long.toString(state.seed()), "random", Long.toString(state.random()));
            }
            @Override public State decode(Map<String, String> fields) {
                if (!fields.keySet().equals(Set.of("version", "seed", "random")) || !"1".equals(fields.get("version")))
                    throw new IllegalArgumentException("Invalid Supernova state");
                return new State(Long.parseLong(fields.get("seed")), Long.parseLong(fields.get("random")));
            }
        };
    }
    @Override public List<MechanicStep> steps() {
        return List.of(update("supernova.seed", SETUP, "summons.initialize", s -> {
            if (s.assignments().containsValue(HeroType.PHOENIX)) {
                long seed = ThreadLocalRandom.current().nextLong();
                s.state(KEY, new State(seed, seed));
            }
        }));
    }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Cast.class, "supernova.cast", ABILITY_PREPARE, "spells.cast", (s, op) -> {
            var actor = s.pieces().get(op.actor());
            Movement movement;
            if (op.shuffle()) {
                var placements = op.participants().stream().map(s.pieces()::get)
                        .map(p -> new Placement(p.id(), p.position(), p.position())).toList();
                movement = shuffle(placements, s.state(KEY).random());
            } else movement = new Movement(op.placements(), s.state(KEY).random());
            var updates = new HashMap<Long, PieceState>();
            for (var p : movement.placements()) {
                PieceState piece = s.pieces().get(p.id());
                updates.put(piece.id(), piece.placed(p.to(), piece.type()));
            }
            s.update(updates);
            s.state(KEY, new State(s.state(KEY).seed(), movement.random()));
            s.propose(new MechanicEventDraft(s.turn(), actor.color(), actor.id(), null, actor.position(), op.center(),
                    op.shuffle() ? "Supernova: фигуры перемешаны" : "Supernova: фигуры стянуты",
                    (op.shuffle() ? SHUFFLE : PULL).event(movement)));
            return Success.SUCCESS;
        }));
    }
    static Movement shuffle(List<Placement> pieces, long random) {
        var destinations = new ArrayList<>(pieces.stream().map(Placement::from).toList());
        for (int i = destinations.size() - 1; i > 0; i--) {
            var choice = SummonState.initial(random).choose(i + 1);
            random = choice.nextState();
            java.util.Collections.swap(destinations, i, choice.index());
        }
        var result = new ArrayList<Placement>();
        for (int i = 0; i < pieces.size(); i++)
            result.add(new Placement(pieces.get(i).id(), pieces.get(i).from(), destinations.get(i)));
        return new Movement(result, random);
    }
    @Override public void validate(GameSnapshot snapshot) {
        State state = snapshot.mechanics().get(KEY);
        if (snapshot.mode() == game.state.GameMode.CLASSIC && !state.equals(KEY.initial()))
            throw new IllegalArgumentException("Supernova state in Classic");
        long random = state.seed();
        var rules = snapshot.heroRules().get(SupernovaDefinition.INSTANCE);
        for (var event : snapshot.events()) {
            if (!event.isEffect(PULL) && !event.isEffect(SHUFFLE)) continue;
            var type = event.isEffect(SHUFFLE) ? SHUFFLE : PULL;
            var data = type.type().cast(event.effect().data());
            if (SupernovaHero.distance(event.from(), event.to()) > rules.castRadius()
                    || data.placements().isEmpty() || data.placements().stream().anyMatch(p ->
                        !snapshot.pieces().containsKey(p.id()) || SupernovaHero.distance(p.from(), event.to()) > rules.effectRadius()))
                throw new IllegalArgumentException("Invalid mass movement geometry");
            if (type == SHUFFLE) {
                if (data.placements().size() < 2 || !data.equals(shuffle(data.placements(), random))
                        || !data.placements().stream().map(Placement::id).toList().equals(
                            data.placements().stream().map(Placement::id).sorted().toList()))
                    throw new IllegalArgumentException("Permutation differs from random sequence");
            } else if (data.random() != random || data.placements().stream().anyMatch(p ->
                    SupernovaHero.distance(p.to(), event.to()) != 1))
                throw new IllegalArgumentException("Invalid pull destination");
            random = data.random();
            var ability = type == SHUFFLE ? SupernovaAbility.SHUFFLE : SupernovaAbility.PULL;
            if (snapshot.events().stream().noneMatch(e -> e.sequence() < event.sequence() && e.turn() == event.turn()
                    && Objects.equals(e.actorId(), event.actorId()) && e.isAbility(ability)))
                throw new IllegalArgumentException("Mass movement without ability use");
        }
        if (random != state.random()) throw new IllegalArgumentException("Supernova random state differs from history");
        for (var event : snapshot.events()) if (event.isAbility(SupernovaAbility.PULL) || event.isAbility(SupernovaAbility.SHUFFLE)) {
            var type = event.isAbility(SupernovaAbility.PULL) ? PULL : SHUFFLE;
            if (snapshot.events().stream().filter(e -> e.isEffect(type) && e.turn() == event.turn()
                    && Objects.equals(e.actorId(), event.actorId())).count() != 1)
                throw new IllegalArgumentException("Ability without one mass movement");
        }
    }
}
