package game.heroes.leshy;

import game.heroes.Abilities;
import game.heroes.HeroTargeting;
import game.history.AbilityEventTarget;
import java.util.List;

public enum LeshyAbility implements Abilities {
    VINE("leshy.vine", "Лоза"), FOREST("leshy.forest", "Превратить в деревья");
    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());
    private final String id;
    private final String displayName;
    LeshyAbility(String id, String displayName) { this.id = id; this.displayName = displayName; }
    public static List<Abilities> getAbilities() { return ABILITIES; }
    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return HeroTargeting.untargeted(); }
    @Override public AbilityEventTarget eventTarget() { return AbilityEventTarget.NONE; }
}
