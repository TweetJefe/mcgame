package game.heroes.chronomlin;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.heroes.ginseng.GinsengFixtures;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.HashMap;
import model.Field;
import model.FigureColor;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;

public class ChronomlinHeroTest {
    @Test public void nativeEvolutionKeepsCooldownAndLastingEffectsAndSpendsChargeOnce() {
        var snapshot = ChronomlinTest.basic().getSnapshot();
        var actor = snapshot.pieceAt(pos("c2")).withAbilityState(7, 2, null).withBlindness(1).withStun(8);
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(actor.id(), actor);
        var context = new HeroActionContext(snapshot.field(), pieces, 1, snapshot.mechanics());
        var hero = new ChronomlinHero(new ChronomlinRules(6, 2));
        var request = new AbilityRequest(actor.position(), ChronomlinAbility.EVOLVE, null);
        var effect = hero.preview(context, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(actor.placed(actor.position(), Bishop.class).withAbilityState(7, 0, null),
                effect.updates().get(actor.id()));
        assertEquals(effect, hero.preview(context, request));
        assertEquals(actor, context.pieceById(actor.id()));
    }

    @Test public void nativeRayUsesPreparedPieces() {
        var snapshot = ChronomlinTest.basic().getSnapshot();
        var actor = snapshot.pieceAt(pos("c2"));
        var target = snapshot.pieceAt(pos("c6"));
        var blocker = PieceState.initial(100, new Field(8).createFigure(Knight.class, pos("c3"), FigureColor.WHITE), HeroType.NONE);
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(blocker.id(), blocker);
        var hero = new ChronomlinHero(ChronomlinRules.prototype());
        assertEquals(AbilityRejection.ABILITY_BLOCKED, hero.preview(
                new HeroActionContext(snapshot.field(), pieces, 1, snapshot.mechanics()),
                new AbilityRequest(actor.position(), ChronomlinAbility.GRAPPLE, target.position())).result());

        assertEquals(StatusRejection.POTTED, hero.availability(actor.asPot(), ChronomlinAbility.GRAPPLE));
    }

    @Test public void copiedEvolutionUsesTheActualBodyAndPreservesTheCopyingHeroesResources() {
        var snapshot = GinsengFixtures.hook().getSnapshot();
        var actor = snapshot.pieceAt(pos("c2")).withAbilityState(4, 1, snapshot.pieceAt(pos("f2")).id())
                .withBlindness(1).withStun(7);
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(actor.id(), actor);
        var context = new HeroActionContext(snapshot.field(), pieces, 1, snapshot.mechanics());
        var hero = new ChronomlinHero(new ChronomlinRules(6, 2));
        var request = new AbilityRequest(actor.position(), ChronomlinAbility.EVOLVE, null, null, true);
        var effect = hero.preview(context, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(actor.placed(actor.position(), Knight.class), effect.updates().get(actor.id()));
        pieces.put(actor.id(), actor.placed(actor.position(), Rook.class).withAbilityState(9, 0, actor.storedPieceId()));
        assertEquals(AbilityRejection.MAX_EVOLUTION,
                hero.preview(new HeroActionContext(snapshot.field(), pieces, 1, snapshot.mechanics()), request).result());
    }
}
