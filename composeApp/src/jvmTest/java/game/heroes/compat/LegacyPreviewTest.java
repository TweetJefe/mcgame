package game.heroes.compat;

import game.core.Game;
import game.heroes.AbstractHero;
import game.heroes.AbilityRequest;
import game.heroes.HeroAbilities;
import game.heroes.HeroActionContext;
import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroRegistry;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import game.heroes.UiExtensionFixtures;
import game.heroes.pudge.PudgeAbility;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.List;
import java.util.Map;
import model.Field;
import model.FigureColor;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LegacyPreviewTest {
    @Test public void limitedHistoricalEntryRetainsRejectionsWithoutMechanicContext() {
        Field field = new Field(8);
        for (HeroType type : List.of(HeroType.KIKIMORA, HeroType.ZEUS, HeroType.MOLEMINER,
                HeroType.CLOWNGANGER, HeroType.ARES, HeroType.GINSENG)) {
            HeroDefinition<?> definition = HeroDefinitions.get(type);
            var body = definition.supportedBodies().iterator().next();
            PieceState actor = PieceState.initial(1, field.createFigure(body, pos("d4"), FigureColor.WHITE), type);
            for (var ability : HeroRegistry.standard().get(type).abilities()) {
                var effect = HeroAbilities.preview(field, actor, null, null, ability, HeroRules.defaults());
                assertEquals(type + "/" + ability.id(), AbilityRejection.INVALID_ABILITY_TARGET, effect.result());
                assertTrue(effect.updates().isEmpty());
                assertTrue(effect.operations().isEmpty());
            }
        }
    }

    @Test public void compatibilityDoesNotBypassCanonicalActorGuards() {
        Field field = new Field(8);
        PieceState actor = PieceState.initial(1, field.createFigure(Rook.class, pos("d4"), FigureColor.WHITE), HeroType.NONE).asPot();
        assertEquals(AbilityRejection.NO_ABILITY,
                HeroAbilities.preview(field, actor, null, null, PudgeAbility.HOOK, HeroRules.defaults()).result());
        var context = new HeroActionContext(field, Map.of(actor.id(), actor), 1);
        assertEquals(StatusRejection.POTTED, HeroRegistry.standard().get(actor.hero())
                .preview(context, new AbilityRequest(actor.position(), PudgeAbility.HOOK, null)).result());
        assertTrue(HeroRegistry.standard().get(actor.hero()) instanceof AbstractHero);
    }

    @Test public void independentDefinitionsNeedNoHistoricalRegistration() {
        Game game = UiExtensionFixtures.game();
        var actor = game.getPieceState(pos("a1"));
        var rules = game.getHeroRules();
        assertSame(UiExtensionFixtures.DEFINITION, LegacyHeroes.create(actor.hero(), rules).definition());
        var effect = HeroAbilities.preview(game.getGameField(), actor, null, null, UiExtensionFixtures.Spell.CAST, rules);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(2, effect.updates().get(actor.id()).cooldown());
        assertEquals(1, effect.updates().get(actor.id()).blindTurns());
        assertEquals(0, game.getPieceState(actor.position()).cooldown());
    }
}
