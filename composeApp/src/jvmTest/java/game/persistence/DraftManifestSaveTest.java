package game.persistence;

import game.core.Game;
import game.draft.Draft;
import game.draft.DraftCatalogTest;
import game.draft.DraftRules;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static game.core.Positions.pos;
import static model.FigureColor.BLACK;
import static model.FigureColor.WHITE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DraftManifestSaveTest {
    private static Game game() {
        var draft = new Draft(new DraftRules(0, 1));
        draft.pick(WHITE, HeroType.PUDGE, pos("a1"));
        draft.pick(BLACK, HeroType.LUMESHARD, pos("c8"));
        return Game.mobaFromDraft(draft.getSnapshot(), draft.getHeroRules(), 600, () -> 0L);
    }

    @Test public void explicitRosterRoundtripsAndDoesNotExpandWhenMoreHeroesAreRegistered() throws Exception {
        var game = game();
        byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        String xml = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(xml.contains("formatVersion=\"17\""));
        assertTrue(xml.contains("<catalog>"));
        var loaded = Game.fromSnapshot(GameSaveFile.decode(bytes, DraftCatalogTest.extendedRules()), () -> 0L);
        assertEquals(game.getDraft().catalog().heroes(), loaded.getDraft().catalog().heroes());
        assertFalse(loaded.getDraft().catalog().heroes().contains(HeroType.ILLUSION));
        assertArrayEquals(bytes, GameSaveFile.encode(loaded.getSnapshot()));
        assertTrue(loaded.moveFigure(loaded.getCurrentPlayer(), pos("a2"), pos("a3")).isSuccess());
        assertTrue(loaded.endTurn(loaded.getCurrentPlayer()).isSuccess());
    }

    @Test public void malformedMissingOrPrematureExplicitCatalogsAreRejected() throws Exception {
        String xml = new String(GameSaveFile.encode(game().getSnapshot()), StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> GameSaveFile.decode(xml.replaceAll("(?s)<catalog>.*?</catalog>", "").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> GameSaveFile.decode(xml.replace("id=\"PUDGE\"", "id=\"UNKNOWN\"").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> GameSaveFile.decode(xml.replace("id=\"LUMESHARD\"", "id=\"PUDGE\"").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> GameSaveFile.decode(xml.replace("formatVersion=\"17\"", "formatVersion=\"16\"").getBytes(StandardCharsets.UTF_8)));
    }
}
