package game.heroes;

import game.moves.MovementRejection;
import game.moves.Success;
import game.moves.Capture;
import game.state.PieceState;
import model.Field;
import model.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bounded, ordered death processing. Individual reactions are supplied by registered heroes. */
public final class CaptureReactions {
    private CaptureReactions() {}

    private record Death(PieceState victim, long killer, Position square) {}

    public static HeroEffect resolve(Field field, Map<Long, PieceState> pieces, PieceState victim,
                                     long captor, Position square, HeroRules rules) {
        return resolve(new HeroActionContext(field, pieces, 0), victim, captor, square,
                HeroRegistry.fromRules(rules));
    }

    public static HeroEffect resolve(HeroActionContext context, PieceState victim, long captor,
                                     Position square, HeroRegistry heroes) {
        var work = new HashMap<>(context.pieces());
        var updates = new HashMap<Long, PieceState>();
        var captures = new ArrayList<Capture>();
        var pending = new ArrayDeque<Death>();
        pending.add(new Death(victim, captor, square));
        int processed = 0;
        while (!pending.isEmpty()) {
            if (++processed > context.pieces().size() * 2L + 1) return HeroEffect.rejected(MovementRejection.INVALID_BOARD);
            Death death = pending.remove();
            HeroActionContext reactionContext = new HeroActionContext(context.field(), work, context.currentTurn(),
                    context.terrain(), context.faun(), context.spells(), context.economy());
            HeroEffect effect = heroes.get(death.victim().hero())
                    .onCaptured(reactionContext, death.victim(), death.killer(), death.square());
            if (!effect.result().isSuccess()) return HeroEffect.rejected(effect.result());
            work.putAll(effect.updates());
            updates.putAll(effect.updates());
            captures.addAll(effect.captures());
            for (Capture capture : effect.captures()) {
                PieceState winner = work.get(capture.actorId());
                if (winner != null && winner.status() == PieceState.Status.BOARD
                        && winner.color() != capture.victim().getColor()) {
                    PieceState charged = heroes.get(winner.hero()).afterCapture(winner);
                    work.put(charged.id(), charged);
                    updates.put(charged.id(), charged);
                }
            }
            List<Capture> further = effect.captures();
            for (int i = further.size() - 1; i >= 0; i--) {
                Capture capture = further.get(i);
                PieceState killed = reactionContext.pieceById(capture.victimId());
                if (killed == null) return HeroEffect.rejected(MovementRejection.INVALID_BOARD);
                pending.addFirst(new Death(killed, capture.actorId(), capture.square()));
            }
        }
        return new HeroEffect(Success.SUCCESS, updates, captures);
    }
}
