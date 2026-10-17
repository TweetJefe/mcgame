package game.economy;

import game.heroes.HeroType;
import game.history.GameEvent;
import game.state.*;
import model.FigureColor;
import model.figures.King;
import java.util.HashSet;
import java.util.Objects;
import java.util.HashMap;
import java.util.ArrayList;

/** Checks wallets against the financial ledger and authoritative action history. */
public final class EconomyValidator {
    private EconomyValidator() {}
    private record ActionKey(long turn, long actor, Long target) {}
    public static void validate(GameSnapshot s) {
        EconomyState economy = Objects.requireNonNull(s.economy(), "Missing economy");
        if (!economy.enabled()) {
            require(s.pieces().values().stream().noneMatch(p -> p.hero() == HeroType.BOUNTY_CROW_PAWN), "Crow requires economy");
            require(s.events().stream().noneMatch(e -> e.kind() == GameEvent.Kind.BOUNTY || e.kind() == GameEvent.Kind.STEAL), "Gold event without economy");
            return;
        }
        require(s.mode() == GameMode.MOBA, "Economy in classic game");
        long white = 0, black = 0, previous = 0;
        var victims = new HashSet<Long>();
        var steals = new HashSet<Long>();
        var captures = new HashMap<ActionKey, GameEvent>();
        var thefts = new HashSet<ActionKey>();
        var bounties = new HashMap<Long, ArrayList<GameEvent>>();
        for (GameEvent event : s.events()) {
            if ((event.kind() == GameEvent.Kind.MOVE || event.kind() == GameEvent.Kind.COUNTER_CAPTURE || event.kind() == GameEvent.Kind.BOG_CAPTURE) && event.targetId() != null)
                captures.put(new ActionKey(event.turn(), event.actorId(), event.targetId()), event);
            if (event.kind() == GameEvent.Kind.STEAL) thefts.add(new ActionKey(event.turn(), event.actorId(), null));
            if (event.kind() == GameEvent.Kind.BOUNTY) {
                PieceState source = s.pieces().get(event.actorId()), target = s.pieces().get(event.targetId());
                require((source.hero() == HeroType.BOUNTY_CROW_PAWN || source.hero()==HeroType.GINSENG_BISHOP) && source.color() != target.color() && target.type() != King.class,
                        "Invalid bounty event source or target");
                bounties.computeIfAbsent(event.targetId(), ignored -> new ArrayList<>()).add(event);
            }
        }
        for (GoldChange entry : economy.history()) {
            require(entry.turn() >= previous && entry.turn() <= s.turn().number(), "Invalid gold history turn");
            previous = entry.turn();
            PieceState actor = s.pieces().get(entry.actorId());
            require(actor != null && actor.color() == entry.side(), "Gold recipient differs from actor");
            if (entry.kind() == GoldChange.Kind.CAPTURE) {
                PieceState victim = s.pieces().get(entry.victimId());
                require(victim != null && victim.color() != actor.color() && victim.status() == PieceState.Status.CAPTURED
                        && victims.add(victim.id()), "Invalid or duplicated capture gold");
                require(entry.baseHalves() == s.heroRules().gold().rewardHalves(victim), "Capture price differs from rules");
                require(entry.passiveHalves() == (actor.hero() == HeroType.BOUNTY_CROW_PAWN ? 1 : 0), "Invalid Crow passive reward");
                GameEvent capture = captures.get(new ActionKey(entry.turn(), entry.actorId(), entry.victimId()));
                require(capture != null, "Reward without matching capture event");
                GameEvent lastMark = null;
                var targetMarks = bounties.get(entry.victimId());
                if (targetMarks != null) for (GameEvent mark : targetMarks) {
                    if (mark.sequence() >= capture.sequence()) break;
                    lastMark = mark;
                }
                boolean marked = lastMark != null && capture.turn() - lastMark.turn() <= 2L * s.crowRules().bountyTurns();
                require(entry.bountyHalves() == (marked ? 3 : 0), "Bounty reward differs from mark lifetime");
            } else {
                require((actor.hero() == HeroType.BOUNTY_CROW_PAWN || actor.hero()==HeroType.GINSENG_BISHOP) && steals.add(entry.turn()), "Invalid theft source");
                require(thefts.contains(new ActionKey(entry.turn(), entry.actorId(), null)), "Transfer without theft event");
                long available = entry.side() == FigureColor.WHITE ? black : white;
                require(entry.totalHalves() == Math.min(4, available) && available > 0, "Invalid theft amount");
                if (entry.side() == FigureColor.WHITE) black -= entry.totalHalves(); else white -= entry.totalHalves();
            }
            if (entry.side() == FigureColor.WHITE) white = Math.addExact(white, entry.totalHalves());
            else black = Math.addExact(black, entry.totalHalves());
        }
        require(white == economy.whiteHalves() && black == economy.blackHalves(), "Wallet differs from gold history");
        for (PieceState piece : s.pieces().values())
            require((piece.status() == PieceState.Status.CAPTURED) == victims.contains(piece.id()), "Missing capture gold entry");
        require(s.events().stream().filter(e -> e.kind() == GameEvent.Kind.STEAL).count() == steals.size(), "Missing theft entry");
        for (BountyMark mark : economy.marks()) {
            PieceState source = s.pieces().get(mark.sourceId()), target = s.pieces().get(mark.targetId());
            require(source != null && (source.hero() == HeroType.BOUNTY_CROW_PAWN || source.hero()==HeroType.GINSENG_BISHOP) && target != null
                    && source.color() != target.color() && target.type() != King.class && !target.isRemoved(),
                    "Invalid marked piece");
            require(mark.expiresAfterTurn() >= s.turn().number()
                    && mark.expiresAfterTurn() - s.turn().number() <= 2L * s.crowRules().bountyTurns(), "Invalid bounty expiry");
            var targetMarks = bounties.get(mark.targetId());
            require(targetMarks != null && !targetMarks.isEmpty(), "Mark without ability event");
            GameEvent last = targetMarks.getLast();
            require(last.actorId() == mark.sourceId() && mark.expiresAfterTurn() - last.turn() == 2L * s.crowRules().bountyTurns(), "Mark differs from latest ability event");
        }
        for (var targetMarks : bounties.values()) {
            GameEvent last = targetMarks.getLast();
            PieceState target = s.pieces().get(last.targetId());
            boolean active = !target.isRemoved()
                    && s.turn().number() - last.turn() <= 2L * s.crowRules().bountyTurns();
            require((economy.mark(target.id()) != null) == active, "Missing or expired bounty mark");
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
