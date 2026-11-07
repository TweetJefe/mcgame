package game.heroes.sunwukong;

import game.heroes.DraftProfile;
import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import java.util.List;
import java.util.Set;
import model.figures.Pawn;

public final class SunWukongDefinition {
    private SunWukongDefinition() {}
    public static final HeroDefinition<SunWukongRules> INSTANCE = new HeroDefinition<>(
            HeroType.SUN_WUKONG, "Sun Wukong-Пешка", Set.of(Pawn.class), SunWukongRules.class,
            SunWukongRules.prototype(), new SunWukongRulesCodec(), SunWukongHero::new,
            List.of(SunWukongMechanic.INSTANCE))
            .withStatePolicy(new SunWukongStatePolicy())
            .withDraftProfile(DraftProfile.pickable(14));
}
