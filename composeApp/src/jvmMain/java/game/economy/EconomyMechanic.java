package game.economy;

import game.heroes.bountycrow.BountyCrowAbility;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicStep;
import game.moves.EconomyRejection;
import game.moves.Success;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.List;
import rules.standard.MoveValidator;

import static game.mechanics.BuiltInStates.ECONOMY;
import static game.mechanics.MechanicPhase.ABILITY_GUARD;
import static game.mechanics.MechanicPhase.AFTER_ACTION;
import static game.mechanics.MechanicPhase.AFTER_SAFETY;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.step;
import static game.mechanics.Mechanics.update;

public final class EconomyMechanic implements MechanicDefinition<EconomyState> {
    public static final EconomyMechanic INSTANCE = new EconomyMechanic();
    private EconomyMechanic() {}
    public record Mark(PieceState actor, PieceState target) implements MechanicOperation {}
    public record Steal(long actor) implements MechanicOperation {}
    @Override public boolean legacyWireState() { return true; }
    @Override public MechanicKey<EconomyState> key() { return ECONOMY; }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Mark.class, "economy.mark", AFTER_ACTION, null, (s, op) -> {
            s.state(ECONOMY, s.before().economy().withMark(new BountyMark(op.actor().id(), op.target().id(),
                    Math.addExact(s.turn(), 2L * s.rules().crow().bountyTurns())))); return Success.SUCCESS;
        }), handler(Steal.class, "economy.steal", AFTER_ACTION, "economy.mark", (s, op) -> {
            s.state(ECONOMY, s.state(ECONOMY).apply(new GoldChange(s.turn(), GoldChange.Kind.STEAL, s.side(), op.actor(), null,
                    Math.min(4, s.state(ECONOMY).balanceHalves(MoveValidator.opposite(s.side()))), 0, 0)));
            return Success.SUCCESS;
        }));
    }
    @Override public List<MechanicStep> steps() {
        return List.of(step("economy.guard", ABILITY_GUARD, "lightning.guard", s -> {
            var ability = s.request().ability();
            if ((ability == BountyCrowAbility.BOUNTY || ability == BountyCrowAbility.STEAL) && !s.state(ECONOMY).enabled()) return EconomyRejection.ECONOMY_DISABLED;
            if (ability == BountyCrowAbility.STEAL && s.state(ECONOMY).balanceHalves(MoveValidator.opposite(s.side())) == 0) return EconomyRejection.NO_GOLD_TO_STEAL;
            return Success.SUCCESS;
        }), update("economy.captures", AFTER_SAFETY, null, s -> {
            var economy = GoldEngine.captures(s.before().economy(), s.captures(), s.before().pieces(), s.pieces(), s.rules().gold(), s.turn());
            for (var piece : s.pieces().values()) if (piece.isRemoved()) economy = economy.removeMark(piece.id());
            s.state(ECONOMY, economy);
        }), update("economy.expire", END_TURN, "status.blindness", s -> {
            var economy = s.state(ECONOMY).expire(s.turn());
            for (var piece : s.pieces().values()) if (piece.isRemoved()) economy = economy.removeMark(piece.id());
            s.state(ECONOMY, economy);
        }));
    }
    @Override public void validate(GameSnapshot snapshot) { EconomyValidator.validate(snapshot); }
}
