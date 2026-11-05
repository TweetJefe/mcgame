package game.heroes;

import game.heroes.ginseng.GinsengFixtures;
import game.heroes.pudge.PudgeAbility;
import game.heroes.pudge.PudgeHero;
import game.heroes.pudge.PudgeRules;
import game.moves.AbilityRejection;
import game.moves.Success;
import game.state.PieceState;
import org.junit.Test;

import java.util.Map;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AbilityCostTest {
    @Test public void costChangesOnlyNativeResourcesAndKeepsTheProposedEffect() {
        var game = GinsengFixtures.hook();
        var actor = game.getPieceState(pos("c2")).withAbilityState(2, 2, 999L).withBlindness(1);
        var proposed = actor.placed(pos("d3"), actor.type());
        var raw = HeroEffect.success(Map.of(actor.id(), proposed)).withCost(new AbilityCost(5, 1));
        var nativeEffect = raw.applyCost(actor, AbilityUse.of(HeroType.GINSENG, false));
        assertEquals(proposed.withAbilityState(5, 1, 999L), nativeEffect.updates().get(actor.id()));
        assertEquals(AbilityCost.NONE, nativeEffect.cost());
        assertSame(nativeEffect, nativeEffect.applyCost(actor, AbilityUse.of(HeroType.GINSENG, false)));
        var copyEffect = raw.applyCost(actor, AbilityUse.of(HeroType.PUDGE, true));
        assertEquals(proposed, copyEffect.updates().get(actor.id()));
        assertEquals(actor, game.getPieceState(pos("c2")).withAbilityState(2, 2, 999L).withBlindness(1));
        assertEquals(new AbilityCost(5, 1), raw.cost());
    }

    @Test public void unavailableCopyKeepsSpecificConstraintsButIgnoresNativeCosts() {
        var game = GinsengFixtures.hook();
        var actor = game.getPieceState(pos("c2")).withAbilityState(2, 0, null);
        var hero = new PudgeHero(new PudgeRules(2, 3, true, true));
        var use = AbilityUse.of(HeroType.PUDGE, true);
        assertEquals(Success.SUCCESS, hero.availability(actor, PudgeAbility.HOOK, use));
        assertEquals(Success.SUCCESS, hero.availability(actor, PudgeAbility.SWALLOW, use));
        assertEquals(AbilityRejection.NO_ABILITY, hero.availability(actor, PudgeAbility.HOOK));
        assertEquals(AbilityRejection.ALREADY_SWALLOWED, hero.availability(
                actor.withAbilityState(2, 0, 999L), PudgeAbility.SWALLOW, use));
        assertEquals(AbilityRejection.NO_ABILITY, hero.availability(actor, PudgeAbility.HOOK,
                AbilityUse.of(HeroType.ZEUS, true)));
        PieceState target = game.getPieceState(pos("a4"));
        HeroEffect effect = hero.preview(new HeroActionContext(game.getGameField(),
                Map.of(actor.id(), actor, target.id(), target), 1), new AbilityRequest(actor.position(), PudgeAbility.HOOK, target.position(), null, true));
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(actor, effect.updates().get(actor.id()));
    }

    @Test public void rejectedEffectsCannotCarryCostsAndNegativeCostsAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AbilityCost.cooldown(-1));
        assertThrows(IllegalArgumentException.class, () -> AbilityCost.ultimate(-1));
        assertThrows(IllegalArgumentException.class, () -> HeroEffect.rejected(AbilityRejection.NO_ABILITY)
                .withCost(AbilityCost.cooldown(1)));
        assertTrue(HeroEffect.rejected(AbilityRejection.NO_ABILITY).cost().equals(AbilityCost.NONE));
    }
}
