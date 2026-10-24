package game.persistence;

import game.heroes.HeroType;
import game.scenarios.PudgeScenario;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class HeroTypeCodecTest {
    @Test public void heroNamesAreIndependentOfBodiesAndBothWireNamesResolve() {
        for (HeroType hero : HeroType.values()) {
            assertFalse(hero.name().matches(".*_(ROOK|BISHOP|KNIGHT|PAWN)$"));
            assertSame(hero, HeroTypeCodec.decode(hero.name()));
            assertSame(hero, HeroTypeCodec.decode(HeroTypeCodec.encode(hero)));
        }
        assertEquals("PUDGE_ROOK", HeroTypeCodec.encode(HeroType.PUDGE));
        assertEquals("GINSENG_BISHOP", HeroTypeCodec.encode(HeroType.GINSENG));
        assertEquals("BABA_YAGA_PAWN", HeroTypeCodec.encode(HeroType.BABA_YAGA));
        assertEquals("NONE", HeroTypeCodec.encode(HeroType.NONE));
        assertThrows(IllegalArgumentException.class, () -> HeroTypeCodec.decode("Pudge"));
        assertThrows(IllegalArgumentException.class, () -> HeroTypeCodec.decode("UNKNOWN"));
    }

    @Test public void canonicalHeroNameLoadsWithoutChangingLegacySaveContents() throws Exception {
        byte[] legacy = GameSaveFile.encode(PudgeScenario.DIAGONAL_HOOK.createGame(() -> 0L).getSnapshot());
        String canonical = new String(legacy, StandardCharsets.UTF_8).replace("PUDGE_ROOK", "PUDGE");
        var restored = GameSaveFile.decode(canonical.getBytes(StandardCharsets.UTF_8));
        assertTrue(restored.pieces().values().stream().anyMatch(piece -> piece.hero() == HeroType.PUDGE));
        assertArrayEquals(legacy, GameSaveFile.encode(restored));
    }
}
