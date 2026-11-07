package game.heroes.supernova;

import game.heroes.Abilities;
import game.heroes.HeroTargeting;
import game.history.AbilityEventTarget;
import java.util.List;

public enum SupernovaAbility implements Abilities {
    PULL("supernova.pull", "Стянуть фигуры", area()),
    SHUFFLE("supernova.shuffle", "Перемешать фигуры", area());

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());
    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    SupernovaAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }
    public static List<Abilities> getAbilities() { return ABILITIES; }
    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
    @Override public AbilityEventTarget eventTarget() { return AbilityEventTarget.NONE; }
    
    private static HeroTargeting area() {
        return HeroTargeting.square(HeroTargeting.Permission.SELF, HeroTargeting.Permission.KING,
                HeroTargeting.Permission.INVISIBLE, HeroTargeting.Permission.POTTED,
                HeroTargeting.Permission.BOXED, HeroTargeting.Permission.ILLUSION);
    }

}
