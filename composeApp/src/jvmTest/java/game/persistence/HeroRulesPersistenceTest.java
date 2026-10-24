package game.persistence;

import game.core.Game;
import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRules;
import game.heroes.HeroRulesCodec;
import game.state.CastlingRights;
import game.state.GamePosition;
import model.Field;
import model.FigureColor;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class HeroRulesPersistenceTest {
    @Test public void allRegisteredRuleOverridesSurviveFullSaveAndLoad() throws Exception {
        for (HeroDefinition<?> definition : HeroDefinitions.all()) roundtrip(definition);
    }

    private static <R> void roundtrip(HeroDefinition<R> definition) throws Exception {
        if (definition.rulesCodec().isEmpty()) return;
        HeroRulesCodec<R> codec = definition.rulesCodec().orElseThrow();
        var attributes = new HashMap<>(codec.encode(definition.defaultRules()));
        attributes.replaceAll((key, value) -> value.equals("true") ? "false" : value.equals("false") ? "true"
                : String.valueOf(Integer.parseInt(value) + 1));
        R changed = codec.decode(new HeroRuleAttributes(attributes));
        HeroRules rules = HeroRules.defaults().with(definition, changed);
        Game game = Game.fromMobaPosition(new GamePosition(new Field(8).getGameField(), FigureColor.WHITE,
                CastlingRights.initial(), null, 0), Map.of(), rules, () -> 0L);
        byte[] saved = GameSaveFile.encode(game.getSnapshot());
        var restored = GameSaveFile.decode(saved);
        assertEquals(definition.type().name(), rules, restored.heroRules());
        assertEquals(changed, restored.heroRules().get(definition));
        assertArrayEquals(saved, GameSaveFile.encode(restored));
        Game loaded = Game.fromSnapshot(restored, () -> 0L);
        assertEquals(rules, loaded.getHeroRules());
    }

    @Test public void codecsRejectMissingOverflowingAndInvalidBooleanParameters() {
        var pudge = HeroDefinitions.get(game.heroes.HeroType.PUDGE).rulesCodec().orElseThrow();
        var valid = Map.of("hookCooldown", "2", "ultimateCaptures", "2",
                "hookDiagonals", "true", "swallowDiagonals", "true");
        for (String field : valid.keySet()) {
            var missing = new HashMap<>(valid);
            missing.remove(field);
            assertThrows(IllegalArgumentException.class, () -> pudge.decode(new HeroRuleAttributes(missing)));
        }
        var invalidBoolean = new HashMap<>(valid);
        invalidBoolean.put("hookDiagonals", "1");
        assertThrows(IllegalArgumentException.class, () -> pudge.decode(new HeroRuleAttributes(invalidBoolean)));
        var overflow = new HashMap<>(valid);
        overflow.put("hookCooldown", "2147483648");
        assertThrows(ArithmeticException.class, () -> pudge.decode(new HeroRuleAttributes(overflow)));
    }

    @Test public void saveReaderRejectsDuplicateAndPrematureRegisteredRuleSections() throws Exception {
        Game game = new Game(8, () -> 0L);
        String xml = new String(GameSaveFile.encode(game.getSnapshot()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("formatVersion=\"1\""));
        String duplicated = xml.replace("</moba-chess>", "<rules hookCooldown=\"2\"/></moba-chess>");
        assertThrows(IOException.class, () -> GameSaveFile.decode(duplicated.getBytes(StandardCharsets.UTF_8)));
        String premature = xml.replace("</moba-chess>", "<kikimoraRules yarnCooldown=\"3\" ultimateCaptures=\"2\"/></moba-chess>");
        assertThrows(IOException.class, () -> GameSaveFile.decode(premature.getBytes(StandardCharsets.UTF_8)));
    }
}
