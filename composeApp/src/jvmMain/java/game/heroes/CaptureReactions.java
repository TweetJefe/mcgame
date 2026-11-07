package game.heroes;

import game.moves.StatusRejection;

import game.mechanics.PieceRestriction;

import game.mechanics.MechanicOperation;
import game.mechanics.DeathContext;
import java.util.function.Function;
import game.mechanics.MechanicStates;
import game.moves.Capture;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import model.Field;
import model.Position;

/** Bounded death processing with native hero reactions followed by registered mechanic reactions. */
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
        return resolve(context, victim, captor, square, heroes, (before, effect) -> effect, context::mechanics);
    }

    public static HeroEffect resolve(HeroActionContext context, PieceState victim, long captor,
            Position square, HeroRegistry heroes,
            BiFunction<HeroActionContext, HeroEffect, HeroEffect> operations,
            Supplier<MechanicStates> states) {
        var work = new HashMap<>(context.pieces());
        var updates = new HashMap<Long, PieceState>();
        var captures = new ArrayList<Capture>();
        var proposed = new ArrayList<MechanicOperation>();
        var pending = new ArrayDeque<Death>();
        pending.add(new Death(victim, captor, square));
        int processed = 0;
        while (!pending.isEmpty()) {
            if (++processed > context.pieces().size() * 2L + 1) return HeroEffect.rejected(MovementRejection.INVALID_BOARD);
            Death death = pending.remove();
            var guard = new HeroActionContext(context.field(), work, context.currentTurn(), states.get(), heroes.mechanics());
            if (guard.blocks(death.victim(), PieceRestriction.CAPTURE))
                return HeroEffect.rejected(StatusRejection.PROTECTED_OBSTACLE);
            var reactions = new ArrayList<Function<HeroActionContext, HeroEffect>>();
            if (!guard.blocks(death.victim(), PieceRestriction.PASSIVES))
                reactions.add(reactionContext -> heroes.get(death.victim().hero())
                        .onCaptured(reactionContext, death.victim(), death.killer(), death.square()));
            for (var reaction : heroes.mechanics().captureReactions())
                reactions.add(reactionContext -> reaction.action().apply(new DeathContext(reactionContext,
                        death.victim(), death.killer(), death.square())));
            var further = new ArrayList<Death>();
            for (var reaction : reactions) {
                HeroActionContext reactionContext = new HeroActionContext(context.field(), work, context.currentTurn(),
                        states.get(), heroes.mechanics());
                HeroEffect effect = reaction.apply(reactionContext);
                if (!effect.result().isSuccess()) return HeroEffect.rejected(effect.result());
                effect = operations.apply(reactionContext, effect);
                if (!effect.result().isSuccess()) return HeroEffect.rejected(effect.result());
                proposed.addAll(effect.operations());
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
                    PieceState killed = reactionContext.pieceById(capture.victimId());
                    if (killed == null) return HeroEffect.rejected(MovementRejection.INVALID_BOARD);
                    further.add(new Death(killed, capture.actorId(), capture.square()));
                }
            }
            for (int i = further.size() - 1; i >= 0; i--) pending.addFirst(further.get(i));
        }
        return new HeroEffect(Success.SUCCESS, updates, captures, proposed);
    }
}
