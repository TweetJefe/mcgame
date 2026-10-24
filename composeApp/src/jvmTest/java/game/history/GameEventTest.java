package game.history;

import game.heroes.Abilities;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.SpellState;
import model.FigureColor;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class GameEventTest {
    private enum Extension implements Abilities {
        RENAMED_CONSTANT;
        @Override public String id() { return "extension.cast"; }
        @Override public String getDisplayName() { return "Extension"; }
    }

    @Test public void categoryDoesNotDependOnTheAbilityIdOrItsEnumName() {
        GameEvent event = GameEvent.abilityUsed(1, 1, Extension.RENAMED_CONSTANT, FigureColor.WHITE,
                1L, 2L, null, null, "custom");
        assertEquals(GameEvent.Kind.ABILITY_USED, event.kind());
        assertEquals("extension.cast", event.abilityId());
        assertSame(Extension.RENAMED_CONSTANT, event.ability());
        assertTrue(event.isAbility(Extension.RENAMED_CONSTANT));
        assertFalse(event.isAbility(GinsengAbility.COPY_SPELL));
    }

    @Test public void actionCategoryAndSpellMetadataCannotDisagree() {
        assertThrows(IllegalArgumentException.class, () -> new GameEvent(1, 1, GameEvent.Kind.MOVE,
                FigureColor.WHITE, 1L, null, null, null, "bad", Extension.RENAMED_CONSTANT, null, false));
        assertThrows(IllegalArgumentException.class, () -> new GameEvent(1, 1, GameEvent.Kind.ABILITY_USED,
                FigureColor.WHITE, 1L, null, null, null, "bad"));
        assertThrows(IllegalArgumentException.class, () -> new GameEvent(1, 1, GameEvent.Kind.END_TURN,
                FigureColor.WHITE, null, null, null, null, "bad", null, Extension.RENAMED_CONSTANT, false));
        assertThrows(IllegalArgumentException.class, () -> new GameEvent(1, 1, GameEvent.Kind.END_TURN,
                FigureColor.WHITE, null, null, null, null, "bad", null, null, true));
    }

    @Test public void copyingAndConsumingANewAbilityNeedsNoGlobalKindEntry() {
        List<GameEvent> events = List.of(
                GameEvent.abilityUsed(1, 1, Extension.RENAMED_CONSTANT, FigureColor.WHITE,
                        1L, 3L, null, null, "source"),
                GameEvent.abilityUsed(2, 1, GinsengAbility.COPY_SPELL, FigureColor.WHITE,
                        2L, 1L, null, null, "copy", Extension.RENAMED_CONSTANT, false),
                GameEvent.abilityUsed(3, 3, Extension.RENAMED_CONSTANT, FigureColor.WHITE,
                        2L, 3L, null, null, "borrowed", null, true));
        SpellState copied = SpellState.fromHistory(events.subList(0, 2));
        assertSame(Extension.RENAMED_CONSTANT, copied.borrowed().get(2L).ability());
        SpellState spent = SpellState.fromHistory(events);
        assertTrue(spent.borrowed().isEmpty());
        assertSame(Extension.RENAMED_CONSTANT, spent.lastUsed().get(2L));
    }
}
