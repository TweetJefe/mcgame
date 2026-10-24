package game.heroes.faun;

import game.heroes.kikimora.KikimoraAbility;

import game.heroes.Abilities;
import game.heroes.ginseng.SpellState;
import game.history.GameEvent;
import game.moves.Capture;
import game.state.PieceState;
import model.Position;

import java.util.*;

/**
 * Clouds outlive their caster. Invisibility keeps the real piece on the board.
 */
public record FaunState(List<Cloud> clouds, Map<Long, Invisibility> invisible) {
    public FaunState {
        clouds = List.copyOf(clouds);
        invisible = Map.copyOf(invisible);
    }

    public static FaunState empty() {
        return new FaunState(List.of(), Map.of());
    }

    public boolean silenced(Position square) {
        return clouds.stream().anyMatch(c -> c.contains(square));
    }

    public boolean hidden(PieceState p, model.FigureColor viewer) {
        return p != null && p.color() != viewer && invisible.containsKey(p.id());
    }

    public FaunState cast(long actor, Abilities ability, Position center, long turn, FaunRules rules) {
        var nextClouds = new ArrayList<>(clouds);
        var nextInvisible = new HashMap<>(invisible);
        nextInvisible.remove(actor);
        if (ability == FaunAbility.CLOUD)
            nextClouds.add(new Cloud(actor, center, turn, Math.addExact(turn, 2L * rules.cloudTurns())));
        if (ability == FaunAbility.INVISIBILITY)
            nextInvisible.put(actor, new Invisibility(turn, Math.addExact(turn, 2L * rules.invisibilityTurns())));
        return new FaunState(nextClouds, nextInvisible);
    }

    public FaunState afterCaptures(List<Capture> captures, Map<Long, PieceState> pieces) {
        var next = new HashMap<>(invisible);
        for (Capture c : captures) next.remove(c.actorId());
        next.keySet().removeIf(id -> !pieces.containsKey(id) || pieces.get(id).isRemoved() || pieces.get(id).isPotted());
        return new FaunState(clouds, next);
    }

    public FaunState expire(long turn) {
        var next = new HashMap<>(invisible);
        next.values().removeIf(i -> i.expiresAfterTurn() <= turn);
        return new FaunState(clouds.stream().filter(c -> c.expiresAfterTurn() > turn).toList(), next);
    }

    public static FaunState fromHistory(List<GameEvent> events, FaunRules rules) {
        FaunState state = empty();
        for (GameEvent e : events) {
            Abilities a = SpellState.activeAbility(e);
            if (a != null) {
                if (state.silenced(e.from())) throw new IllegalArgumentException("Active cast from a cloud");
                if (a == FaunAbility.CLOUD && (e.from() == null || e.to() == null || Math.max(Math.abs(e.from().getRow() - e.to().getRow()), Math.abs(e.from().getColumn() - e.to().getColumn())) > 4))
                    throw new IllegalArgumentException("Cloud center out of range");
                if (a == FaunAbility.INVISIBILITY && (e.from() == null || !e.from().equals(e.to())))
                    throw new IllegalArgumentException("Invisibility changes square");
                state = state.cast(e.actorId(), a, e.to(), e.turn(), rules);
            } else if (e.kind() == GameEvent.Kind.END_TURN) state = state.expire(e.turn());
            if ((e.kind() == GameEvent.Kind.MOVE || e.kind() == GameEvent.Kind.COUNTER_CAPTURE || e.kind() == GameEvent.Kind.BOG_CAPTURE || e.kind() == GameEvent.Kind.ARES_REVENGE || e.kind()==GameEvent.Kind.MINE_CAPTURE || (e.kind()==GameEvent.Kind.BLAST_CAPTURE || e.kind()==GameEvent.Kind.LIGHTNING_CAPTURE) || e.kind()==GameEvent.Kind.BLAST_DEATH) && e.targetId() != null) {
                var next = new HashMap<>(state.invisible());
                next.remove(e.actorId());
                next.remove(e.targetId());
                state = new FaunState(state.clouds(), next);
            } else if (e.kind() == GameEvent.Kind.REPLACE || e.isAbility(KikimoraAbility.CLAY_POT)) {
                var next = new HashMap<>(state.invisible());
                next.remove(e.targetId());
                state = new FaunState(state.clouds(), next);
            }
        }
        return state;
    }
}
