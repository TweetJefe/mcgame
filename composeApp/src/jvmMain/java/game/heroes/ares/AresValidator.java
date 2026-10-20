package game.heroes.ares;

import game.heroes.*;
import game.history.GameEvent;
import game.state.*;
import model.figures.King;

import java.util.*;

public final class AresValidator {
    private AresValidator() {
    }

    public static void validate(GameSnapshot s) {
        var stuns = new HashMap<Long, Long>();
        var counts = new HashMap<Long, Integer>();
        for (GameEvent e : s.events()) {
            if (e.kind() == GameEvent.Kind.STUN) {
                PieceState actor = s.pieces().get(e.actorId()), target = s.pieces().get(e.targetId());
                if (actor == null || (actor.hero() != HeroType.ARES_BISHOP && actor.hero() != HeroType.GINSENG_BISHOP) || target == null || target.type() == King.class || e.color() != actor.color())
                    throw new IllegalArgumentException("Invalid stun source or target");
                if (counts.merge(e.turn(), 1, Integer::sum) > 2 || s.events().stream().noneMatch(a -> a.sequence() < e.sequence() && a.turn() == e.turn() && a.actorId() != null && a.actorId().equals(e.actorId()) && a.kind() == GameEvent.Kind.SPEAR))
                    throw new IllegalArgumentException("Stun without spear");
                stuns.merge(e.targetId(), Math.addExact(e.turn(), 1L), Math::max);
            } else if (e.kind() == GameEvent.Kind.END_TURN) stuns.values().removeIf(t -> t <= e.turn());
            else if (e.targetId() != null && Set.of(GameEvent.Kind.MOVE, GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE, GameEvent.Kind.ARES_REVENGE, GameEvent.Kind.REPLACE).contains(e.kind()))
                stuns.remove(e.targetId());
            if (e.kind() == GameEvent.Kind.ARES_REVENGE) {
                PieceState source = s.pieces().get(e.actorId()), target = s.pieces().get(e.targetId());
                if (source.hero() != HeroType.ARES_BISHOP || source.status() != PieceState.Status.CAPTURED || source.ultimateCharge() != 0 || source.color() != e.color() || target == null || target.status() != PieceState.Status.CAPTURED || target.color() == source.color())
                    throw new IllegalArgumentException("Invalid Ares revenge");
                if (s.events().stream().noneMatch(c -> c.sequence() < e.sequence() && c.turn() == e.turn() && c.targetId() != null && c.targetId().equals(source.id()) && Set.of(GameEvent.Kind.MOVE, GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE).contains(c.kind())))
                    throw new IllegalArgumentException("Revenge without Ares capture");
            }
        }
        for (PieceState p : s.pieces().values())
            if (p.stunnedThroughTurn() != stuns.getOrDefault(p.id(), 0L) || (p.isStunned() && (s.mode() != GameMode.MOBA || p.type() == King.class || p.isRemoved() || p.stunnedThroughTurn() < s.turn().number())))
                throw new IllegalArgumentException("Stun differs from history");
    }
}
