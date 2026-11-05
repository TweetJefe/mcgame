package game.heroes;

import game.heroes.pudge.PudgeAbility;
import game.heroes.pudge.PudgeHero;
import game.heroes.pudge.PudgeRules;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import model.Field;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HeroRequestTest {
    private final Field field = new Field(8);
    private final PieceState actor = PieceState.initial(1, field.getFigureByPosition(pos("a1")), HeroType.PUDGE);
    private final PieceState target = PieceState.initial(2, field.getFigureByPosition(pos("a2")), HeroType.NONE);
    private final Hero hero = new PudgeHero(PudgeRules.sandbox());

    @Test public void canonicalPreviewFindsPiecesAndDoesNotMutateTheSnapshot() {
        var context = new HeroActionContext(field, Map.of(1L, actor, 2L, target), 7);
        var request = new AbilityRequest(pos("a1"), PudgeAbility.SWALLOW, pos("a2"));
        var first = hero.preview(context, request);
        assertEquals(Success.SUCCESS, first.result());
        assertEquals(PieceState.Status.SWALLOWED, first.updates().get(target.id()).status());
        assertEquals(first, hero.preview(context, request));
        assertSame(target, context.pieceAt(pos("a2")));
        assertSame(actor, context.pieceAt(pos("a1")));
        assertEquals(first, hero.previewRequest(context, actor, target, request));
    }

    @Test public void canonicalPreviewValidatesAvailabilityBeforeTargeting() {
        PieceState cooling = actor.withAbilityState(1, 0, null);
        var context = new HeroActionContext(field, Map.of(1L, cooling, 2L, target), 7);
        assertEquals(AbilityRejection.ABILITY_COOLDOWN, hero.preview(context,
                new AbilityRequest(pos("a1"), PudgeAbility.HOOK, null, pos("h8"), false)).result());
        assertEquals(AbilityRejection.INVALID_ABILITY, hero.preview(context, null).result());
        assertEquals(AbilityRejection.INVALID_ABILITY, hero.preview(context,
                new AbilityRequest(null, null, null)).result());
        assertEquals(MovementRejection.OUT_OF_BOUNDS, hero.preview(context,
                new AbilityRequest(null, PudgeAbility.HOOK, null)).result());
        assertEquals(MovementRejection.EMPTY_SOURCE, hero.preview(context,
                new AbilityRequest(pos("b3"), PudgeAbility.HOOK, null)).result());
    }

    @Test public void canonicalPreviewAppliesTheSharedTargetPolicy() {
        var context = new HeroActionContext(field, Map.of(1L, actor, 2L, target.withBox(4)), 7);
        assertEquals(StatusRejection.BOXED, hero.preview(context,
                new AbilityRequest(pos("a1"), PudgeAbility.HOOK, pos("a2"))).result());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, hero.preview(context,
                new AbilityRequest(pos("a1"), PudgeAbility.HOOK, pos("a2"), pos("b3"), false)).result());
        assertEquals(MovementRejection.OUT_OF_BOUNDS, hero.preview(context,
                new AbilityRequest(pos("a1"), PudgeAbility.HOOK, null)).result());
    }

    @Test public void fullRequestReachesTheSingleProtectedHookForBothChannels() {
        Hero strategy = new AbstractHero(HeroType.PUDGE, List.of(Shift.CAST), null, 0) {
            @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState caster,
                                                            PieceState victim, AbilityRequest request) {
                assertSame(actor, caster);
                assertSame(target, victim);
                return HeroEffect.success(Map.of(caster.id(), caster.withAbilityState(
                        request.borrowed() ? 8 : 3, 0, null), victim.id(), victim.placed(request.destination(), victim.type())));
            }
        };
        var context = new HeroActionContext(field, Map.of(1L, actor, 2L, target), 7);
        for (boolean borrowed : List.of(false, true)) {
            var request = new AbilityRequest(pos("a1"), Shift.CAST, pos("a2"), pos("b3"), borrowed);
            assertTrue(strategy.requests(context, actor, Shift.CAST, borrowed).contains(request));
            var effect = strategy.preview(context, request);
            assertEquals(Success.SUCCESS, effect.result());
            assertEquals(pos("b3"), effect.updates().get(target.id()).position());
            assertEquals(borrowed ? 8 : 3, effect.updates().get(actor.id()).cooldown());
        }
    }

    @Test public void detachedCompatibilityEntryPreservesItsOriginalPieceArguments() {
        var context = new HeroActionContext(field, Map.of(), 0);
        assertEquals(Success.SUCCESS, hero.preview(context, actor, target, target.position(), PudgeAbility.SWALLOW).result());
        assertEquals(Success.SUCCESS, hero.previewRequest(context, actor, target,
                new AbilityRequest(actor.position(), PudgeAbility.SWALLOW, target.position())).result());
        assertEquals(MovementRejection.EMPTY_SOURCE, hero.preview(context,
                new AbilityRequest(actor.position(), PudgeAbility.SWALLOW, target.position())).result());
    }

    private enum Shift implements Abilities {
        CAST;
        @Override public String id() { return "request.shift"; }
        @Override public String getDisplayName() { return "Shift"; }
        @Override public HeroTargeting targeting() { return HeroTargeting.targetAndDestination(); }
    }
}
