package game.heroes.sunwukong;

import game.heroes.HeroType;
import game.history.GameEvent;
import game.mechanics.PieceBodyHistory;
import game.state.GameMode;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.summoning.SummonState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.List;
import model.FigureColor;

/** Проверка состояния по принятым случайным решениям и журналу превращений. */
final class WukongHistory {
    private WukongHistory() {}
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    static WukongState replay(GameSnapshot snapshot, long seed) {
        var forms = new HashMap<Long, WukongState.Form>();
        var clones = new HashMap<Long, WukongState.Origin>();
        long random = seed;
        for (var event : snapshot.events()) {
            if (event.isEffect(SunWukongMechanic.SHIFT)) {
                var data = SunWukongMechanic.SHIFT.type().cast(event.effect().data());
                var choice = SummonState.initial(random).choose(3);
                require(choice.nextState() == data.randomState() && data.form().chosen() == List.of(
                        WukongState.Body.KNIGHT, WukongState.Body.BISHOP, WukongState.Body.ROOK).get(choice.index()),
                        "Wukong random choice differs from its sequence");
                require(forms.putIfAbsent(event.actorId(), data.form()) == null, "Overlapping temporary forms");
                require(data.form().until() == Math.addExact(event.turn(),
                        2L * snapshot.heroRules().get(SunWukongDefinition.INSTANCE).shapeshiftTurns()), "Invalid form lifetime");
                require(paired(snapshot, event, SunWukongAbility.SHAPESHIFT), "Form without ability use");
                random = data.randomState();
            }
            if (event.isEffect(SunWukongMechanic.REVERT)) {
                var form = forms.remove(event.actorId());
                require(form != null && form.until() == event.turn(), "Return without a matching expiry");
                var data = SunWukongMechanic.REVERT.type().cast(event.effect().data());
                boolean alreadyRemoved = snapshot.events().stream().anyMatch(e -> e.sequence() < event.sequence()
                        && e.targetId() != null && e.targetId().equals(event.actorId()) && java.util.Set.of(GameEvent.Kind.MOVE,
                                GameEvent.Kind.CAPTURE, GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE,
                                GameEvent.Kind.ARES_REVENGE, GameEvent.Kind.MINE_CAPTURE, GameEvent.Kind.BLAST_CAPTURE,
                                GameEvent.Kind.BLAST_DEATH, GameEvent.Kind.LIGHTNING_CAPTURE, GameEvent.Kind.REPLACE).contains(e.legacyKind()));
                require(data.after() == (alreadyRemoved || !data.before().isClassic() ? data.before() : form.original()), "Invalid returning body");
                require(snapshot.events().stream().anyMatch(e -> e.kind() == GameEvent.Kind.END_TURN && e.turn() == event.turn()),
                        "Return outside completed turn");
            }
            if (event.isEffect(SunWukongMechanic.CLONED)) {
                var data = SunWukongMechanic.CLONED.type().cast(event.effect().data());
                var source = snapshot.pieces().get(event.actorId());
                var target = snapshot.pieces().get(event.targetId());
                require(source != null && target != null && source.id() != target.id() && source.color() == target.color()
                        && target.hero() == HeroType.SUN_WUKONG && data.source() == source.id() && data.target() == target.id(),
                        "Invalid Wukong lineage");
                require(paired(snapshot, event, SunWukongAbility.CLONE), "Clone without ability use");
                require(clones.putIfAbsent(target.id(), new WukongState.Origin(source.id(), event.turn(), event.to())) == null,
                        "Duplicate clone origin");
                require(snapshot.events().stream().noneMatch(e -> e.sequence() < event.sequence() && e.actorId() != null
                        && e.actorId().equals(target.id()) && e.kind() == GameEvent.Kind.ABILITY_USED),
                        "Clone used hero abilities before being created");
            }
        }
        return new WukongState(seed, random, forms, clones);
    }

    private static boolean paired(GameSnapshot snapshot, GameEvent effect, SunWukongAbility ability) {
        return snapshot.events().stream().anyMatch(e -> e.sequence() < effect.sequence() && e.turn() == effect.turn()
                && java.util.Objects.equals(e.actorId(), effect.actorId()) && e.isAbility(ability)
                && (ability == SunWukongAbility.SHAPESHIFT || java.util.Objects.equals(e.targetId(), effect.targetId())));
    }

    static void validate(GameSnapshot snapshot) {
        var state = snapshot.mechanics().get(SunWukongMechanic.KEY);
        require(state.equals(replay(snapshot, state.initialSeed())), "Wukong state differs from history");
        require(snapshot.mode() == GameMode.MOBA || state.equals(WukongState.empty()), "Wukong state in Classic");
        var ids = snapshot.pieces().values().stream().filter(p -> p.hero() == HeroType.SUN_WUKONG)
                .map(PieceState::id).collect(Collectors.toCollection(HashSet::new));
        snapshot.events().stream().filter(e -> e.isEffect(SunWukongMechanic.SHIFT))
                .map(GameEvent::actorId).forEach(ids::add);
        var bodies = PieceBodyHistory.forPieces(snapshot, ids);
        for (long id : ids)
            require(snapshot.pieces().get(id).type() == bodies.latest(id), "Current Wukong form differs from history");
        for (var event : snapshot.events()) if (event.kind() == GameEvent.Kind.END_TURN)
            for (var form : state.forms().values()) require(form.until() > event.turn(), "Overdue temporary form");
        for (FigureColor side : List.of(FigureColor.WHITE, FigureColor.BLACK)) {
            var cooldowns = snapshot.pieces().values().stream().filter(p -> p.hero() == HeroType.SUN_WUKONG
                    && p.color() == side && !p.isRemoved()).map(PieceState::cooldown).distinct().toList();
            require(cooldowns.size() <= 1, "Wukong copies have different shared cooldowns");
        }
        for (var event : snapshot.events()) if (event.isAbility(SunWukongAbility.SHAPESHIFT) || event.isAbility(SunWukongAbility.CLONE)) {
            var type = event.isAbility(SunWukongAbility.SHAPESHIFT) ? SunWukongMechanic.SHIFT : SunWukongMechanic.CLONED;
            require(snapshot.events().stream().filter(e -> e.effect() != null && e.effect().type() == type
                    && e.turn() == event.turn() && java.util.Objects.equals(e.actorId(), event.actorId())).count() == 1,
                    "Ability without its mechanic event");
        }
    }
}
