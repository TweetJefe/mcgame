package game.heroes.ginseng;

import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicStep;
import game.state.GameSnapshot;
import java.util.List;

import static game.mechanics.BuiltInStates.SPELLS;
import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.Mechanics.update;

public final class SpellMechanic implements MechanicDefinition<SpellState> {
    public static final SpellMechanic INSTANCE = new SpellMechanic();
    private SpellMechanic() {}
    @Override public boolean legacyWireState() { return true; }
    @Override public MechanicKey<SpellState> key() { return SPELLS; }
    @Override public List<MechanicStep> steps() {
        return List.of(update("spells.cast", ABILITY_PREPARE, "clown.create", s -> {
            var request = s.request(); var actor = s.before().pieceAt(request.actor()); var target = s.before().pieceAt(request.target());
            var copied = request.ability() == GinsengAbility.COPY_SPELL && target != null ? s.before().spells().lastUsed().get(target.id()) : null;
            s.state(SPELLS, s.state(SPELLS).cast(actor.id(), request.ability(), request.borrowed(), s.turn(), target == null ? null : target.id(), copied));
        }));
    }
    @Override public void validate(GameSnapshot snapshot) { SpellValidator.validate(snapshot); }
}
