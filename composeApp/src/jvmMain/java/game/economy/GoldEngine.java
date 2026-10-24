package game.economy;

import game.heroes.HeroType;
import game.moves.Capture;
import game.state.PieceState;

import java.util.List;
import java.util.Map;

/**
 * Pure financial effects, prepared before publishing the corresponding board action.
 */
public final class GoldEngine {
    private GoldEngine() {
    }

    public static EconomyState captures(EconomyState economy, List<Capture> captures,
                                        Map<Long, PieceState> before, Map<Long, PieceState> after,
                                        GoldRules rules, long turn) {
        if (!economy.enabled()) return economy;
        var next = economy;
        for (Capture capture : captures) {
            if(!capture.rewarded())continue;
            // A pawn can promote and be counter-captured within the same atomic action.
            PieceState victim = after.get(capture.victimId());
            PieceState actor = after.get(capture.actorId());
            if (victim == null) victim = before.get(capture.victimId());
            if (actor == null) actor = before.get(capture.actorId());
            BountyMark mark = next.mark(capture.victimId());
            long bounty = mark != null && mark.expiresAfterTurn() >= turn ? 3 : 0;
            long passive = actor.hero() == HeroType.BOUNTY_CROW ? 1 : 0;
            next = next.apply(new GoldChange(turn, GoldChange.Kind.CAPTURE, capture.creditedSide(), capture.actorId(),
                    capture.victimId(), rules.rewardHalves(victim), bounty, passive)).removeMark(capture.victimId());
        }
        return next;
    }
}
