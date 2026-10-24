package game.heroes.plain;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import game.heroes.NoHeroRules;

import java.util.Set;

public final class NoneDefinition {
    private NoneDefinition() {}

    public static final HeroDefinition<NoHeroRules> INSTANCE = new HeroDefinition<>(
            HeroType.NONE, "Обычная фигура", Set.of(),
            NoHeroRules.class, NoHeroRules.INSTANCE, null,
            (parameters, configuration) -> LegacyHeroes.create(HeroType.NONE, configuration));
}
