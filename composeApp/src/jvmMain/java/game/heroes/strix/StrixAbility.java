package game.heroes.strix;

import game.heroes.Abilities;
import game.heroes.HeroTargeting;
import game.history.AbilityEventTarget;
import java.util.List;

public enum StrixAbility implements Abilities {
    REWIND("strix.rewind", "Вернуть позицию", HeroTargeting.square(HeroTargeting.Permission.SELF,
            HeroTargeting.Permission.POTTED, HeroTargeting.Permission.ILLUSION)),
    TIME_SKIP("strix.timeSkip", "Пропустить ход врага", HeroTargeting.untargeted());

    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());
    private final String id;
    private final String displayName;
    private final HeroTargeting targeting;

    StrixAbility(String id, String displayName, HeroTargeting targeting) {
        this.id = id;
        this.displayName = displayName;
        this.targeting = targeting;
    }

    public static List<Abilities> getAbilities() { return ABILITIES; }
    @Override public String id() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public HeroTargeting targeting() { return targeting; }
    @Override public AbilityEventTarget eventTarget() {
        return this == TIME_SKIP ? AbilityEventTarget.NONE : AbilityEventTarget.REQUIRED;
    }
}
