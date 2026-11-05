package game.heroes.zeus;

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
import game.moves.AbilityRejection;
import game.moves.Success;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.List;

import static game.mechanics.BuiltInStates.LIGHTNING;
import static game.mechanics.BuiltInStates.SUMMONS;
import static game.mechanics.MechanicPhase.ABILITY_GUARD;
import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.SETTLED;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.step;
import static game.mechanics.Mechanics.update;

public final class LightningMechanic implements MechanicDefinition<LightningState> {
    public static final LightningMechanic INSTANCE = new LightningMechanic();
    private LightningMechanic() {}
    public record Cast(PieceState actor, PieceState target, Abilities ability, boolean borrowed) implements MechanicOperation {}
    @Override public boolean legacyWireState() { return true; }
    @Override public MechanicKey<LightningState> key() { return LIGHTNING; }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Cast.class, "lightning.cast", ABILITY_PREPARE, null, (s, op) -> {
            var result = new ZeusHero(s.rules().zeus()).resolve(s.before().field(), s.pieces(),
                    s.pieces().get(op.actor().id()), op.target(), op.ability(),
                    s.state(LIGHTNING), s.state(SUMMONS), s.turn(), s.rules(), s);
            if (!result.effect().result().isSuccess()) return result.effect().result();
            s.update(result.effect().updates());
            var captures = new ArrayList<>(s.captures()); captures.addAll(result.effect().captures());
            s.captures(captures);
            s.state(LIGHTNING, result.lightning());
            s.state(SUMMONS, result.summons());
            ClownMechanic.hits(s, result.illusionHits());
            return Success.SUCCESS;
        }));
    }

    @Override public List<MechanicStep> steps() {
        return List.of(step("lightning.guard", ABILITY_GUARD, null, s -> {
            var actor = s.before().pieceAt(s.request().actor());
            return s.request().ability() == ZeusAbility.THUNDERSTORM && ZeusHero.charged(s.before().pieces(), s.state(LIGHTNING), actor).isEmpty()
                    ? AbilityRejection.NO_CHARGED_TARGETS : Success.SUCCESS;
        }), update("lightning.clean", SETTLED, "faun.clean", s -> s.state(LIGHTNING, s.state(LIGHTNING).clean(s.pieces()))),
            update("lightning.expire", END_TURN, "faun.expire", s -> s.state(LIGHTNING, s.state(LIGHTNING).expire(s.turn()).clean(s.pieces()))));
    }
    @Override public List<MechanicEventType<?>> events() {
        return HistoricalEvents.types(GameEvent.Kind.LIGHTNING_HIT, GameEvent.Kind.LIGHTNING_RESET, GameEvent.Kind.LIGHTNING_DISCHARGE, GameEvent.Kind.LIGHTNING_CAPTURE);
    }
    @Override public void validate(GameSnapshot snapshot) { LightningValidator.validate(snapshot); }
}
