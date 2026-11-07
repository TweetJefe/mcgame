package game.heroes.sunwukong;

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
import game.mechanics.MechanicStateCodec;
import game.mechanics.MechanicStep;
import game.mechanics.PieceBodyChange;
import game.mechanics.compat.HistoricalMechanics;
import game.moves.Success;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.summoning.SummonState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import model.Figure;
import model.Position;
import model.figures.Pawn;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.CAN_END;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.SETUP;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.update;

/** Временная форма и происхождение копий принадлежат настоящим ID, включая Ginseng. */
public final class SunWukongMechanic implements MechanicDefinition<WukongState> {
    public static final SunWukongMechanic INSTANCE = new SunWukongMechanic();
    public static final MechanicKey<WukongState> KEY = new MechanicKey<>("sunwukong", WukongState.class, WukongState.empty());
    public record Shapeshift(long actor, boolean borrowed) implements MechanicOperation {}
    public record Clone(long actor, long target) implements MechanicOperation {}
    public record Shift(WukongState.Form form, long randomState) {}
    public record Revert(WukongState.Body before, WukongState.Body after) {}
    public record Cloned(long source, long target) {}

    public static final MechanicEventType<Shift> SHIFT = new MechanicEventType<>("sunwukong.shift", Shift.class,
            p -> Map.of("original", p.form().original().name(), "chosen", p.form().chosen().name(),
                    "until", Long.toString(p.form().until()), "randomState", Long.toString(p.randomState())),
            data -> {
                fields(data, "original", "chosen", "until", "randomState");
                return new Shift(new WukongState.Form(WukongState.Body.valueOf(data.get("original")),
                        WukongState.Body.valueOf(data.get("chosen")), Long.parseLong(data.get("until"))),
                        Long.parseLong(data.get("randomState")));
            });
    public static final MechanicEventType<Revert> REVERT = new MechanicEventType<>("sunwukong.revert", Revert.class,
            p -> Map.of("before", p.before().name(), "after", p.after().name()),
            data -> {
                fields(data, "before", "after");
                return new Revert(WukongState.Body.valueOf(data.get("before")), WukongState.Body.valueOf(data.get("after")));
            }, new MechanicEventPolicy(false, true, false, false));
    public static final MechanicEventType<Cloned> CLONED = new MechanicEventType<>("sunwukong.cloned", Cloned.class,
            p -> Map.of("source", Long.toString(p.source()), "target", Long.toString(p.target())),
            data -> {
                fields(data, "source", "target");
                return new Cloned(Long.parseLong(data.get("source")), Long.parseLong(data.get("target")));
            }, new MechanicEventPolicy(true, true, true, true));

