package game.mechanics.compat;

import game.economy.EconomyMechanic;
import game.heroes.HeroType;
import game.heroes.pudge.SwallowMechanic;
import game.heroes.ares.AresValidator;
import game.heroes.chronomlin.ChronomlinValidator;
import game.heroes.clownganger.ClownMechanic;
import game.heroes.faun.FaunMechanic;
import game.heroes.ginseng.SpellMechanic;
import game.heroes.kikimora.KikimoraValidator;
import game.heroes.moleminer.MineMechanic;
import game.heroes.zeus.LightningMechanic;
import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicStep;
import game.state.GameSnapshot;
import game.summoning.SummonMechanic;
import game.terrain.TerrainMechanic;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import rules.moba.AttackRules;

import static game.mechanics.MechanicPhase.CAN_END;
import static game.mechanics.MechanicPhase.END_HISTORY;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.HISTORY;
import static game.mechanics.MechanicPhase.SETUP;
import static game.mechanics.Mechanics.update;

/** Связанный набор существующих механик для совместимости прежних описаний героев. */
public final class HistoricalMechanics implements MechanicDefinition<HistoricalMechanics.State> {
    public enum State { INSTANCE }
    public static final HistoricalMechanics INSTANCE = new HistoricalMechanics();
    private static final MechanicKey<State> KEY = new MechanicKey<>("historical", State.class, State.INSTANCE);
    private HistoricalMechanics() {}
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicDefinition<?>> dependencies() {
        return List.of(SpellMechanic.INSTANCE, FaunMechanic.INSTANCE,
                ClownMechanic.INSTANCE, MineMechanic.INSTANCE,
                LightningMechanic.INSTANCE, EconomyMechanic.INSTANCE,
                TerrainMechanic.INSTANCE, SummonMechanic.INSTANCE, SwallowMechanic.INSTANCE);
    }
    @Override public List<MechanicStep> steps() {
        return List.of(update("historical.assignments", SETUP, null, s -> {
                if (s.assignments().values().stream().filter(h -> h == HeroType.ZEUS).count() > 1)
                    throw new IllegalArgumentException("Only one Zeus per match");
                if (s.assignments().containsValue(HeroType.BABA_YAGA) || s.assignments().containsValue(HeroType.ILLUSION))
                    throw new IllegalArgumentException("Baba Yaga can only be summoned");
            }), update("status.canEnd", CAN_END, "clown.canEnd", s ->
                s.pieces(AttackRules.expireStuns(s.pieces(), s.turn()))),
            update("status.expire.before", END_TURN, "clown.expire", s ->
                s.pieces(AttackRules.expireStuns(s.pieces(), s.turn()))),
            update("status.expire.after", END_TURN, "lightning.expire", s ->
                s.pieces(AttackRules.expireStuns(s.pieces(), s.turn()))),
            update("status.blindness", END_TURN, "status.expire.after", s -> {
                var next = new HashMap<>(s.pieces());
                for (var piece : s.pieces().values())
                    if (piece.color() == s.side() && !piece.isRemoved() && piece.isBlinded())
                        next.put(piece.id(), piece.withBlindness(piece.blindTurns() - 1));
                s.pieces(next);
            }), update("historical.history", HISTORY, null, HistoricalJournal::action),
            update("historical.endHistory", END_HISTORY, null, HistoricalJournal::endTurn));
    }
    @Override public List<MechanicEventType<?>> events() {
        return HistoricalEvents.types(GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.STUN,
                GameEvent.Kind.ARES_REVENGE, GameEvent.Kind.YARN_STUN);
    }
    @Override public void validate(GameSnapshot snapshot) {
        HistoricalBlindness.validate(snapshot);
        for (var event : snapshot.events()) {
            if (HistoricalEvents.kind(event.effect()) == null) continue;
            var data = (HistoricalEvents.Participants) event.effect().data();
            if (!Objects.equals(event.actorId(), data.sourceId()) || !Objects.equals(event.targetId(), data.targetId())
                    || !Objects.equals(event.from(), data.from()) || !Objects.equals(event.to(), data.to()))
                throw new IllegalArgumentException("Historical effect payload differs from participants");
        }
        ChronomlinValidator.validate(snapshot);
        KikimoraValidator.validate(snapshot);
        AresValidator.validate(snapshot);
    }
}
