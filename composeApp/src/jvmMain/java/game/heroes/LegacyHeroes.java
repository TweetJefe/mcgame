package game.heroes;

import game.heroes.pudge.PudgeHero;

/** Temporary factory used by descriptions of heroes awaiting their behavior migration. */
public final class LegacyHeroes {
    private LegacyHeroes() {}

    public static Hero create(HeroType type, HeroRules configuration) {
        return new LegacyHero(type, configuration, new PudgeHero(configuration.pudge()));
    }
}
