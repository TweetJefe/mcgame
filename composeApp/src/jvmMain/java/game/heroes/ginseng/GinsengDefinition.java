package game.heroes.ginseng;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Bishop;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class GinsengDefinition {
    private GinsengDefinition() {}

    public static final HeroDefinition<GinsengRules> INSTANCE = new HeroDefinition<>(
            HeroType.GINSENG, "Ginseng-Слон", Set.of(Bishop.class),
            GinsengRules.class, GinsengRules.prototype(), new GinsengRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.GINSENG, configuration));
}
