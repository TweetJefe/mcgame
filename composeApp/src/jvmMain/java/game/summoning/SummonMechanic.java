package game.summoning;

import game.mechanics.PieceRestriction;

import game.heroes.HeroType;
import game.heroes.clownganger.ClownMechanic;
import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicStep;
import game.mechanics.compat.HistoricalEvents;
import game.moves.Success;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static game.mechanics.BuiltInStates.SUMMONS;
import static game.mechanics.BuiltInStates.TERRAIN;
import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.ENTRY;
import static game.mechanics.MechanicPhase.SETUP;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.step;
import static game.mechanics.Mechanics.update;

public final class SummonMechanic implements MechanicDefinition<SummonState> {
    public static final SummonMechanic INSTANCE = new SummonMechanic();
    private SummonMechanic() {}
    public record Cast(PieceState actor) implements MechanicOperation {}
    private record Handled(Set<Long> ids) { private Handled { ids = Set.copyOf(ids); } }
    private static final MechanicKey<Handled> HANDLED = new MechanicKey<>("summons.handled", Handled.class, new Handled(Set.of()));
    @Override public boolean legacyWireState() { return true; }
    @Override public MechanicKey<SummonState> key() { return SUMMONS; }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Cast.class, "summons.cast", ABILITY_PREPARE, "terrain.place", (s, op) -> {
            var result = SummonResolver.cast(s.pieces(), s.state(SUMMONS), op.actor(), s.turn(), s.side(), s.rules(), s.state(TERRAIN), s.heroes(),
                    piece -> !s.context().blocks(piece, PieceRestriction.CAPTURE));
            if (!result.result().isSuccess()) return result.result();
            s.pieces(result.pieces());
            s.state(SUMMONS, result.summons());
            return Success.SUCCESS;
        }));
    }
    @Override public List<MechanicStep> steps() {
        return List.of(update("summons.initialize", SETUP, "historical.assignments", s -> {
            boolean needed = s.assignments().containsValue(HeroType.IZBA) || s.assignments().containsValue(HeroType.GINSENG)
                    || s.assignments().containsValue(HeroType.CLOWNGANGER) || s.assignments().containsValue(HeroType.MOLEMINER)
                    || s.assignments().containsValue(HeroType.ZEUS);
            if (needed && !s.state(SUMMONS).enabled())
                s.state(SUMMONS, SummonState.initial(java.util.concurrent.ThreadLocalRandom.current().nextLong()));
        }), step("summons.entry", ENTRY, "mines.blast", s -> {
            var handled = new HashSet<>(s.local(HANDLED).ids());
            var result = SummonResolver.resolve(s.before().field(), s.pieces(), s.state(SUMMONS), s.state(TERRAIN), s.captures(), s.turn(), s.side(), s.rules(), handled, s);
            if (!result.result().isSuccess()) return result.result();
            s.local(HANDLED, new Handled(handled));
            s.pieces(result.pieces());
            s.state(TERRAIN, result.terrain());
            s.state(SUMMONS, result.summons());
            s.captures(result.captures());
            ClownMechanic.hits(s, result.illusionHits());
            return Success.SUCCESS;
        }), update("summons.mature", END_TURN, "status.expire.before", s -> {
            var result = SummonResolver.mature(s.pieces(), s.state(SUMMONS), s.turn(), s.side(), s.state(TERRAIN), s.heroes());
            s.pieces(result.pieces());
            s.state(SUMMONS, result.summons());
        }));
    }
    @Override public List<MechanicEventType<?>> events() {
        return HistoricalEvents.types(GameEvent.Kind.YAGA_BIRTH, GameEvent.Kind.YAGA_MATURE, GameEvent.Kind.REPLACE);
    }
    @Override public void validate(GameSnapshot snapshot) { SummonValidator.validate(snapshot); }
}
