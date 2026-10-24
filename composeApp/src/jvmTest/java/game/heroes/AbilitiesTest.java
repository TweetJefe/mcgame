package game.heroes;

import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.SpellState;
import game.heroes.pudge.PudgeAbility;
import game.heroes.pudge.PudgeHero;
import game.heroes.pudge.PudgeRules;
import game.history.GameEvent;
import game.state.PieceState;
import model.FigureColor;
import model.Position;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class AbilitiesTest {
    @Test public void everyExistingSaveIdResolvesToItsOriginalHeroEnum() {
        HeroRegistry heroes = HeroRegistry.standard();
        Set<String> ids = new HashSet<>();
        for (HeroType type : HeroType.values())
            for (Abilities ability : heroes.get(type).abilities()) {
                assertTrue(ids.add(ability.id()));
                assertSame(ability, heroes.abilityById(ability.id()));
                assertSame(heroes.get(type), heroes.ownerOf(ability));
                assertFalse(ability.getDisplayName().isBlank());
            }
        // These identifiers are the persisted schema of the former common enum.
        assertEquals(Set.of("YARN", "CLAY_POT", "GRAPPLE", "EVOLVE", "CHAIN_LIGHTNING", "THUNDERSTORM",
                "PLANT_MINE", "DETONATE", "BOX", "ILLUSION", "HOOK", "SWALLOW", "LASER", "TELEPORT",
                "BOUNTY", "STEAL", "MUD", "AMBUSH", "SUMMON_YAGA", "DISPLACE", "COPY_SPELL",
                "CLOUD", "INVISIBILITY", "SPEAR"), ids);
        assertSame(PudgeAbility.HOOK, heroes.abilityById("HOOK"));
        assertSame(GinsengAbility.COPY_SPELL, heroes.abilityById("COPY_SPELL"));
        assertThrows(IllegalArgumentException.class, () -> heroes.abilityById("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> heroes.abilityById("hook"));
        assertNull(heroes.findAbility("MOVE"));
    }

    @Test public void registrationUsesStableIdsInsteadOfEnumNamesOrACentralEnumList() {
        HeroRegistry custom = new HeroRegistry().register(strategy(ExtensionAbility.RENAMED_CONSTANT));
        assertSame(ExtensionAbility.RENAMED_CONSTANT, custom.abilityById("extension.custom"));
        assertNull(custom.findAbility("RENAMED_CONSTANT"));
        assertSame(custom.get(HeroType.ZEUS), custom.ownerOf(ExtensionAbility.RENAMED_CONSTANT));
        // Distinct enum instances with equal persistence IDs are ambiguous and must fail immediately.
        HeroRegistry pudge = new HeroRegistry().register(new PudgeHero(PudgeRules.prototype()));
        assertThrows(IllegalArgumentException.class, () -> pudge.register(strategy(ConflictingAbility.OTHER_NAME)));
        assertThrows(IllegalArgumentException.class, () -> new HeroRegistry().register(strategy(InvalidAbility.BLANK)));
    }

    @Test public void replayRestoresCopiedSpellsAcrossDifferentHeroEnums() {
        List<GameEvent> events = List.of(
                GameEvent.abilityUsed(1, 1, PudgeAbility.HOOK, FigureColor.WHITE, 1L, 3L,
                        null, null, "hook"),
                GameEvent.abilityUsed(2, 1, GinsengAbility.COPY_SPELL, FigureColor.WHITE, 2L, 1L,
                        null, null, "copy", PudgeAbility.HOOK, false),
                GameEvent.abilityUsed(3, 3, PudgeAbility.HOOK, FigureColor.WHITE, 2L, 3L,
                        null, null, "borrowed hook", null, true));
        SpellState copied = SpellState.fromHistory(events.subList(0, 2));
        assertSame(GinsengAbility.COPY_SPELL, copied.lastUsed().get(2L));
        assertSame(PudgeAbility.HOOK, copied.borrowed().get(2L).ability());
        SpellState spent = SpellState.fromHistory(events);
        assertSame(PudgeAbility.HOOK, spent.lastUsed().get(2L));
        assertTrue(spent.borrowed().isEmpty());
        assertNull(SpellState.activeAbility(new GameEvent(1, 1, GameEvent.Kind.COUNTER_CAPTURE,
                FigureColor.WHITE, 1L, 2L, null, null, "reaction")));
        assertNull(SpellState.activeAbility(null));
    }

    private static Hero strategy(Abilities ability) {
        return new AbstractHero(HeroType.ZEUS, List.of(ability), null, 0) {
            @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                            PieceState target, Position square, Abilities selected) {
                return HeroEffect.empty();
            }
        };
    }

    private enum ExtensionAbility implements Abilities {
        RENAMED_CONSTANT;
        @Override public String id() { return "extension.custom"; }
        @Override public String getDisplayName() { return "Custom"; }
    }

    private enum ConflictingAbility implements Abilities {
        OTHER_NAME;
        @Override public String id() { return "HOOK"; }
        @Override public String getDisplayName() { return "Duplicate"; }
    }

    private enum InvalidAbility implements Abilities {
        BLANK;
        @Override public String id() { return " "; }
        @Override public String getDisplayName() { return "Invalid"; }
    }
}
