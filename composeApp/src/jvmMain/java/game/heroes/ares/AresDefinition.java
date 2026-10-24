package game.heroes.ares;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Bishop;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class AresDefinition {
    private AresDefinition() {}

    public static final HeroDefinition<AresRules> INSTANCE = new HeroDefinition<>(
            HeroType.ARES, "Ares-Слон", Set.of(Bishop.class),
            AresRules.class, AresRules.prototype(), new AresRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.ARES, configuration));
}
