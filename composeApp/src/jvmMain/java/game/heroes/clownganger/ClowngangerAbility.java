package game.heroes.clownganger;

import game.heroes.Abilities;
import game.heroes.HeroTargeting;

import java.util.List;

/** Stable IDs, labels and targeting for this hero's abilities. */
public enum ClowngangerAbility implements Abilities {
    BOX("BOX", "Коробка", HeroTargeting.square(HeroTargeting.Permission.ILLUSION, HeroTargeting.Permission.SELF, HeroTargeting.Permission.KING)),
    ILLUSION("ILLUSION", "Иллюзия", HeroTargeting.square(HeroTargeting.Permission.ILLUSION));

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());

    /** All abilities in declaration order, shared as an immutable list. */
    public static List<Abilities> getAbilities() { return ABILITIES; }

    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    ClowngangerAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }

    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
}
