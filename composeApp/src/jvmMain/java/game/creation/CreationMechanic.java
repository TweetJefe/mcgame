package game.creation;

import game.heroes.HeroRegistry;
import game.mechanics.PieceBodyHistory;
import model.Field;
import game.heroes.HeroActionContext;
import game.heroes.HeroPieceFactory;
import game.heroes.HeroType;
import game.heroes.PieceCreationContext;
import game.heroes.RevivalPlan;
import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventDraft;
import game.mechanics.MechanicEventPolicy;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicSession;
import game.mechanics.MechanicStep;
import game.mechanics.PieceBodyChange;
import game.mechanics.PieceHeroChange;
import game.mechanics.compat.HistoricalMechanics;
import game.state.GameMode;
import game.state.GameSnapshot;
import game.state.ItemSlot;
import game.state.PieceState;
import game.state.SquareColor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import model.FigureColor;
import model.Position;
import model.figures.Ward;

import static game.mechanics.MechanicPhase.CAN_END_SETTLED;
import static game.mechanics.MechanicPhase.END_TURN_SETTLED;
import static game.mechanics.Mechanics.update;

/** Учёт новых жизней не переписывает погибших, список взятий и начисленное золото. */
public final class CreationMechanic implements MechanicDefinition<CreationMechanic.State> {
    public record State(Map<Long, PieceBirth> births, Set<Long> dismissed, Set<Long> evolved) {
        public State { births = Map.copyOf(births); dismissed = Set.copyOf(dismissed); evolved = Set.copyOf(evolved); }
        public Set<Long> usedCorpses() {
            return births.values().stream().map(PieceBirth::corpse).filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
    }
    public record Removed(long id) {}
    public record Evolution(long id, HeroType beforeHero, CreatedBody beforeBody, HeroType hero, CreatedBody body) {}
    public static final CreationMechanic INSTANCE = new CreationMechanic();
    public static final MechanicKey<State> KEY = new MechanicKey<>("creation", State.class,
            new State(Map.of(), Set.of(), Set.of()));
    public static final MechanicEventType<PieceBirth> BORN = new MechanicEventType<>("creation.born", PieceBirth.class,
            CreationMechanic::encodeBirth, CreationMechanic::decodeBirth, new MechanicEventPolicy(true, true, true, true));
    public static final MechanicEventType<Removed> REMOVED = new MechanicEventType<>("creation.removed", Removed.class,
            p -> Map.of("id", Long.toString(p.id())), data -> { fields(data,"id"); return new Removed(Long.parseLong(data.get("id"))); },
            new MechanicEventPolicy(false, true, false, true));
    public static final MechanicEventType<Evolution> EVOLVED = new MechanicEventType<>("creation.evolved", Evolution.class,
            p -> Map.of("id", Long.toString(p.id()), "beforeHero", p.beforeHero().name(), "beforeBody", p.beforeBody().name(),
                    "hero", p.hero().name(), "body", p.body().name()), data -> {
                fields(data,"id","beforeHero","beforeBody","hero","body");
                return new Evolution(Long.parseLong(data.get("id")), HeroType.valueOf(data.get("beforeHero")),
                        CreatedBody.valueOf(data.get("beforeBody")), HeroType.valueOf(data.get("hero")), CreatedBody.valueOf(data.get("body")));
            }, new MechanicEventPolicy(false, true, false, true));

    private CreationMechanic() {}
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicDefinition<?>> dependencies() { return List.of(HistoricalMechanics.INSTANCE); }
    @Override public List<MechanicEventType<?>> events() { return List.of(BORN, REMOVED, EVOLVED); }

