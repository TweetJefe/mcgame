package game.heroes.faun;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Knight;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class FaunDefinition {
    private FaunDefinition() {}

    public static final HeroDefinition<FaunRules> INSTANCE = new HeroDefinition<>(
            HeroType.FAUN, "Faun-Конь", Set.of(Knight.class),
            FaunRules.class, FaunRules.prototype(), new FaunRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.FAUN, configuration));
}
