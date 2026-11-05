package game.mechanics;

import game.heroes.Abilities;
import game.heroes.AbstractHero;
import game.heroes.AbilityRequest;
import game.heroes.CaptureReactions;
import game.heroes.HeroActionContext;
import game.heroes.HeroDefinition;
import game.heroes.HeroEffect;
import game.heroes.HeroRegistry;
import game.heroes.HeroType;
import game.heroes.NoHeroRules;
import game.moves.Success;
import game.state.PieceState;
import model.Field;
import model.figures.Rook;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class CaptureReactionTest {
    private enum Unit { VALUE }
    private static MechanicDefinition<Unit> module(String id, List<MechanicCaptureReaction> reactions) {
        var key = new MechanicKey<>(id, Unit.class, Unit.VALUE);
        return new MechanicDefinition<>() {
            @Override public MechanicKey<Unit> key() { return key; }
            @Override public List<MechanicCaptureReaction> captureReactions() { return reactions; }
        };
    }

    @Test public void reactionsSeePreparedStatesInDependencyOrderAfterTheNativeReaction() {
        var first = module("first", List.of(new MechanicCaptureReaction("first.react", death -> {
            PieceState killer = death.context().pieceById(death.killerId());
            assertEquals(1, killer.blindTurns());
            return HeroEffect.success(Map.of(killer.id(), killer.withBlindness(2)));
        })));
        var second = module("second", List.of(new MechanicCaptureReaction("second.react", Set.of(),
                Set.of("first.react"), death -> {
            PieceState killer = death.context().pieceById(death.killerId());
            assertEquals(2, killer.blindTurns());
            return HeroEffect.success(Map.of(killer.id(), killer.withBlindness(3)));
        })));
        var definition = new HeroDefinition<>(HeroType.NONE, "Reactive", Set.of(Rook.class), NoHeroRules.class,
                NoHeroRules.INSTANCE, null, rules -> null, List.of(second, first));
        var hero = new AbstractHero(definition, List.<Abilities>of(), null, 0) {
            @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                    PieceState target, AbilityRequest request) { return HeroEffect.empty(); }
            @Override public HeroEffect onCaptured(HeroActionContext context, PieceState victim, long killer, model.Position square) {
                PieceState winner = context.pieceById(killer);
                return HeroEffect.success(Map.of(killer, winner.withBlindness(1)));
            }
        };
        Field field = new Field(8);
        PieceState victim = PieceState.initial(1, field.getFigureByPosition(pos("a1")), HeroType.NONE);
        PieceState killer = PieceState.initial(2, field.getFigureByPosition(pos("h8")), HeroType.NONE);
        var context = new HeroActionContext(field, Map.of(1L, victim.hidden(PieceState.Status.CAPTURED), 2L, killer), 1);
        HeroEffect result = CaptureReactions.resolve(context, victim, killer.id(), victim.position(),
                new HeroRegistry().register(hero));
        assertEquals(Success.SUCCESS, result.result());
        assertEquals(3, result.updates().get(killer.id()).blindTurns());
        assertEquals(killer, context.pieceById(killer.id()));
    }

    @Test public void invalidReactionGraphsFailDuringRegistration() {
        var empty = new MechanicCaptureReaction("same", death -> HeroEffect.empty());
        assertThrows(IllegalArgumentException.class, () -> new MechanicRegistry(List.of(
                module("a", List.of(empty)), module("b", List.of(empty)))));
        var missing = new MechanicCaptureReaction("missing", Set.of(), Set.of("absent"), death -> HeroEffect.empty());
        assertThrows(IllegalArgumentException.class, () -> new MechanicRegistry(List.of(module("m", List.of(missing)))));
        var a = new MechanicCaptureReaction("a", Set.of(), Set.of("b"), death -> HeroEffect.empty());
        var b = new MechanicCaptureReaction("b", Set.of(), Set.of("a"), death -> HeroEffect.empty());
        assertThrows(IllegalArgumentException.class, () -> new MechanicRegistry(List.of(module("cycle", List.of(a, b)))));
    }
}
