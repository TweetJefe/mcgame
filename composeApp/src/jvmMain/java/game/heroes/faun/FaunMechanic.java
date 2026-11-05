package game.heroes.faun;

import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicStep;
import game.state.GameSnapshot;
import java.util.List;

import static game.mechanics.BuiltInStates.FAUN;
import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.SETTLED;
import static game.mechanics.Mechanics.update;

public final class FaunMechanic implements MechanicDefinition<FaunState> {
    public static final FaunMechanic INSTANCE = new FaunMechanic();
    private FaunMechanic() {}
    @Override public boolean legacyWireState() { return true; }
    @Override public MechanicKey<FaunState> key() { return FAUN; }
    @Override public List<MechanicStep> steps() {
        return List.of(update("faun.cast", ABILITY_PREPARE, "mines.plant", s -> {
            var request = s.request(); var actor = s.before().pieceAt(request.actor());
            s.state(FAUN, s.state(FAUN).cast(actor.id(), request.ability(), request.target(), s.turn(), s.rules().faun()));
        }), update("faun.clean", SETTLED, "clown.clean", s -> s.state(FAUN, s.state(FAUN).afterCaptures(s.captures(), s.pieces()))),
            update("faun.expire", END_TURN, "terrain.expire", s -> s.state(FAUN, s.state(FAUN).expire(s.turn()))));
    }
    @Override public void validate(GameSnapshot snapshot) { FaunValidator.validate(snapshot); }
}
