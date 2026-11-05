package game.terrain;

import game.heroes.Abilities;
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
import java.util.List;
import model.Position;

import static game.mechanics.BuiltInStates.TERRAIN;
import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.CAN_END;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.ENTRY;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.step;
import static game.mechanics.Mechanics.update;

public final class TerrainMechanic implements MechanicDefinition<TerrainState> {
    public static final TerrainMechanic INSTANCE = new TerrainMechanic();
    private TerrainMechanic() {}
    public record Place(PieceState actor, Position square, Abilities ability) implements MechanicOperation {}
    private record Entries(List<Long> ids) { private Entries { ids = List.copyOf(ids); } }
    private static final MechanicKey<Entries> IMMEDIATE = new MechanicKey<>("terrain.immediate", Entries.class, new Entries(List.of()));
    @Override public boolean legacyWireState() { return true; }
    @Override public MechanicKey<TerrainState> key() { return TERRAIN; }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Place.class, "terrain.place", ABILITY_PREPARE, "lightning.cast", (s, op) -> {
            var result = TerrainResolver.place(s.state(TERRAIN), s.before().pieces(), op.actor(), op.square(), op.ability(), s.turn(), s.rules());
            if (!result.result().isSuccess()) return result.result();
            s.state(TERRAIN, result.terrain());
            s.local(IMMEDIATE, new Entries(result.immediateEntries()));
            return Success.SUCCESS;
        }));
    }
    @Override public List<MechanicStep> steps() {
        return List.of(step("terrain.entry", ENTRY, "mines.entry", s -> {
            var result = TerrainResolver.resolve(s.before().field(), s.entryBefore(), s.pieces(), s.state(TERRAIN), s.captures(),
                    s.iteration() == 0 ? s.local(IMMEDIATE).ids() : List.of(), s.rules(), s);
            if (!result.result().isSuccess()) return result.result();
            s.pieces(result.pieces());
            s.state(TERRAIN, result.terrain());
            s.captures(result.captures());
            ClownMechanic.hits(s, result.illusionHits());
            return Success.SUCCESS;
        }), update("terrain.canEnd", CAN_END, "status.canEnd", s -> s.state(TERRAIN, s.state(TERRAIN).expire(s.turn()))),
            update("terrain.expire", END_TURN, "summons.mature", s -> s.state(TERRAIN, s.state(TERRAIN).expire(s.turn()))));
    }
    @Override public List<MechanicEventType<?>> events() { return HistoricalEvents.types(GameEvent.Kind.BOG_CAPTURE); }
    @Override public void validate(GameSnapshot snapshot) { TerrainValidator.validate(snapshot); }
}
