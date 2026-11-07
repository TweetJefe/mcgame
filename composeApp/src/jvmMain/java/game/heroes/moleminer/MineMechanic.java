package game.heroes.moleminer;

import game.mechanics.PieceRestriction;

import game.heroes.clownganger.ClownMechanic;
import game.heroes.HeroType;
import game.mechanics.MechanicSession;
import game.moves.Capture;
import game.state.PieceState;
import game.summoning.SummonState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import model.Field;
import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicStep;
import game.mechanics.compat.HistoricalEvents;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.GameSnapshot;
import java.util.List;
import model.Position;
import model.figures.King;

import static game.mechanics.BuiltInStates.MINES;
import static game.mechanics.BuiltInStates.SUMMONS;
import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.ENTRY;
import static game.mechanics.MechanicPhase.MOVE_PATH;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.step;

public final class MineMechanic implements MechanicDefinition<MineState> {
    public static final MineMechanic INSTANCE = new MineMechanic();
    private MineMechanic() {}
    public record Plant(long owner, Position square) implements MechanicOperation {}
    public record Blast(long owner, Position landing) implements MechanicOperation {}
    @Override public boolean legacyWireState() { return true; }
    @Override public MechanicKey<MineState> key() { return MINES; }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Plant.class, "mines.plant", ABILITY_PREPARE, "spells.cast", (s, op) -> {
            s.state(MINES, s.state(MINES).plant(op.owner(), op.square(), s.turn())); return Success.SUCCESS;
        }), handler(Blast.class, "mines.blast", ENTRY, "terrain.entry", (s, op) -> {
            var result = explode(s.before().field(), s.pieces(), op.owner(), op.landing(), s.state(MINES), s.state(SUMMONS), s.captures(), s.turn(), s);
            s.pieces(result.pieces());
            s.state(MINES, result.mines());
            s.state(SUMMONS, result.summons());
            s.captures(result.captures());
            ClownMechanic.hits(s, result.illusionHits());
            return Success.SUCCESS;
        }));
    }
    @Override public List<MechanicStep> steps() {
        return List.of(step("mines.kingPath", MOVE_PATH, null, s -> {
            var move = s.movement();
            if (move.actor().type() != King.class) return Success.SUCCESS;
            if (s.state(MINES).hostile(move.to(), s.before().pieces(), move.actor().color())) return StatusRejection.KING_IMMUNE;
            Position crossed = new Position(move.from().getRow(), move.from().getColumn() + Integer.signum(move.to().getColumn() - move.from().getColumn()));
            return move.castling() && s.state(MINES).hostile(crossed, s.before().pieces(), move.actor().color())
                    ? StatusRejection.KING_IMMUNE : Success.SUCCESS;
        }), step("mines.entry", ENTRY, null, s -> {
            var result = MineResolver.resolve(s.before().field(), s.entryBefore(), s.pieces(), s.state(MINES), s.captures(), s.turn(), s.rules(), s);
            if (!result.result().isSuccess()) return result.result();
            s.pieces(result.pieces());
            s.state(MINES, result.mines());
            s.captures(result.captures());
            ClownMechanic.hits(s, result.illusionHits());
            return Success.SUCCESS;
        }));
    }
    @Override public List<MechanicEventType<?>> events() {
        return HistoricalEvents.types(GameEvent.Kind.MINE_TRIGGER, GameEvent.Kind.MINE_CAPTURE, GameEvent.Kind.BLAST_AREA, GameEvent.Kind.BLAST_CAPTURE, GameEvent.Kind.BLAST_DEATH);
    }
    @Override public void validate(GameSnapshot snapshot) { MineValidator.validate(snapshot); }

    private record Explosion(Map<Long, PieceState> pieces, MineState mines, SummonState summons,
                            List<Capture> captures, List<Long> illusionHits) {}

    /** Одновременное удаление жертв предшествует наградам и ответным реакциям. */
    private static Explosion explode(Field factory, Map<Long, PieceState> prepared, long actorId, Position landing,
            MineState mines, SummonState summons, List<Capture> previous, long turn,
            MechanicSession session) {
        var corners = MoleminerHero.corners(landing);
        var choice = summons.choose(corners.size());
        var blast = new MineState.Blast(actorId, turn, landing, corners.get(choice.index()));
        var states = new HashMap<>(prepared);
        var captures = new ArrayList<>(previous);
        var hits = new ArrayList<Long>();
        PieceState actor = states.get(actorId);
        var victims = prepared.values().stream().filter(piece -> piece.position() != null
                && !session.context().blocks(piece, PieceRestriction.CAPTURE)
                && blast.contains(piece.position()) && piece.type() != King.class && !piece.isBoxed() && !piece.isPotted())
                .sorted(Comparator.comparingLong(PieceState::id)).toList();
        for (PieceState victim : victims) {
            if (victim.hero() == HeroType.ILLUSION) {
                if (victim.color() != actor.color()) hits.add(victim.id());
                continue;
            }
            states.put(victim.id(), victim.hidden(victim.color() == actor.color()
                    ? PieceState.Status.DESTROYED : PieceState.Status.CAPTURED));
        }
        // Все жертвы удалены до первой награды и реакции на гибель.
        for (PieceState victim : victims) {
            if (victim.hero() == HeroType.ILLUSION) continue;
            captures.add(new Capture(actorId, victim.id(), actor.color(),
                    factory.createFigure(victim.type(), victim.position(), victim.color()), victim.position(), true,
                    Capture.Cause.BLAST));
            if (victim.color() != actor.color()) {
                PieceState charged = session.heroes().get(actor.hero()).afterCapture(states.get(actorId));
                states.put(actorId, charged);
            }
        }
        session.state(MINES, mines.blast(blast));
        session.state(SUMMONS, new SummonState(true, choice.nextState(), summons.records()));
        for (PieceState victim : victims) {
            if (victim.hero() == HeroType.ILLUSION) continue;
            var reaction = session.react(states, victim, actorId, victim.position());
            if (!reaction.result().isSuccess()) throw new IllegalArgumentException("Invalid explosion reaction");
            states.putAll(reaction.updates());
            captures.addAll(reaction.captures());
        }
        return new Explosion(Map.copyOf(states), session.state(MINES), session.state(SUMMONS),
                List.copyOf(captures), List.copyOf(hits));
    }

}
