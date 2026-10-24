package game.heroes.lumeshard;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Bishop;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class LumeshardDefinition {
    private LumeshardDefinition() {}

    public static final HeroDefinition<LumeshardRules> INSTANCE = new HeroDefinition<>(
            HeroType.LUMESHARD, "Lumeshard-Слон", Set.of(Bishop.class),
            LumeshardRules.class, LumeshardRules.prototype(), new LumeshardRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.LUMESHARD, configuration));
}
