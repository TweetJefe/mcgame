package game.heroes.chronomlin;

import game.heroes.Abilities;
import game.history.AbilityEventTarget;
import game.heroes.HeroTargeting;

import java.util.List;

/** Stable IDs, labels and targeting for this hero's abilities. */
public enum ChronomlinAbility implements Abilities {
    GRAPPLE("GRAPPLE", "Притянуться", HeroTargeting.square(HeroTargeting.Permission.ILLUSION, HeroTargeting.Permission.KING, HeroTargeting.Permission.POTTED)),
    EVOLVE("EVOLVE", "Улучшиться", HeroTargeting.untargeted());

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());

    /** All abilities in declaration order, shared as an immutable list. */
    public static List<Abilities> getAbilities() { return ABILITIES; }

    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    ChronomlinAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }

    @Override public AbilityEventTarget eventTarget() {
        return switch (this) {
            case EVOLVE -> AbilityEventTarget.NONE;
            default -> AbilityEventTarget.REQUIRED;
        };
    }

    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
}
