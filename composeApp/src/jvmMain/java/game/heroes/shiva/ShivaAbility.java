package game.heroes.shiva;

import game.history.AbilityEventTarget;
import game.heroes.Abilities;
import game.heroes.HeroTargeting;
import java.util.List;

public enum ShivaAbility implements Abilities {
    LOTUS("shiva.lotus", "Лотус", HeroTargeting.square()),
    REBIRTH("shiva.rebirth", "Воскрешение", HeroTargeting.untargeted());
    private static final List<Abilities> ABILITIES = Abilities.getAbilities(values());
    private final String id;
    private final String title;
    private final HeroTargeting targeting;
    ShivaAbility(String id, String title, HeroTargeting targeting) { this.id = id; this.title = title; this.targeting = targeting; }
    @Override public String id() { return id; }
    @Override public String getDisplayName() { return title; }
    @Override public HeroTargeting targeting() { return targeting; }
    @Override public AbilityEventTarget eventTarget() { return AbilityEventTarget.NONE; }
    public static List<Abilities> getAbilities() { return ABILITIES; }
}
