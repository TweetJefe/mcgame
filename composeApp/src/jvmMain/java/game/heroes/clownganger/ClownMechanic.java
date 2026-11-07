package game.heroes.clownganger;

import game.mechanics.PieceRestriction;

import game.heroes.HeroActionContext;
import game.heroes.HeroPieceFactory;
import game.heroes.PieceCreationContext;
import game.heroes.HeroType;
import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicSession;
import game.mechanics.MechanicStep;
import game.mechanics.compat.HistoricalEvents;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.TurnRejection;
import game.state.GameSnapshot;
import game.state.ItemSlot;
import game.state.PieceState;
import game.summoning.SummonState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import model.Position;

import static game.mechanics.BuiltInStates.CLOWN;
import static game.mechanics.BuiltInStates.SUMMONS;
import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.AFTER_MOVE;
import static game.mechanics.MechanicPhase.CAN_END;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.MOVE_ATTACKER;
import static game.mechanics.MechanicPhase.MOVE_START;
import static game.mechanics.MechanicPhase.MOVE_TARGET;
import static game.mechanics.MechanicPhase.SETTLED;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.step;
import static game.mechanics.Mechanics.update;

public final class ClownMechanic implements MechanicDefinition<ClownState> {
    public static final ClownMechanic INSTANCE = new ClownMechanic();
    private ClownMechanic() {}
    public record Create(PieceState actor, Position square) implements MechanicOperation {}
    private record Hits(List<Long> ids) { private Hits { ids = List.copyOf(ids); } }
    private static final MechanicKey<Hits> HITS = new MechanicKey<>("clown.hits", Hits.class, new Hits(List.of()));
    public static void hits(MechanicSession s, List<Long> ids) {
        var combined = new ArrayList<>(s.local(HITS).ids()); combined.addAll(ids);
        s.local(HITS, new Hits(combined));
    }
    @Override public boolean legacyWireState() { return true; }
    @Override public MechanicKey<ClownState> key() { return CLOWN; }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Create.class, "clown.create", ABILITY_PREPARE, "summons.cast", (s, op) -> {
            var states = new HashMap<>(s.pieces()); var actor = op.actor(); var clown = s.state(CLOWN);
            if (states.size() >= SummonState.MAX_IDENTITIES) return AbilityRejection.INVALID_ABILITY_TARGET;
            for (var illusion : clown.illusions().values())
                if (illusion.sourceId() == actor.id() && !states.get(illusion.id()).isRemoved()) return AbilityRejection.INVALID_ABILITY_TARGET;
            long id = Math.addExact(states.keySet().stream().mapToLong(Long::longValue).max().orElse(0), 1);
            var choice = s.state(SUMMONS).choose(2);
            s.state(SUMMONS, new SummonState(true, choice.nextState(), s.state(SUMMONS).records()));
            Position bornAt = choice.index() == 0 ? actor.position() : op.square();
            if (choice.index() == 0) states.put(actor.id(), states.get(actor.id()).placed(op.square(), actor.type()));
            PieceState base = new PieceState(id, actor.type(), actor.color(), bornAt, PieceState.Status.BOARD, HeroType.ILLUSION,
                    0, 0, List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), null, actor.blindTurns(), null);
            states.put(id, new HeroPieceFactory(s.heroes()).create(base, new PieceCreationContext(bornAt, actor)));
            s.pieces(states);
            s.state(CLOWN, clown.create(id, actor.id(), bornAt, s.turn(), s.rules().clownganger()));
            return Success.SUCCESS;
        }));
    }
    @Override public List<MechanicStep> steps() {
        return List.of(step("clown.moveStart", MOVE_START, null, s -> {
            var actor = s.movement().actor();
            return actor != null && s.state(CLOWN).movementTurn() == s.turn()
                    && !s.state(CLOWN).canMove(actor.id(), s.turn(), true, s.before().pieces())
                    ? TurnRejection.MOVE_ALREADY_USED : Success.SUCCESS;
        }), step("clown.attacker", MOVE_ATTACKER, null, s -> {
            var victim = s.before().pieceAt(s.movement().to());
            return s.movement().actor().hero() == HeroType.ILLUSION && victim != null
                    ? StatusRejection.ILLUSION_CANNOT_CAPTURE : Success.SUCCESS;
        }), step("clown.target", MOVE_TARGET, null, s -> {
            var victim = s.before().pieceAt(s.movement().to());
            if (victim == null || victim.hero() != HeroType.ILLUSION) return Success.SUCCESS;
            if (s.movement().actor().isBlinded()) return StatusRejection.BLINDED_CANNOT_CAPTURE;
            s.state(CLOWN, s.state(CLOWN).hit(victim.id()));
            s.interceptMove();
            return Success.SUCCESS;
        }), update("clown.afterMove", AFTER_MOVE, null, s -> s.state(CLOWN, s.state(CLOWN).move(s.movement().actor().id(), s.turn()))),
            update("clown.clean", SETTLED, null, s -> {
            var clown = s.state(CLOWN);
            for (long id : s.local(HITS).ids()) clown = clown.hit(id);
            s.state(CLOWN, clown);
            s.pieces(clown.clean(s.pieces(), 0, p -> s.context().blocks(p, PieceRestriction.TRANSFORMATION)));
        }), update("clown.canEnd", CAN_END, null, s -> s.pieces(s.state(CLOWN).clean(s.pieces(), s.turn(), p -> s.context().blocks(p, PieceRestriction.TRANSFORMATION)))),
            update("clown.expire", END_TURN, null, s -> s.pieces(s.state(CLOWN).clean(s.pieces(), s.turn(), p -> s.context().blocks(p, PieceRestriction.TRANSFORMATION)))));
    }
    @Override public boolean allowsExtraMove(HeroActionContext context, PieceState actor) {
        return context.clown().canMove(actor.id(), context.currentTurn(), true, context.pieces());
    }
    @Override public List<MechanicEventType<?>> events() { return HistoricalEvents.types(GameEvent.Kind.ILLUSION_HIT); }
    @Override public void validate(GameSnapshot snapshot) { ClownValidator.validate(snapshot); }
}
