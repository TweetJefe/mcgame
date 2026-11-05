package game.heroes.ginseng;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.heroes.pudge.PudgeAbility;
import game.mechanics.BuiltInStates;
import game.moves.AbilityRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.HashMap;
import java.util.Map;
import model.Field;
import model.FigureColor;
import model.figures.Knight;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;

public class GinsengHeroTest {
    @Test public void selfDisplacementAppliesCostToTheMovedStateWithoutLosingLastingFields() {
        var snapshot = GinsengFixtures.hook().getSnapshot();
        var actor = snapshot.pieceAt(pos("c2")).withAbilityState(0, 4, snapshot.pieceAt(pos("e2")).id())
                .withBlindness(1).withStun(7);
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(actor.id(), actor);
        var context = new HeroActionContext(snapshot.field(), pieces, 1, snapshot.mechanics());
        var hero = new GinsengHero(new GinsengRules(6, 4));
        var request = new AbilityRequest(actor.position(), GinsengAbility.DISPLACE, actor.position(), pos("c3"), false);
        var effect = hero.preview(context, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(actor.placed(pos("c3"), actor.type()).withAbilityState(6, 4, actor.storedPieceId()),
                effect.updates().get(actor.id()));
        assertEquals(effect, hero.preview(context, request));
        assertEquals(actor, context.pieceById(actor.id()));
    }

    @Test public void canonicalVacancyUsesThePieceSnapshotAndRawAvailabilityKeepsItsHistoricalIdentityGuard() {
        var snapshot = GinsengFixtures.hook().getSnapshot();
        var actor = snapshot.pieceAt(pos("c2"));
        var blocker = PieceState.initial(100, new Field(8).createFigure(Knight.class, pos("d2"), FigureColor.WHITE), HeroType.NONE);
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(blocker.id(), blocker);
        var context = new HeroActionContext(snapshot.field(), pieces, 1, snapshot.mechanics());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,
                new GinsengHero(GinsengRules.prototype()).preview(context,
                        new AbilityRequest(actor.position(), GinsengAbility.DISPLACE, actor.position(), pos("d2"), false)).result());
        assertEquals(AbilityRejection.NO_ABILITY, GinsengAbilities.availability(snapshot.pieceAt(pos("e2")),
                GinsengAbility.DISPLACE, GinsengRules.prototype(), true));
    }

    @Test public void copySpendsOnlyTheConfiguredChargeAndKeepsTheCanonicalSourceMetadata() {
        var snapshot = GinsengFixtures.hook().getSnapshot();
        var actor = snapshot.pieceAt(pos("c2")).withAbilityState(5, 4, null);
        var source = snapshot.pieceAt(pos("e2"));
        var pieces = new HashMap<>(snapshot.pieces());
        pieces.put(actor.id(), actor);
        var context = new HeroActionContext(snapshot.field(), pieces, 1, snapshot.mechanics().with(BuiltInStates.SPELLS,
                new SpellState(Map.of(source.id(), PudgeAbility.HOOK), Map.of())));
        var hero = new GinsengHero(new GinsengRules(6, 4));
        var request = new AbilityRequest(actor.position(), GinsengAbility.COPY_SPELL, source.position());
        var effect = hero.preview(context, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(actor.withAbilityState(5, 0, null), effect.updates().get(actor.id()));
        assertEquals(PudgeAbility.HOOK,
                hero.abilityEventDetails(context, context, actor, source, request).copiedAbility());
        assertEquals(actor, context.pieceById(actor.id()));
    }
}
