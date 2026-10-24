package game.heroes.bolotnik;

import game.heroes.Abilities;
import game.history.AbilityEventTarget;
import game.heroes.HeroTargeting;

import java.util.List;

/** Stable IDs, labels and targeting for this hero's abilities. */
public enum BolotnikAbility implements Abilities {
    MUD("MUD", "Грязь", HeroTargeting.square(HeroTargeting.Permission.ILLUSION, HeroTargeting.Permission.SELF, HeroTargeting.Permission.POTTED)),
    AMBUSH("AMBUSH", "Засада", HeroTargeting.untargeted());

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());

    /** All abilities in declaration order, shared as an immutable list. */
    public static List<Abilities> getAbilities() { return ABILITIES; }

    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    BolotnikAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }

    @Override public AbilityEventTarget eventTarget() {
        return switch (this) {
            case MUD -> AbilityEventTarget.OPTIONAL;
            case AMBUSH -> AbilityEventTarget.NONE;
            default -> AbilityEventTarget.REQUIRED;
        };
    }

    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
}
