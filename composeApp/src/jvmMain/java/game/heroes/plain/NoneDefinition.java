package game.heroes.plain;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.NoHeroRules;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;

public final class NoneDefinition {
    private NoneDefinition() {}

    public static final HeroDefinition<NoHeroRules> INSTANCE = new HeroDefinition<>(
            HeroType.NONE, "Обычная фигура", Set.of(),
            NoHeroRules.class, NoHeroRules.INSTANCE, null,
            NoneHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new NoneStatePolicy());
}
