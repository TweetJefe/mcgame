package game.heroes.phoenix;

import game.heroes.Abilities;
import game.heroes.HeroTargeting;
import game.history.AbilityEventTarget;
import java.util.List;

public enum PhoenixAbility implements Abilities {
    REBIRTH("phoenix.rebirth", "Яйцо феникса", HeroTargeting.untargeted());

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());
    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    PhoenixAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }
    public static List<Abilities> getAbilities() { return ABILITIES; }
    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
    @Override public AbilityEventTarget eventTarget() { return AbilityEventTarget.NONE; }
    
}
