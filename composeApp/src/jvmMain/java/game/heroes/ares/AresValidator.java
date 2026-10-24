package game.heroes.ares;

import game.heroes.kikimora.KikimoraAbility;

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
            if (e.kind() == GameEvent.Kind.STUN || e.kind()==GameEvent.Kind.YARN_STUN) {
                PieceState actor = s.pieces().get(e.actorId()), target = s.pieces().get(e.targetId());boolean yarn=e.kind()==GameEvent.Kind.YARN_STUN;
                if (actor == null || (actor.hero() !=(yarn?HeroType.KIKIMORA:HeroType.ARES) && actor.hero() != HeroType.GINSENG) || target == null || target.type() == King.class || e.color() != actor.color())
                    throw new IllegalArgumentException("Invalid stun source or target");
                if (counts.merge(e.turn(), 1, Integer::sum) > 2 || s.events().stream().noneMatch(c->c.sequence() < e.sequence() && c.turn() == e.turn() && java.util.Objects.equals(c.actorId(),e.actorId()) && c.isAbility(yarn?KikimoraAbility.YARN:AresAbility.SPEAR)))throw new IllegalArgumentException("Stun without matching ability");
                int duration=yarn && s.events().stream().filter(c->c.kind() == GameEvent.Kind.YARN_STUN && c.turn()==e.turn() && java.util.Objects.equals(c.actorId(),e.actorId())).count()==2?2:1;
                long until=yarn?game.heroes.kikimora.KikimoraAbilities.stunDeadline(e.turn(),actor.color(),target.color(),duration):Math.addExact(e.turn(), 1L);
                stuns.merge(e.targetId(),until, Math::max);
            } else if (e.kind() == GameEvent.Kind.END_TURN) stuns.values().removeIf(t -> t <= e.turn());
            else if (e.targetId() != null && !(e.kind()==GameEvent.Kind.MOVE && s.pieces().get(e.targetId()).hero()==HeroType.ILLUSION) && Set.of(GameEvent.Kind.MOVE, GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE, GameEvent.Kind.ARES_REVENGE,GameEvent.Kind.MINE_CAPTURE,GameEvent.Kind.LIGHTNING_CAPTURE,GameEvent.Kind.BLAST_CAPTURE,GameEvent.Kind.BLAST_DEATH, GameEvent.Kind.REPLACE).contains(e.kind()))
                stuns.remove(e.targetId());
            if (e.kind() == GameEvent.Kind.ARES_REVENGE) {
                PieceState source = s.pieces().get(e.actorId()), target = s.pieces().get(e.targetId());
                if (source.hero() != HeroType.ARES || (source.status() != PieceState.Status.CAPTURED && source.status()!=PieceState.Status.DESTROYED) || source.ultimateCharge() != 0 || source.color() != e.color() || target == null || (target.status() != PieceState.Status.CAPTURED && target.status()!=PieceState.Status.DESTROYED))
                    throw new IllegalArgumentException("Invalid Ares revenge");
                if (s.events().stream().noneMatch(c -> c.sequence() < e.sequence() && c.turn() == e.turn() && c.targetId() != null && c.targetId().equals(source.id()) && Set.of(GameEvent.Kind.MOVE, GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE,GameEvent.Kind.MINE_CAPTURE,GameEvent.Kind.LIGHTNING_CAPTURE,GameEvent.Kind.BLAST_CAPTURE,GameEvent.Kind.BLAST_DEATH).contains(c.kind())))
                    throw new IllegalArgumentException("Revenge without Ares capture");
            }
        }
        for (PieceState p : s.pieces().values())
            if (p.stunnedThroughTurn() !=(p.status()==PieceState.Status.DISSOLVED?0:stuns.getOrDefault(p.id(), 0L)) || (p.isStunned() && (s.mode() != GameMode.MOBA || p.type() == King.class || p.isRemoved() || p.stunnedThroughTurn() < s.turn().number())))
                throw new IllegalArgumentException("Stun differs from history");
    }
}
