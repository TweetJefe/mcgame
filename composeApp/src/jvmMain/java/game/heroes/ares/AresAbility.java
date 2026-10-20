package game.heroes.ares;

import game.heroes.Abilities;
import game.heroes.HeroTargeting;

import java.util.List;

/** Stable IDs, labels and targeting for this hero's abilities. */
public enum AresAbility implements Abilities {
    SPEAR("SPEAR", "Копьё", HeroTargeting.square(HeroTargeting.Permission.ILLUSION, HeroTargeting.Permission.KING, HeroTargeting.Permission.INVISIBLE, HeroTargeting.Permission.POTTED, HeroTargeting.Permission.BOXED));

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());

    /** All abilities in declaration order, shared as an immutable list. */
    public static List<Abilities> getAbilities() { return ABILITIES; }

    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    AresAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }

    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
}
