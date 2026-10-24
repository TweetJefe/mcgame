package game.persistence;

import game.core.Game;
import game.heroes.Abilities;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroEffect;
import game.heroes.HeroRegistry;
import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRules;
import game.heroes.HeroRulesCodec;
import game.heroes.HeroTargeting;
import game.heroes.HeroType;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.SpellState;
import game.heroes.pudge.PudgeAbility;
import game.history.AbilityEventTarget;
import game.history.GameEvent;
import game.moves.Success;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.PieceState;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.Rook;
import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class GameEventXmlTest {
    private enum Extension implements Abilities {
        CUSTOM, SYSTEM_NAMED;
        @Override public String id() { return this == CUSTOM ? "extension.cast" : "MOVE"; }
        @Override public String getDisplayName() { return "Extension"; }
        @Override public HeroTargeting targeting() { return HeroTargeting.untargeted(); }
        @Override public AbilityEventTarget eventTarget() { return AbilityEventTarget.NONE; }
    }
    private record ExtensionRules(int marker) {}
    private static final HeroRulesCodec<ExtensionRules> CODEC = new HeroRulesCodec<>() {
        @Override public String section() { return "extensionRules"; }
        @Override public int sinceVersion() { return 15; }
        @Override public Map<String, String> encode(ExtensionRules rules) {
            return Map.of("marker", String.valueOf(rules.marker()));
        }
        @Override public ExtensionRules decode(HeroRuleAttributes attributes) {
            return new ExtensionRules(attributes.integer("marker"));
        }
    };
    // Reuse NONE as a test-only identity; registering a production hero adds a new HeroType value.
    private static final HeroDefinition<ExtensionRules> DEFINITION = new HeroDefinition<>(
            HeroType.NONE, "Extension", Set.of(Rook.class), ExtensionRules.class,
            new ExtensionRules(1), CODEC, ExtensionHero::new);
    private static final class ExtensionHero extends AbstractHero {
        ExtensionHero(ExtensionRules rules) { super(DEFINITION, List.of(Extension.values()), null, 0); }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                        PieceState target, Position square, Abilities ability) {
            return HeroEffect.empty();
        }
    }

    private static Element entry() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("event");
    }
    private static HeroRegistry registry() {
        return new HeroRegistry().register(new ExtensionHero(new ExtensionRules(1)))
                .register(HeroRegistry.standard().get(HeroType.GINSENG));
    }
    private static GameEvent cast(Abilities ability) {
        return GameEvent.abilityUsed(1, 1, ability, FigureColor.WHITE, 1L, null,
                new Position(0, 0), new Position(0, 0), "cast");
    }

    @Test public void arbitraryAndSystemNamedAbilityIdsRoundtripIndependentlyOfCategory() throws Exception {
        for (Extension ability : Extension.values()) {
            GameEvent event = cast(ability);
            Element entry = entry();
            GameEventXml.write(entry, event, 15);
            assertEquals("ABILITY_USED", entry.getAttribute("kind"));
            assertEquals(ability.id(), entry.getAttribute("abilityId"));
            assertEquals(event, GameEventXml.read(entry, 15, registry()));
            assertEquals(15, GameEventXml.requiredVersion(List.of(event)));
        }
    }

    @Test public void allHistoricalAbilityNamesStillUseTheirOriginalWireRepresentation() throws Exception {
        HeroRegistry heroes = HeroRegistry.standard();
        for (HeroType type : HeroType.values())
            for (Abilities ability : heroes.get(type).abilities()) {
                GameEvent event = cast(ability);
                int version = GameEventXml.requiredVersion(List.of(event));
                assertTrue(version < 15);
                Element entry = entry();
                GameEventXml.write(entry, event, version);
                assertEquals(ability.id(), entry.getAttribute("kind"));
                assertFalse(entry.hasAttribute("abilityId"));
                assertEquals(event, GameEventXml.read(entry, version, heroes));
            }
    }

    @Test public void borrowedLegacyAbilityCannotLoseItsChannelInAnOlderFormat() throws Exception {
        GameEvent event = GameEvent.abilityUsed(1, 3, PudgeAbility.HOOK, FigureColor.WHITE,
                2L, 3L, new Position(0, 0), new Position(0, 1), "borrowed", null, true);
        assertEquals(7, GameEventXml.requiredVersion(List.of(event)));
        assertThrows(IllegalArgumentException.class, () -> GameEventXml.write(entry(), event, 1));
        Element entry = entry();
        GameEventXml.write(entry, event, 7);
        assertEquals(event, GameEventXml.read(entry, 7, HeroRegistry.standard()));
    }

    @Test public void genericEventsRejectMissingUnknownAmbiguousOrPrematureIds() throws Exception {
        Element entry = entry();
        GameEventXml.write(entry, cast(Extension.CUSTOM), 15);
        assertThrows(IllegalArgumentException.class, () -> GameEventXml.read(entry, 14, registry()));
        entry.removeAttribute("abilityId");
        assertThrows(IllegalArgumentException.class, () -> GameEventXml.read(entry, 15, registry()));
        entry.setAttribute("abilityId", "unregistered");
        assertThrows(IllegalArgumentException.class, () -> GameEventXml.read(entry, 15, registry()));
        entry.setAttribute("kind", "MOVE");
        assertThrows(IllegalArgumentException.class, () -> GameEventXml.read(entry, 15, registry()));
        entry.setAttribute("kind", "HOOK");
        assertThrows(IllegalArgumentException.class, () -> GameEventXml.read(entry, 15, HeroRegistry.standard()));
        assertThrows(IllegalArgumentException.class, () -> GameEventXml.write(entry(), cast(Extension.CUSTOM), 14));
    }

    @Test public void copyingANewAbilitySurvivesWireReplayAndConsumption() throws Exception {
        List<GameEvent> original = List.of(cast(Extension.CUSTOM),
                GameEvent.abilityUsed(2, 1, GinsengAbility.COPY_SPELL, FigureColor.WHITE,
                        2L, 1L, new Position(1, 0), new Position(0, 0), "copy", Extension.CUSTOM, false),
                GameEvent.abilityUsed(3, 3, Extension.CUSTOM, FigureColor.WHITE,
                        2L, null, new Position(1, 0), new Position(1, 0), "borrowed", null, true));
        var restored = new ArrayList<GameEvent>();
        for (GameEvent event : original) {
            Element entry = entry();
            GameEventXml.write(entry, event, 15);
            restored.add(GameEventXml.read(entry, 15, registry()));
        }
        assertEquals(original, restored);
        assertSame(Extension.CUSTOM, SpellState.fromHistory(restored.subList(0, 2)).borrowed().get(2L).ability());
        SpellState spent = SpellState.fromHistory(restored);
        assertTrue(spent.borrowed().isEmpty());
        assertSame(Extension.CUSTOM, spent.lastUsed().get(2L));
    }

    @Test public void gameExecutesSavesRestoresAndContinuesWithANewRegisteredAbility() throws Exception {
        var definitions = new ArrayList<>(HeroDefinitions.all());
        definitions.removeIf(definition -> definition.type() == HeroType.NONE);
        definitions.add(DEFINITION);
        HeroRules defaults = HeroRules.defaults(definitions);
        HeroRules configuration = defaults.with(DEFINITION, new ExtensionRules(7));
        Game game = Game.fromMobaPosition(new GamePosition(new Field(8).getGameField(), FigureColor.WHITE,
                CastlingRights.initial(), null, 0), Map.of(), configuration, () -> 0L);
        Position actor = new Position(0, 0);
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), actor, null, Extension.CUSTOM));
        GameEvent event = game.getSnapshot().events().getLast();
        assertEquals(GameEvent.Kind.ABILITY_USED, event.kind());
        assertSame(Extension.CUSTOM, event.ability());
        byte[] saved = GameSaveFile.encode(game.getSnapshot());
        String xml = new String(saved, StandardCharsets.UTF_8);
        assertTrue(xml.contains("formatVersion=\"15\""));
        assertTrue(xml.contains("abilityId=\"extension.cast\""));
        assertThrows(IOException.class, () -> GameSaveFile.decode(saved));
        Game loaded = Game.fromSnapshot(GameSaveFile.decode(saved, defaults), () -> 0L);
        assertEquals(configuration, loaded.getHeroRules());
        assertSame(Extension.CUSTOM, loaded.getSpells().lastUsed().get(loaded.getPieceState(actor).id()));
        assertArrayEquals(saved, GameSaveFile.encode(loaded.getSnapshot()));
        assertEquals(Success.SUCCESS, loaded.moveFigure(loaded.getCurrentPlayer(), new Position(1, 0), new Position(2, 0)));
        assertEquals(Success.SUCCESS, loaded.endTurn(loaded.getCurrentPlayer()));
    }
}
