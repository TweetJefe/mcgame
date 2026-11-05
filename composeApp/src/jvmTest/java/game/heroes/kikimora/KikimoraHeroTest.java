package game.heroes.kikimora;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.HashMap;
import model.Field;
import model.FigureColor;
import model.figures.Knight;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;

public class KikimoraHeroTest {
    @Test public void yarnKeepsTheTwoTargetsOwnDeadlinesAndChargesTheCasterOnce() {
        var snapshot = KikimoraTest.basic().getSnapshot();
        var actor = snapshot.pieceAt(pos("c2"));
        var pieces = new HashMap<>(snapshot.pieces());
        var second = PieceState.initial(100, new Field(8).createFigure(Knight.class, pos("e4"), FigureColor.WHITE), HeroType.NONE);
        pieces.put(second.id(), second);
        var context = new HeroActionContext(snapshot.field(), pieces, 5, snapshot.mechanics());
        var hero = new KikimoraHero(new KikimoraRules(7, 2));
        var request = new AbilityRequest(actor.position(), KikimoraAbility.YARN, pos("d3"));
        var effect = hero.preview(context, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(7, effect.updates().get(actor.id()).cooldown());
        assertEquals(8, effect.updates().get(snapshot.pieceAt(pos("d3")).id()).stunnedThroughTurn());
        assertEquals(9, effect.updates().get(second.id()).stunnedThroughTurn());
        assertEquals(effect, hero.preview(context, request));
        assertEquals(snapshot.pieces().get(actor.id()), context.pieceById(actor.id()));
    }

    @Test public void pottedCasterIsRejectedBeforeAbilityIdentity() {
        var foreign = PieceState.initial(1, new Field(8).createFigure(Knight.class, pos("c2"), FigureColor.WHITE),
                HeroType.NONE).asPot();

        assertEquals(StatusRejection.POTTED,
                new KikimoraHero(KikimoraRules.prototype()).availability(foreign, KikimoraAbility.YARN));
    }

    @Test public void aCopiedYarnPreservesTheActualCastersOwnState() {
        var snapshot = KikimoraTest.basic().getSnapshot();
        var original = snapshot.pieceAt(pos("c2"));
        var actor = new PieceState(original.id(), original.type(), original.color(), original.position(), original.status(),
                HeroType.GINSENG, 4, 1, original.inventory(), snapshot.pieceAt(pos("b1")).id(), 1,
                original.startingColor(), 7);
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(actor.id(), actor);
        var context = new HeroActionContext(snapshot.field(), pieces, 5, snapshot.mechanics());
        var effect = new KikimoraHero(new KikimoraRules(7, 2)).preview(context,
                new AbilityRequest(actor.position(), KikimoraAbility.YARN, pos("d3"), null, true));
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(actor, effect.updates().get(actor.id()));
        assertEquals(6, effect.updates().get(snapshot.pieceAt(pos("d3")).id()).stunnedThroughTurn());
    }
}
