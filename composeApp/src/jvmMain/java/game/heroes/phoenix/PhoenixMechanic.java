package game.heroes.phoenix;

import game.heroes.HeroActionContext;
import game.heroes.HeroRegistry;
import game.heroes.HeroPieceFactory;
import game.heroes.HeroType;
import game.heroes.PieceCreationContext;
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
import game.mechanics.PieceBodyChange;
import game.mechanics.PieceHeroChange;
import game.mechanics.PieceHeroHistory;
import game.mechanics.compat.HistoricalMechanics;
import game.moves.Capture;
import game.moves.Success;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import model.Figure;
import model.figures.Archbishop;
import model.figures.Bishop;
import model.figures.Egg;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.CAN_END;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.ENTRY;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.update;

/** Яйцо и происхождение вылупившегося героя восстанавливаются из типизированного журнала. */
public final class PhoenixMechanic implements MechanicDefinition<PhoenixMechanic.State> {
    public enum Body {
        KNIGHT(Knight.class), BISHOP(Bishop.class), ROOK(Rook.class);
        private final Class<? extends Figure> type;
        Body(Class<? extends Figure> type) { this.type = type; }
        public Class<? extends Figure> type() { return type; }
        static Body of(Class<? extends Figure> type) {
            for (Body body : values()) if (body.type == type) return body;
            throw new IllegalArgumentException("Unsupported incubation body");
        }
    }
    public record Incubation(HeroType hero, Body body, long until) {
        public Incubation {
            if ((hero != HeroType.PHOENIX && hero != HeroType.GINSENG) || body == null || until < 1)
                throw new IllegalArgumentException("Invalid incubation");
        }
    }
    public record State(Map<Long, Incubation> eggs, Map<Long, HeroType> origins, Map<Long, Incubation> shells) {
        public State(Map<Long, Incubation> eggs, Map<Long, HeroType> origins) { this(eggs, origins, eggs); }
        public State { eggs = Map.copyOf(eggs); origins = Map.copyOf(origins); shells = Map.copyOf(shells); }
    }
    public record Incubate(long actor) implements MechanicOperation {}
    public record Return(long victim) {}
    private enum Handled { NO, YES }
    private static final MechanicKey<Handled> HANDLED = new MechanicKey<>("phoenix.returned", Handled.class, Handled.NO);
    public static final MechanicKey<State> KEY = new MechanicKey<>("phoenix", State.class, new State(Map.of(), Map.of()));
    public static final PhoenixMechanic INSTANCE = new PhoenixMechanic();
    public static final MechanicEventType<Incubation> EGG = incubationEvent("phoenix.egg", true, true);
    public static final MechanicEventType<Incubation> HATCH = incubationEvent("phoenix.hatch", false, true);
    public static final MechanicEventType<Incubation> LOST = incubationEvent("phoenix.lost", false, false);
    public static final MechanicEventType<Return> RETURN = new MechanicEventType<>("phoenix.return", Return.class,
            p -> Map.of("victim", Long.toString(p.victim())), data -> {
                if (!data.keySet().equals(Set.of("victim"))) throw new IllegalArgumentException("Invalid Phoenix return");
                return new Return(Long.parseLong(data.get("victim")));
            }, new MechanicEventPolicy(true, true, true, true));

