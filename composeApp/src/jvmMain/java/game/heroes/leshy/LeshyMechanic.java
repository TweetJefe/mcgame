package game.heroes.leshy;

import game.heroes.HeroActionContext;
import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventDraft;
import game.mechanics.MechanicEventPolicy;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicSession;
import game.mechanics.MechanicStep;
import game.mechanics.PieceRestriction;
import game.mechanics.PieceRestrictions;
import game.mechanics.compat.HistoricalMechanics;
import game.moves.Success;
import game.state.GameMode;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import model.Position;
import model.figures.King;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.CAN_END;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.Mechanics.handler;

/** Эффекты — накладки на ID и клетки: личность, тело и ресурсы цели не подменяются. */
public final class LeshyMechanic implements MechanicDefinition<LeshyMechanic.State> {
    public record Vine(long source, long castTurn, long until, List<Position> cells) {
        public Vine {
            cells = List.copyOf(cells);
            if (source < 1 || castTurn < 1 || until <= castTurn || cells.isEmpty() || cells.size() > 7 || cells.stream().distinct().count() != cells.size() || cells.stream().anyMatch(p -> !inside(p)))
                throw new IllegalArgumentException("Invalid vine patch");
        }
    }
    public record Tree(long source, long target, long castTurn, long until, Position square) {
        public Tree {
            if (source < 1 || target < 1 || source == target || castTurn < 1 || until != 0 && until <= castTurn || !inside(square))
                throw new IllegalArgumentException("Invalid tree effect");
        }
    }
    public record State(List<Vine> vines, Map<Long, Tree> trees) {
        public State { vines = List.copyOf(vines); trees = Map.copyOf(trees); }
        public State expire(long turn) {
            var next = new HashMap<>(trees);
            next.values().removeIf(t -> t.until() != 0 && t.until() <= turn);
            return new State(vines.stream().filter(v -> v.until() > turn).toList(), next);
        }
        public Set<Position> cells() { return vines.stream().flatMap(v -> v.cells().stream()).collect(Collectors.toUnmodifiableSet()); }
    }
    public record Plant(long source, List<Position> cells) implements MechanicOperation {
        public Plant { cells = List.copyOf(cells); }
    }
    public record Forest(long source, List<Long> targets) implements MechanicOperation {
        public Forest { targets = List.copyOf(targets); }
    }
    public record Trees(List<Tree> trees) { public Trees { trees = List.copyOf(trees); } }
    public record Expiry(long turn) {}
    public static final LeshyMechanic INSTANCE = new LeshyMechanic();
    public static final MechanicKey<State> KEY = new MechanicKey<>("leshy", State.class, new State(List.of(), Map.of()));
    private static final PieceRestrictions ROOTED = PieceRestrictions.of(PieceRestriction.ORDINARY_MOVE, PieceRestriction.ATTACK);
    private static final PieceRestrictions TREE = PieceRestrictions.of(PieceRestriction.ORDINARY_MOVE, PieceRestriction.ATTACK,
            PieceRestriction.ABILITIES, PieceRestriction.ITEMS, PieceRestriction.CAPTURE, PieceRestriction.DISPLACEMENT,
            PieceRestriction.PASSIVES, PieceRestriction.TRANSFORMATION);
    public static final MechanicEventType<Vine> PLANTED = new MechanicEventType<>("leshy.planted", Vine.class,
            v -> Map.of("source", Long.toString(v.source()), "castTurn", Long.toString(v.castTurn()),
                    "until", Long.toString(v.until()), "cells", cells(v.cells())), data -> {
                fields(data, "source", "castTurn", "until", "cells");
                return new Vine(number(data,"source"), number(data,"castTurn"), number(data,"until"), parseCells(data.get("cells")));
            });
    public static final MechanicEventType<Trees> WOODED = new MechanicEventType<>("leshy.wooded", Trees.class,
            p -> Map.of("trees", p.trees().stream().map(t -> t.source()+","+t.target()+","+t.castTurn()+","+t.until()+","
                    +t.square().getRow()+","+t.square().getColumn()).collect(Collectors.joining(";"))), data -> {
                fields(data,"trees");
                var trees = new ArrayList<Tree>();
                for (String line : data.get("trees").split(";", -1)) {
                    var values = line.split(",", -1);
                    if (values.length != 6) throw new IllegalArgumentException("Invalid tree fields");
                    trees.add(new Tree(Long.parseLong(values[0]), Long.parseLong(values[1]), Long.parseLong(values[2]),
                            Long.parseLong(values[3]), new Position(Integer.parseInt(values[4]), Integer.parseInt(values[5]))));
                }
                return new Trees(trees);
            });
    public static final MechanicEventType<Expiry> EXPIRED = new MechanicEventType<>("leshy.expired", Expiry.class,
            p -> Map.of("turn", Long.toString(p.turn())), data -> { fields(data,"turn");
            return new Expiry(number(data,"turn")); },
            new MechanicEventPolicy(true, false, false, false));

