package game.heroes.sunwukong;

import game.heroes.Abilities;
import game.heroes.HeroTargeting;
import game.history.AbilityEventTarget;
import java.util.List;

public enum SunWukongAbility implements Abilities {
    SHAPESHIFT("sunwukong.shapeshift", "Случайная форма", HeroTargeting.untargeted()),
    CLONE("sunwukong.clone", "Создать клона", HeroTargeting.square());

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());
    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    SunWukongAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }

    public static List<Abilities> getAbilities() { return ABILITIES; }
    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
    @Override public AbilityEventTarget eventTarget() {
        return this == SHAPESHIFT ? AbilityEventTarget.NONE : AbilityEventTarget.REQUIRED;
    }
}