    private PhoenixMechanic() {}
    private static MechanicEventType<Incubation> incubationEvent(String id, boolean active, boolean squares) {
        return new MechanicEventType<>(id, Incubation.class,
                p -> Map.of("hero", p.hero().name(), "body", p.body().name(), "until", Long.toString(p.until())),
                data -> {
                    if (!data.keySet().equals(Set.of("hero", "body", "until")))
                        throw new IllegalArgumentException("Invalid incubation fields");
                    return new Incubation(HeroType.valueOf(data.get("hero")), Body.valueOf(data.get("body")),
                            Long.parseLong(data.get("until")));
                }, new MechanicEventPolicy(active, true, false, squares));
    }
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicDefinition<?>> dependencies() { return List.of(HistoricalMechanics.INSTANCE); }
    @Override public List<MechanicEventType<?>> events() { return List.of(EGG, HATCH, LOST, RETURN); }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Incubate.class, "phoenix.incubate", ABILITY_PREPARE, "spells.cast", (s, op) -> {
            PieceState actor = s.pieces().get(op.actor());
            var record = new Incubation(actor.hero(), Body.of(actor.type()),
                    Math.addExact(s.turn(), 2L * s.rules().get(PhoenixDefinition.INSTANCE).eggTurns()));
            var eggs = new HashMap<>(s.state(KEY).eggs());
            if (eggs.putIfAbsent(actor.id(), record) != null) throw new IllegalArgumentException("Already incubating");
            var shells = new HashMap<>(s.state(KEY).shells());
            shells.put(actor.id(), record);
            s.state(KEY, new State(eggs, s.state(KEY).origins(), shells));
            s.update(Map.of(actor.id(), actor.placed(actor.position(), Egg.class)));
            propose(s, actor, EGG, record, "Phoenix: появилось яйцо");
            return Success.SUCCESS;
        }));
    }
    @Override public List<MechanicStep> steps() {
        return List.of(update("phoenix.return", ENTRY, "summons.entry", PhoenixMechanic::returnAfterPawn),
                update("phoenix.canEnd", CAN_END, "status.canEnd", s -> mature(s, false)),
                update("phoenix.mature", END_TURN, "status.blindness", s -> mature(s, true)));
    }
    private static void returnAfterPawn(MechanicSession s) {
        if (s.movement() == null || s.local(HANDLED) == Handled.YES) return;
        s.local(HANDLED, Handled.YES);
        var move = s.movement();
        PieceState original = s.before().pieceById(move.actor().id());
        if (original == null || original.hero() != HeroType.PHOENIX || original.type() != Knight.class
                || original.isPotted() || s.interceptedMove()) return;
        var capture = s.captures().stream().filter(c -> c.actorId() == original.id()
                && c.cause() == Capture.Cause.MOVE && c.victim() instanceof Pawn).findFirst().orElse(null);
        PieceState actor = s.pieces().get(original.id());
        if (capture == null || actor.status() != PieceState.Status.BOARD) return;
        // Реакция на клетку взятия уже разрешена; погибший Phoenix не возвращается.
        if (s.context().pieceAt(move.from()) != null) return;
        s.update(Map.of(actor.id(), actor.placed(move.from(), actor.type())));
        s.propose(new MechanicEventDraft(s.turn(), actor.color(), actor.id(), capture.victimId(), move.to(), move.from(),
                "Phoenix: возврат после взятия пешки", RETURN.event(new Return(capture.victimId()))));
    }
    private static void mature(MechanicSession s, boolean journal) {
        var state = s.state(KEY);
        var eggs = new HashMap<>(state.eggs());
        var origins = new HashMap<>(state.origins());
        for (var entry : state.eggs().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            PieceState egg = s.pieces().get(entry.getKey());
            var record = entry.getValue();
            if (egg.isRemoved()) {
                eggs.remove(egg.id());
                if (journal) propose(s, egg, LOST, record, "Phoenix: яйцо погибло");
            } else if (egg.status() == PieceState.Status.BOARD && egg.color() == s.side() && !egg.isPotted() && record.until() <= s.turn()) {
                var base = new PieceState(egg.id(), Archbishop.class, egg.color(), egg.position(), egg.status(),
                        HeroType.SUPERNOVA, 0, 0, egg.inventory(), egg.storedPieceId(), egg.blindTurns(),
                        egg.startingColor(), egg.stunnedThroughTurn(), egg.boxedThroughTurn(), false);
                PieceState hatch = new HeroPieceFactory(s.heroes()).create(base,
                        new PieceCreationContext(egg.position(), egg));
                s.update(Map.of(hatch.id(), hatch));
                eggs.remove(egg.id());
                origins.put(egg.id(), record.hero());
                if (journal) propose(s, hatch, HATCH, record, "Из яйца вылупилась Supernova");
            }
        }
        s.state(KEY, new State(eggs, origins, state.shells()));
    }
    private static void propose(MechanicSession s, PieceState actor, MechanicEventType<Incubation> type,
            Incubation data, String message) {
        s.propose(new MechanicEventDraft(s.turn(), actor.color(), actor.id(), null,
                actor.position(), actor.position(), message, type.event(data)));
    }
    @Override public Class<? extends Figure> revivalBody(HeroActionContext context, PieceState corpse) {
        var shell = context.state(KEY).shells().get(corpse.id());
        return corpse.type() == Egg.class && shell != null ? shell.body().type() : null;
    }
    @Override public HeroType initialHero(GameSnapshot snapshot, PieceState piece) {
        return snapshot.mechanics().get(KEY).origins().get(piece.id());
    }
    @Override public List<PieceHeroChange> heroChanges(GameEvent event) {
        return event.isEffect(HATCH) ? List.of(new PieceHeroChange(event.actorId(),
                HATCH.type().cast(event.effect().data()).hero(), HeroType.SUPERNOVA)) : List.of();
    }
    @Override public List<PieceBodyChange> bodyChanges(GameEvent event) {
        if (event.isEffect(EGG)) return List.of(new PieceBodyChange(event.actorId(),
                EGG.type().cast(event.effect().data()).body().type(), Egg.class));
        if (event.isEffect(HATCH)) return List.of(new PieceBodyChange(event.actorId(), Egg.class, Archbishop.class));
        return List.of();
    }
    @Override public State restore(GameSnapshot snapshot) {
        var eggs = new HashMap<Long, Incubation>();
        var origins = new HashMap<Long, HeroType>();
        var shells = new HashMap<Long, Incubation>();
        for (var event : snapshot.events()) {
            if (event.isEffect(EGG)) {
                var record = EGG.type().cast(event.effect().data());
                shells.put(event.actorId(), record);
                if (eggs.putIfAbsent(event.actorId(), record) != null
                        || record.until() != event.turn() + 2L * snapshot.heroRules().get(PhoenixDefinition.INSTANCE).eggTurns()
                        || snapshot.events().stream().noneMatch(e -> e.sequence() < event.sequence() && e.turn() == event.turn()
                            && e.isAbility(PhoenixAbility.REBIRTH) && Objects.equals(e.actorId(), event.actorId())))
                    throw new IllegalArgumentException("Invalid egg origin");
            }
            if (event.isEffect(HATCH) || event.isEffect(LOST)) {
                var type = event.isEffect(HATCH) ? HATCH : LOST;
                var record = type.type().cast(event.effect().data());
                if (!record.equals(eggs.remove(event.actorId()))) throw new IllegalArgumentException("Unpaired egg ending");
                if (type == HATCH) {
                    if (event.turn() != record.until() || snapshot.events().stream().noneMatch(e ->
                            e.kind() == GameEvent.Kind.END_TURN && e.turn() == event.turn() && e.color() == event.color()))
                        throw new IllegalArgumentException("Hatch before expiry or outside completed turn");
                    origins.put(event.actorId(), record.hero());
                }
            }
        }
        return new State(eggs, origins, shells);
    }
    @Override public void validate(GameSnapshot snapshot) {
        var state = snapshot.mechanics().get(KEY);
        if (!state.equals(restore(snapshot))) throw new IllegalArgumentException("Egg state differs from history");
        var identities = PieceHeroHistory.replay(snapshot);
        for (var piece : snapshot.pieces().values()) {
            if (piece.hero() != identities.latest(piece.id())) throw new IllegalArgumentException("Hero differs from history");
            if (piece.type() == Egg.class && !piece.isRemoved() && !state.eggs().containsKey(piece.id()))
                throw new IllegalArgumentException("Egg without incubation");
            if (piece.hero() == HeroType.SUPERNOVA && !state.origins().containsKey(piece.id())
                    && HeroRegistry.fromRules(snapshot.heroRules()).mechanics().birth(snapshot,piece) == null)
                throw new IllegalArgumentException("Supernova without hatching");
        }
        var ids = new java.util.HashSet<>(state.origins().keySet());
        ids.addAll(state.eggs().keySet());
        snapshot.pieces().values().stream().filter(p -> p.hero() == HeroType.PHOENIX).map(PieceState::id).forEach(ids::add);
        var bodies = game.mechanics.PieceBodyHistory.forPieces(snapshot, ids);
        for (long id : ids) if (bodies.latest(id) != snapshot.pieces().get(id).type())
            throw new IllegalArgumentException("Incubation body differs from history");
        for (var entry : state.eggs().entrySet()) {
            var piece = snapshot.pieces().get(entry.getKey());
            if (piece == null || piece.type() != Egg.class || piece.hero() != entry.getValue().hero())
                throw new IllegalArgumentException("Incubation differs from piece");
            if (!piece.isRemoved() && !piece.isPotted() && entry.getValue().until() < snapshot.turn().number())
                throw new IllegalArgumentException("Overdue egg");
        }
        for (var event : snapshot.events()) {
            if (event.isEffect(EGG) && identities.before(event.actorId(), event.sequence())
                    != EGG.type().cast(event.effect().data()).hero())
                throw new IllegalArgumentException("Incubation identity differs from history");
            if (event.isAbility(PhoenixAbility.REBIRTH) && snapshot.events().stream().filter(e -> e.isEffect(EGG)
                    && e.turn() == event.turn() && Objects.equals(e.actorId(), event.actorId())).count() != 1)
                throw new IllegalArgumentException("Rebirth without one egg");
            if (event.isEffect(RETURN)) {
                var data = RETURN.type().cast(event.effect().data());
                if (identities.before(event.actorId(), event.sequence()) != HeroType.PHOENIX
                        || data.victim() != event.targetId() || snapshot.events().stream().noneMatch(e ->
                        e.kind() == GameEvent.Kind.MOVE && e.turn() == event.turn() && Objects.equals(e.actorId(), event.actorId())
                            && Objects.equals(e.targetId(), event.targetId()) && e.from().equals(event.to()) && e.to().equals(event.from()))
                        || snapshot.pieces().get(data.victim()).type() != Pawn.class)
                    throw new IllegalArgumentException("Return without matching pawn capture");
            }
        }
    }
}
