package game.heroes.pudge;

import game.heroes.HeroEffect;
import game.mechanics.DeathContext;
import game.mechanics.MechanicCaptureReaction;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicKey;
import game.moves.Capture;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import model.Position;
import model.figures.King;

import java.util.List;
import java.util.Map;

/** Проглоченный союзник принадлежит исполнителю, включая исполнителя копии. */
public final class SwallowMechanic implements MechanicDefinition<SwallowMechanic.State> {
    public enum State { INSTANCE }
    public static final SwallowMechanic INSTANCE = new SwallowMechanic();
    private static final MechanicKey<State> KEY = new MechanicKey<>("swallow", State.class, State.INSTANCE);
    private SwallowMechanic() {}
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicCaptureReaction> captureReactions() {
        return List.of(new MechanicCaptureReaction("swallow.release", SwallowMechanic::release));
    }

    private static HeroEffect release(DeathContext death) {
        var context = death.context();
        var victim = death.victim();
        Position square = death.square();
        if (victim.storedPieceId() == null) return HeroEffect.empty();
        PieceState ally = context.pieceById(victim.storedPieceId());
        if (ally == null || ally.status() != PieceState.Status.SWALLOWED)
            return HeroEffect.rejected(MovementRejection.INVALID_BOARD);
        PieceState returned = ally.placed(square, ally.type());
        PieceState killer = context.pieceById(death.killerId());
        if (killer == null || killer.isRemoved() || killer.isPotted())
            return HeroEffect.success(Map.of(returned.id(), returned));
        if (killer.type() == King.class) return HeroEffect.rejected(MovementRejection.KING_IN_CHECK);
        PieceState killed = killer.hidden(killer.color() == victim.color()
                ? PieceState.Status.DESTROYED : PieceState.Status.CAPTURED);
        Position capturedAt = killer.position() == null ? square : killer.position();
        Capture capture = new Capture(ally.id(), killer.id(), ally.color(),
                context.field().createFigure(killer.type(), capturedAt, killer.color()),
                capturedAt, true, Capture.Cause.PUDGE);
        return new HeroEffect(Success.SUCCESS, Map.of(returned.id(), returned, killed.id(), killed), List.of(capture));
    }
}