    public static PieceState create(MechanicSession s, long caster, PieceState corpse, HeroType hero,
            CreatedBody body, SquareColor startingColor, int cooldown, Position square, RevivalPlan.Evolution evolution) {
        if (s.context().pieceAt(square) != null || s.pieces().size() >= 100_000)
            throw new IllegalArgumentException("No creation space");
        if (corpse != null && s.state(KEY).usedCorpses().contains(corpse.id()))
            throw new IllegalArgumentException("Corpse already used");
        long id = Math.addExact(s.pieces().keySet().stream().mapToLong(Long::longValue).max().orElse(0), 1);
        var owner = corpse == null ? s.pieces().get(caster) : corpse;
        var base = new PieceState(id, body.type(), owner.color(), square, PieceState.Status.BOARD, hero, cooldown, 0,
                List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), null, 0, startingColor);
        var origin = corpse == null ? new PieceCreationContext(square, s.pieces().get(caster)) : PieceCreationContext.revival(square, corpse);
        var piece = new HeroPieceFactory(s.heroes()).create(base, origin);
        var birth = new PieceBirth(id, caster, corpse == null ? null : corpse.id(), s.turn(), hero, body,
                piece.startingColor(), piece.cooldown(), square, s.request().ability().id(), evolution);
        var births = new HashMap<>(s.state(KEY).births());
        births.put(id, birth);
        s.state(KEY, new State(births, s.state(KEY).dismissed(), s.state(KEY).evolved()));
        s.update(Map.of(id, piece));
        s.propose(new MechanicEventDraft(s.turn(), s.pieces().get(caster).color(), caster, id,
                s.before().pieceById(caster).position(), square, corpse == null ? "Создан объект" : "Фигура вернулась в игру",
                BORN.event(birth)));
        return piece;
    }
    public static void dismiss(MechanicSession s, long id, boolean journal) {
        var piece = s.pieces().get(id);
        if (piece == null || piece.isRemoved()) return;
        s.update(Map.of(id, piece.hidden(PieceState.Status.REPLACED)));
        var dismissed = new HashSet<>(s.state(KEY).dismissed());
        dismissed.add(id);
        s.state(KEY, new State(s.state(KEY).births(), dismissed, s.state(KEY).evolved()));
        if (journal) s.propose(new MechanicEventDraft(s.turn(), piece.color(), id, null, piece.position(), piece.position(),
                "Закончился срок созданного объекта", REMOVED.event(new Removed(id))));
    }
    @Override public List<MechanicStep> steps() {
        return List.of(update("creation.canEnd", CAN_END_SETTLED, null, s -> evolve(s, false)),
                update("creation.evolve", END_TURN_SETTLED, null, s -> evolve(s, true)));
    }
    private static long deadline(PieceBirth birth, FigureColor owner, FigureColor activeAtBirth) {
        return Math.addExact(birth.turn(), 2L * birth.evolution().ownTurns() - (owner == activeAtBirth ? 0 : 1));
    }
    private static void evolve(MechanicSession s, boolean journal) {
        var state = s.state(KEY);
        var evolved = new HashSet<>(state.evolved());
        for (var birth : state.births().values().stream().sorted(java.util.Comparator.comparingLong(PieceBirth::id)).toList()) {
            if (birth.evolution() == null || evolved.contains(birth.id())) continue;
            var piece = s.pieces().get(birth.id());
            if (piece == null || piece.isRemoved() || piece.position() == null || piece.isPotted()
                    || piece.color() != s.side() || deadline(birth, piece.color(), s.pieces().get(birth.caster()).color()) > s.turn()) continue;
            var shape = birth.evolution();
            var base = new PieceState(piece.id(), shape.body(), piece.color(), piece.position(), piece.status(), shape.hero(),
                    0, 0, piece.inventory(), null, piece.blindTurns(), null, piece.stunnedThroughTurn(), piece.boxedThroughTurn(), false);
            var changed = new HeroPieceFactory(s.heroes()).create(base, new PieceCreationContext(piece.position(), piece));
            s.update(Map.of(changed.id(), changed));
            evolved.add(changed.id());
            if (journal) s.propose(new MechanicEventDraft(s.turn(), piece.color(), piece.id(), null, piece.position(), piece.position(),
                    "Завершилось превращение созданной фигуры", EVOLVED.event(new Evolution(piece.id(), piece.hero(),
                            CreatedBody.of(piece.type()), changed.hero(), CreatedBody.of(changed.type())))));
        }
        s.state(KEY, new State(state.births(), s.state(KEY).dismissed(), evolved));
    }
    @Override public PieceBirth birth(GameSnapshot snapshot, PieceState piece) { return snapshot.mechanics().get(KEY).births().get(piece.id()); }
    @Override public boolean dismissed(HeroActionContext context, PieceState piece) { return context.state(KEY).dismissed().contains(piece.id()); }
    @Override public boolean dismissed(GameSnapshot snapshot, PieceState piece) { return snapshot.mechanics().get(KEY).dismissed().contains(piece.id()); }
    @Override public List<PieceHeroChange> heroChanges(GameEvent e) {
        if (e.isEffect(BORN)) { var p = BORN.type().cast(e.effect().data()); return List.of(new PieceHeroChange(p.id(), HeroType.NONE, p.hero())); }
        if (e.isEffect(EVOLVED)) { var p = EVOLVED.type().cast(e.effect().data()); return List.of(new PieceHeroChange(p.id(), p.beforeHero(), p.hero())); }
        return List.of();
    }
    @Override public List<PieceBodyChange> bodyChanges(GameEvent e) {
        if (!e.isEffect(EVOLVED)) return List.of();
        var p = EVOLVED.type().cast(e.effect().data());
        return List.of(new PieceBodyChange(p.id(), p.beforeBody().type(), p.body().type()));
    }
    @Override public State restore(GameSnapshot snapshot) {
        var births = new HashMap<Long, PieceBirth>();
        var dismissed = new HashSet<Long>();
        var evolved = new HashSet<Long>();
        var corpses = new HashSet<Long>();
        for (var e : snapshot.events()) {
            if (e.isEffect(BORN)) {
                var p = BORN.type().cast(e.effect().data());
                if (p.id() != e.targetId() || p.caster() != e.actorId() || p.turn() != e.turn() || !p.square().equals(e.to())
                        || births.putIfAbsent(p.id(), p) != null || p.corpse() != null && !corpses.add(p.corpse()))
                    throw new IllegalArgumentException("Invalid creation journal");
                if (snapshot.events().stream().noneMatch(c -> c.sequence() < e.sequence() && c.turn() == e.turn()
                        && Objects.equals(c.actorId(), p.caster()) && p.ability().equals(c.abilityId()) && Objects.equals(c.from(), e.from())))
                    throw new IllegalArgumentException("Creation without its ability");
            }
            if (e.isEffect(REMOVED)) {
                long id = REMOVED.type().cast(e.effect().data()).id();
                if (id != e.actorId() || !births.containsKey(id) || !dismissed.add(id))
                    throw new IllegalArgumentException("Dismissal without origin");
            }
            if (e.isEffect(EVOLVED)) {
                var p = EVOLVED.type().cast(e.effect().data());
                var birth = births.get(p.id());
                if (birth == null || birth.evolution() == null || p.id() != e.actorId() || !evolved.add(p.id())
                        || p.beforeHero() != birth.hero() || p.hero() != birth.evolution().hero() || p.body().type() != birth.evolution().body()
                        || e.turn() < deadline(birth, snapshot.pieces().get(p.id()).color(), snapshot.pieces().get(birth.caster()).color())
                        || snapshot.events().stream().noneMatch(end -> end.kind() == GameEvent.Kind.END_TURN && end.turn() == e.turn()))
                    throw new IllegalArgumentException("Invalid created life cycle");
            }
        }
        return new State(births, dismissed, evolved);
    }
    @Override public void validate(GameSnapshot snapshot) {
        var state = snapshot.mechanics().get(KEY);
        if (!state.equals(restore(snapshot))) throw new IllegalArgumentException("Creation state differs from history");
        if (snapshot.mode() == GameMode.CLASSIC && !state.equals(KEY.initial())) throw new IllegalArgumentException("Created pieces in Classic");
        var heroes = HeroRegistry.fromRules(snapshot.heroRules());
        var context = new HeroActionContext(snapshot.field(), snapshot.pieces(), snapshot.turn().number(), snapshot.mechanics(), heroes.mechanics());
        var bodies = PieceBodyHistory.forPieces(snapshot, state.births().keySet());
        for (var birth : state.births().values()) {
            var piece = snapshot.pieces().get(birth.id());
            var caster = snapshot.pieces().get(birth.caster());
            if (piece == null || caster == null || birth.turn() > snapshot.turn().number()) throw new IllegalArgumentException("Unknown creation identity");
            if (piece.type() != bodies.latest(piece.id()) || piece.startingColor() != birth.startingColor())
                throw new IllegalArgumentException("Created figure differs from its body history");
            var initial = new PieceState(piece.id(), birth.body().type(), piece.color(), birth.square(), PieceState.Status.BOARD,
                    birth.hero(), birth.cooldown(), 0, List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), null, 0, birth.startingColor());
            heroes.validateState(initial);
            if (birth.corpse() != null) {
                var corpse = snapshot.pieces().get(birth.corpse());
                if (!eligibleCorpse(corpse, snapshot.field()) || corpse.color() != piece.color() || birth.hero() != corpse.hero())
                    throw new IllegalArgumentException("Invalid corpse source");
                var body = context.registry().revivalBody(context, corpse);
                if (birth.body().type() != (body == null ? corpse.type() : body) || birth.startingColor() != corpse.startingColor())
                    throw new IllegalArgumentException("Revival differs from the deceased figure");
                if (!Objects.equals(birth.evolution(), heroes.revival(corpse, context).evolution()))
                    throw new IllegalArgumentException("Revival life cycle differs from the hero policy");
                long bornAt = snapshot.events().stream().filter(e -> e.isEffect(BORN)
                        && BORN.type().cast(e.effect().data()).id() == birth.id()).findFirst().orElseThrow().sequence();
                if (snapshot.events().stream().noneMatch(e -> e.sequence() < bornAt
                        && Objects.equals(e.targetId(), corpse.id()) && isDeath(e)))
                    throw new IllegalArgumentException("Revival before death");
            } else if (piece.color() != caster.color()) throw new IllegalArgumentException("Spawned object changed side");
            if (state.dismissed().contains(birth.id()) && piece.status() != PieceState.Status.REPLACED)
                throw new IllegalArgumentException("Dismissed identity is still present");
            if (birth.evolution() != null && !state.evolved().contains(birth.id()) && !piece.isRemoved() && !piece.isPotted()
                    && deadline(birth, piece.color(), caster.color()) < snapshot.turn().number()) throw new IllegalArgumentException("Overdue life cycle");
        }
        for (var piece : snapshot.pieces().values()) if (piece.type() == Ward.class && !state.births().containsKey(piece.id()))
            throw new IllegalArgumentException("Object without creation origin");
    }
    public static boolean eligibleCorpse(PieceState piece, Field field) {
        return piece != null && (piece.status() == PieceState.Status.CAPTURED || piece.status() == PieceState.Status.DESTROYED)
                && piece.hero() != HeroType.ILLUSION && field.createFigure(piece.type(), new Position(0, 0), piece.color()).canBeResurrected();
    }
    private static boolean isDeath(GameEvent e) {
        return Set.of(GameEvent.Kind.MOVE, GameEvent.Kind.CAPTURE, GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE,
                GameEvent.Kind.ARES_REVENGE, GameEvent.Kind.MINE_CAPTURE, GameEvent.Kind.BLAST_CAPTURE, GameEvent.Kind.BLAST_DEATH,
                GameEvent.Kind.LIGHTNING_CAPTURE).contains(e.legacyKind());
    }
    private static Map<String, String> encodeBirth(PieceBirth p) {
        var fields = new HashMap<String, String>();
        fields.put("id", Long.toString(p.id())); fields.put("caster", Long.toString(p.caster()));
        fields.put("corpse", p.corpse() == null ? "" : p.corpse().toString()); fields.put("turn", Long.toString(p.turn()));
        fields.put("hero", p.hero().name()); fields.put("body", p.body().name());
        fields.put("startingColor", p.startingColor() == null ? "" : p.startingColor().name());
        fields.put("cooldown", Integer.toString(p.cooldown())); fields.put("row", Integer.toString(p.square().getRow()));
        fields.put("column", Integer.toString(p.square().getColumn())); fields.put("ability", p.ability());
        fields.put("nextHero", p.evolution() == null ? "" : p.evolution().hero().name());
        fields.put("nextBody", p.evolution() == null ? "" : CreatedBody.of(p.evolution().body()).name());
        fields.put("ownTurns", p.evolution() == null ? "" : Integer.toString(p.evolution().ownTurns()));
        return Map.copyOf(fields);
    }
    private static PieceBirth decodeBirth(Map<String, String> data) {
        fields(data,"id","caster","corpse","turn","hero","body","startingColor","cooldown","row","column","ability","nextHero","nextBody","ownTurns");
        var next = data.get("nextHero").isEmpty() ? null : new RevivalPlan.Evolution(HeroType.valueOf(data.get("nextHero")),
                CreatedBody.valueOf(data.get("nextBody")).type(), Integer.parseInt(data.get("ownTurns")));
        return new PieceBirth(Long.parseLong(data.get("id")), Long.parseLong(data.get("caster")),
                data.get("corpse").isEmpty() ? null : Long.parseLong(data.get("corpse")), Long.parseLong(data.get("turn")),
                HeroType.valueOf(data.get("hero")), CreatedBody.valueOf(data.get("body")),
                data.get("startingColor").isEmpty() ? null : SquareColor.valueOf(data.get("startingColor")),
                Integer.parseInt(data.get("cooldown")), new Position(Integer.parseInt(data.get("row")), Integer.parseInt(data.get("column"))), data.get("ability"), next);
    }
    private static void fields(Map<String, String> data, String... names) {
        if (!data.keySet().equals(Set.of(names))) throw new IllegalArgumentException("Invalid creation fields");
    }
}
