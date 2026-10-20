package game.heroes.kikimora;

import game.heroes.Abilities;
import game.heroes.HeroTargeting;

import java.util.List;

/** Stable IDs, labels and targeting for this hero's abilities. */
public enum KikimoraAbility implements Abilities {
    YARN("YARN", "Связать пряжей", HeroTargeting.square(HeroTargeting.Permission.ILLUSION)),
    CLAY_POT("CLAY_POT", "Глиняный горшок", HeroTargeting.square(HeroTargeting.Permission.ILLUSION));

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());

    /** All abilities in declaration order, shared as an immutable list. */
    public static List<Abilities> getAbilities() { return ABILITIES; }

    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    KikimoraAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }

    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
}