    private LeshyMechanic() {}
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicDefinition<?>> dependencies() { return List.of(HistoricalMechanics.INSTANCE); }
    @Override public List<MechanicEventType<?>> events() { return List.of(PLANTED, WOODED, EXPIRED); }
    @Override public PieceRestrictions restrictions(HeroActionContext context, PieceState piece) {
        var state = context.state(KEY);
        if (state.trees().containsKey(piece.id())) return TREE;
        return piece.type() != King.class && piece.position() != null && state.cells().contains(piece.position()) ? ROOTED : PieceRestrictions.NONE;
    }
    @Override public Set<Position> passageObstacles(HeroActionContext context) { return context.state(KEY).cells(); }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Plant.class,"leshy.plant", ABILITY_PREPARE,"spells.cast", (s, op) -> {
            var actor = s.pieces().get(op.source());
            var vines = new ArrayList<>(s.state(KEY).vines());
            var vine = new Vine(actor.id(), s.turn(), Math.addExact(s.turn(), 2L*s.rules().get(LeshyDefinition.INSTANCE).vineTurns()), op.cells());
            vines.add(vine);
            s.state(KEY, new State(vines, s.state(KEY).trees()));
            s.propose(new MechanicEventDraft(s.turn(), actor.color(), actor.id(), null, actor.position(), actor.position(),
                    "Leshy: появилась лоза", PLANTED.event(vine)));
            return Success.SUCCESS;
        }), handler(Forest.class,"leshy.forest", ABILITY_PREPARE,"leshy.plant", (s, op) -> {
            var actor = s.pieces().get(op.source());
            var trees = new HashMap<>(s.state(KEY).trees());
            var planted = new ArrayList<Tree>();
            for (long id : op.targets()) {
                var target = s.pieces().get(id);
                long until = target.color() == actor.color() ? Math.addExact(s.turn(), 2L*s.rules().get(LeshyDefinition.INSTANCE).treeTurns()) : 0;
                var tree = new Tree(actor.id(), id, s.turn(), until, target.position());
                trees.put(id, tree);
                planted.add(tree);
            }
            s.state(KEY, new State(s.state(KEY).vines(), trees));
            s.propose(new MechanicEventDraft(s.turn(), actor.color(), actor.id(), null, actor.position(), actor.position(),
                    "Leshy: появились деревья", WOODED.event(new Trees(planted))));
            return Success.SUCCESS;
        }));
    }
    @Override public List<MechanicStep> steps() {
        return List.of(new MechanicStep("leshy.canEnd", CAN_END, Set.of("clown.canEnd"), Set.of(), s -> expire(s, false)),
                new MechanicStep("leshy.expire", END_TURN, Set.of("clown.expire"), Set.of(), s -> expire(s, true)));
    }
    private static game.moves.MoveResult expire(MechanicSession s, boolean journal) {
        var previous = s.state(KEY);
        var next = previous.expire(s.turn());
        s.state(KEY, next);
        if (journal && !previous.equals(next)) s.propose(new MechanicEventDraft(s.turn(), s.side(), null, null, null, null,
                "Leshy: закончился срок лозы или союзного дерева", EXPIRED.event(new Expiry(s.turn()))));
        return Success.SUCCESS;
    }
    @Override public State restore(GameSnapshot snapshot) {
        State state = KEY.initial();
        var rules = snapshot.heroRules().get(LeshyDefinition.INSTANCE);
        for (var e : snapshot.events()) {
            if (e.isEffect(PLANTED)) {
                var vine = PLANTED.type().cast(e.effect().data());
                if (vine.source() != e.actorId() || vine.castTurn() != e.turn() || vine.until() != e.turn()+2L*rules.vineTurns() || !vine.cells().equals(LeshyHero.vineCells(e.from(), snapshot.pieces().get(vine.source()).color(), rules.vineLength())))
                    throw new IllegalArgumentException("Invalid vine geometry or duration");
                var vines = new ArrayList<>(state.vines());
                vines.add(vine);
                state = new State(vines, state.trees());
            }
            if (e.isEffect(WOODED)) {
                var trees = new HashMap<>(state.trees());
                var planted = WOODED.type().cast(e.effect().data()).trees();
                if (planted.isEmpty() || planted.size()>2) throw new IllegalArgumentException("Invalid forest size");
                var used = new HashSet<Position>();
                for (var tree : planted) {
                    var actor = snapshot.pieces().get(tree.source());
                    var target = snapshot.pieces().get(tree.target());
                    if (actor == null || target == null || tree.source() != e.actorId() || tree.target() == tree.source() || target.type() == King.class || tree.castTurn() != e.turn() || tree.until() != (actor.color() == target.color()?e.turn()+2L*rules.treeTurns():0) || tree.square().getRow() != e.from().getRow() || Math.abs(tree.square().getColumn()-e.from().getColumn()) != 1 || !used.add(tree.square()) || trees.putIfAbsent(tree.target(), tree) != null)
                        throw new IllegalArgumentException("Invalid tree origin or duration");
                }
                state = new State(state.vines(), trees);
            }
            if (e.isEffect(EXPIRED)) {
                var next = state.expire(e.turn());
                if (e.turn() != EXPIRED.type().cast(e.effect().data()).turn() || next.equals(state) || snapshot.events().stream().noneMatch(end -> end.kind() == GameEvent.Kind.END_TURN && end.turn() == e.turn()))
                    throw new IllegalArgumentException("Invalid effect expiry");
                state = next;
            }
        }
        return state;
    }
    @Override public void validate(GameSnapshot snapshot) {
        var state = snapshot.mechanics().get(KEY);
        if (!state.equals(restore(snapshot))) throw new IllegalArgumentException("Leshy state differs from history");
        if (snapshot.mode() == GameMode.CLASSIC && !state.equals(KEY.initial())) throw new IllegalArgumentException("Leshy effects in Classic");
        for (var tree : state.trees().values()) {
            var piece = snapshot.pieces().get(tree.target());
            if (piece == null || piece.position() == null || !tree.square().equals(piece.position()) || tree.until() != 0 && tree.until()<snapshot.turn().number()) throw new IllegalArgumentException("Missing or overdue tree");
        }
        for (var vine : state.vines()) if (vine.until()<snapshot.turn().number()) throw new IllegalArgumentException("Overdue vine");
        for (var event : snapshot.events()) {
            var ability = event.isEffect(PLANTED) ? LeshyAbility.VINE : event.isEffect(WOODED) ? LeshyAbility.FOREST : null;
            if (ability != null && snapshot.events().stream().noneMatch(c -> c.sequence()<event.sequence() && c.turn() == event.turn() && Objects.equals(c.actorId(), event.actorId()) && c.isAbility(ability) && Objects.equals(c.from(), event.from()) && Objects.equals(c.to(), event.to()))) throw new IllegalArgumentException("Effect without ability");
            if (event.isAbility(LeshyAbility.VINE) || event.isAbility(LeshyAbility.FOREST)) {
                var type = event.isAbility(LeshyAbility.VINE) ? PLANTED : WOODED;
                if (snapshot.events().stream().filter(e -> e.isEffect(type) && e.turn() == event.turn() && Objects.equals(e.actorId(), event.actorId())).count() != 1) throw new IllegalArgumentException("Ability without one effect");
            }
        }
    }
    private static boolean inside(Position p) {
        return p != null && p.getRow() >= 0 && p.getRow() < 8 && p.getColumn() >= 0 && p.getColumn() < 8;
    }
    private static String cells(List<Position> cells) {
        return cells.stream().map(p -> p.getRow()+","+p.getColumn()).collect(Collectors.joining(";"));
    }
    private static List<Position> parseCells(String text) {
        var result = new ArrayList<Position>();
        for (String row : text.split(";", -1)) {
            var values = row.split(",", -1);
            if (values.length != 2) throw new IllegalArgumentException("Invalid vine cell");
            result.add(new Position(Integer.parseInt(values[0]), Integer.parseInt(values[1])));
        }
        return List.copyOf(result);
    }
    private static void fields(Map<String, String> fields, String... names) {
        if (!fields.keySet().equals(Set.of(names))) throw new IllegalArgumentException("Invalid Leshy event fields");
    }
    private static long number(Map<String, String> data, String key) { return Long.parseLong(data.get(key)); }
}