    private SunWukongMechanic() {}
    private static void fields(Map<String, String> data, String... names) {
        if (!data.keySet().equals(Set.of(names))) throw new IllegalArgumentException("Invalid Wukong event fields");
    }
    @Override public MechanicKey<WukongState> key() { return KEY; }
    @Override public List<MechanicDefinition<?>> dependencies() { return List.of(HistoricalMechanics.INSTANCE); }
    @Override public List<MechanicEventType<?>> events() { return List.of(SHIFT, REVERT, CLONED); }
    @Override public MechanicStateCodec<WukongState> codec() { return new WukongStateCodec(); }

    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Shapeshift.class, "wukong.shift", ABILITY_PREPARE, "spells.cast", (s, op) -> {
            PieceState actor = s.pieces().get(op.actor());
            var state = s.state(KEY);
            var choice = SummonState.initial(state.randomState()).choose(3);
            var body = List.of(WukongState.Body.KNIGHT, WukongState.Body.BISHOP, WukongState.Body.ROOK).get(choice.index());
            var rules = s.rules().get(SunWukongDefinition.INSTANCE);
            var form = new WukongState.Form(WukongState.Body.of(actor.type()), body,
                    Math.addExact(s.turn(), 2L * rules.shapeshiftTurns()));
            var forms = new HashMap<>(state.forms());
            forms.put(actor.id(), form);
            s.state(KEY, new WukongState(state.initialSeed(), choice.nextState(), forms, state.clones()));
            s.update(Map.of(actor.id(), withBody(actor, body.type())));
            // Стоимость уже применена базой; здесь только синхронизация общей перезарядки.
            if (!op.borrowed()) for (var piece : s.pieces().values())
                if (piece.hero() == HeroType.SUN_WUKONG && piece.color() == actor.color() && !piece.isRemoved())
                    s.update(Map.of(piece.id(), piece.withAbilityState(actor.cooldown(), piece.ultimateCharge(), piece.storedPieceId())));
            s.propose(new MechanicEventDraft(s.turn(), actor.color(), actor.id(), null, actor.position(), actor.position(),
                    "Sun Wukong: временная форма", SHIFT.event(new Shift(form, choice.nextState()))));
            return Success.SUCCESS;
        }), handler(Clone.class, "wukong.clone", ABILITY_PREPARE, "wukong.shift", (s, op) -> {
            PieceState actor = s.pieces().get(op.actor()), target = s.pieces().get(op.target());
            int cooldown = s.pieces().values().stream().filter(p -> p.hero() == HeroType.SUN_WUKONG
                    && p.color() == actor.color() && !p.isRemoved()).mapToInt(PieceState::cooldown).max().orElse(0);
            var base = new PieceState(target.id(), Pawn.class, target.color(), target.position(), target.status(),
                    HeroType.SUN_WUKONG, cooldown, 0, target.inventory(), null, target.blindTurns(), null,
                    target.stunnedThroughTurn(), target.boxedThroughTurn(), target.potted());
            var clone = new HeroPieceFactory(s.heroes()).create(base, new PieceCreationContext(target.position(), actor));
            var state = s.state(KEY);
            var clones = new HashMap<>(state.clones());
            clones.put(target.id(), new WukongState.Origin(actor.id(), s.turn(), target.position()));
            s.state(KEY, new WukongState(state.initialSeed(), state.randomState(), state.forms(), clones));
            s.update(Map.of(target.id(), clone));
            s.propose(new MechanicEventDraft(s.turn(), actor.color(), actor.id(), target.id(), actor.position(), target.position(),
                    "Sun Wukong: появился клон", CLONED.event(new Cloned(actor.id(), target.id()))));
            return Success.SUCCESS;
        }));
    }

    @Override public List<MechanicStep> steps() {
        return List.of(update("wukong.seed", SETUP, "summons.initialize", s -> {
            if (s.pieces().values().stream().anyMatch(p -> p.hero() == HeroType.SUN_WUKONG)) {
                long seed = ThreadLocalRandom.current().nextLong();
                s.state(KEY, new WukongState(seed, seed, Map.of(), Map.of()));
            }
        }), update("wukong.canEnd", CAN_END, "status.canEnd", s -> expire(s, false)),
            update("wukong.expire", END_TURN, "status.blindness", s -> expire(s, true)));
    }

    private static void expire(MechanicSession session, boolean journal) {
        var state = session.state(KEY);
        var forms = new HashMap<>(state.forms());
        for (var entry : state.forms().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            if (entry.getValue().until() > session.turn()) continue;
            PieceState piece = session.pieces().get(entry.getKey());
            WukongState.Body before = WukongState.Body.of(piece.type());
            WukongState.Body after = piece.isRemoved() || !before.isClassic() ? before : entry.getValue().original();
            session.update(Map.of(piece.id(), withBody(piece, after.type())));
            forms.remove(piece.id());
            if (journal) session.propose(new MechanicEventDraft(session.turn(), piece.color(), piece.id(), null,
                    piece.position(), piece.position(), "Sun Wukong: временная форма закончилась", REVERT.event(new Revert(before, after))));
        }
        session.state(KEY, new WukongState(state.initialSeed(), state.randomState(), forms, state.clones()));
    }

    static PieceState withBody(PieceState piece, Class<? extends Figure> body) {
        return new PieceState(piece.id(), body, piece.color(), piece.position(), piece.status(), piece.hero(),
                piece.cooldown(), piece.ultimateCharge(), piece.inventory(), piece.storedPieceId(), piece.blindTurns(),
                piece.startingColor(), piece.stunnedThroughTurn(), piece.boxedThroughTurn(), piece.potted());
    }

    @Override public HeroType initialHero(GameSnapshot snapshot, PieceState piece) {
        return snapshot.mechanics().get(KEY).clones().containsKey(piece.id()) ? HeroType.NONE : null;
    }
    @Override public List<game.mechanics.PieceHeroChange> heroChanges(GameEvent event) {
        return event.isEffect(CLONED) ? List.of(new game.mechanics.PieceHeroChange(event.targetId(),
                HeroType.NONE, HeroType.SUN_WUKONG)) : List.of();
    }
    @Override public List<PieceBodyChange> bodyChanges(GameEvent event) {
        if (event.isEffect(SHIFT)) {
            var form = SHIFT.type().cast(event.effect().data()).form();
            return List.of(new PieceBodyChange(event.actorId(), form.original().type(), form.chosen().type()));
        }
        if (event.isEffect(REVERT)) {
            var change = REVERT.type().cast(event.effect().data());
            return List.of(new PieceBodyChange(event.actorId(), change.before().type(), change.after().type()));
        }
        return List.of();
    }

    @Override public WukongState restore(GameSnapshot snapshot) {
        long seed = snapshot.events().stream().filter(e -> e.isEffect(SHIFT))
                .map(e -> SHIFT.type().cast(e.effect().data()).randomState() - 0x9e3779b97f4a7c15L)
                .findFirst().orElse(0L);
        return WukongHistory.replay(snapshot, seed);
    }
    @Override public void validate(GameSnapshot snapshot) { WukongHistory.validate(snapshot); }
}
