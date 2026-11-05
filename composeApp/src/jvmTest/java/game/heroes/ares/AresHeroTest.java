package game.heroes.ares;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.HashMap;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AresHeroTest {
    @Test public void spearChargesOnlyNativeCastsWhileKeepingTheSameCollisionGeometry() {
        var snapshot = AresTest.collision().getSnapshot();
        var actor = snapshot.pieceAt(pos("c2"));
        var context = new HeroActionContext(snapshot.field(), snapshot.pieces(), 5, snapshot.mechanics());
        var hero = new AresHero(new AresRules(6, 2));
        var nativeEffect = hero.preview(context, new AbilityRequest(actor.position(), AresAbility.SPEAR, pos("f2")));
        assertEquals(Success.SUCCESS, nativeEffect.result());
        assertEquals(actor.withAbilityState(6, actor.ultimateCharge(), null), nativeEffect.updates().get(actor.id()));
        var carried = snapshot.pieceAt(pos("d2"));
        assertEquals(pos("e2"), nativeEffect.updates().get(carried.id()).position());
        assertEquals(6, nativeEffect.updates().get(carried.id()).stunnedThroughTurn());
        var copier = new PieceState(actor.id(), actor.type(), actor.color(), actor.position(), actor.status(),
                HeroType.GINSENG, 4, 1, actor.inventory(), carried.id(), 1, actor.startingColor(), 7);
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(copier.id(), copier);
        var copied = hero.preview(new HeroActionContext(snapshot.field(), pieces, 5, snapshot.mechanics()),
                new AbilityRequest(copier.position(), AresAbility.SPEAR, pos("f2"), null, true));
        assertEquals(Success.SUCCESS, copied.result());
        assertEquals(copier, copied.updates().get(copier.id()));
        assertEquals(nativeEffect.updates().get(carried.id()), copied.updates().get(carried.id()));
        assertEquals(nativeEffect, hero.preview(context, new AbilityRequest(actor.position(), AresAbility.SPEAR, pos("f2"))));
    }

    @Test public void rawCallbacksKeepTheirOldPotSemanticsWithoutChangingTheNativeTemplate() {
        var actor = AresTest.basic().getPieceState(pos("c2")).asPot();
        var rules = new AresRules(6, 2);
        var hero = new AresHero(rules);
        assertEquals(Success.SUCCESS, AresAbilities.availability(actor, AresAbility.SPEAR, rules));
        assertEquals(StatusRejection.POTTED, hero.availability(actor, AresAbility.SPEAR));
        assertEquals(1, AresAbilities.afterCapture(actor, rules).ultimateCharge());
        assertEquals(actor, hero.afterCapture(actor));
    }

    @Test public void owningTheCopiedSpearDoesNotGiveTheCasterAresRevenge() {
        var snapshot = AresTest.collision().getSnapshot();
        var original = snapshot.pieceAt(pos("c2"));
        var copier = new PieceState(original.id(), original.type(), original.color(), original.position(), original.status(),
                HeroType.GINSENG, 0, 2, original.inventory(), null, 0, original.startingColor());
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(copier.id(), copier.hidden(PieceState.Status.CAPTURED));
        var context = new HeroActionContext(snapshot.field(), pieces, 1, snapshot.mechanics());
        var reaction = new AresHero(new AresRules(6, 2)).onCaptured(context, copier,
                snapshot.pieceAt(pos("f2")).id(), original.position());
        assertTrue(reaction.updates().isEmpty());
        assertTrue(reaction.captures().isEmpty());
        assertEquals(Success.SUCCESS, reaction.result());
    }
}
