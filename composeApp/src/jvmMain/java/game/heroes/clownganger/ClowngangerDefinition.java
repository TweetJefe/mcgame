package game.heroes.clownganger;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Knight;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class ClowngangerDefinition {
    private ClowngangerDefinition() {}

    public static final HeroDefinition<ClowngangerRules> INSTANCE = new HeroDefinition<>(
            HeroType.CLOWNGANGER, "Clownganger-Конь", Set.of(Knight.class),
            ClowngangerRules.class, ClowngangerRules.prototype(), new ClowngangerRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.CLOWNGANGER, configuration));
}
