package game.heroes.bolotnik;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Knight;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class BolotnikDefinition {
    private BolotnikDefinition() {}

    public static final HeroDefinition<BolotnikRules> INSTANCE = new HeroDefinition<>(
            HeroType.BOLOTNIK, "Bolotnik-Конь", Set.of(Knight.class),
            BolotnikRules.class, BolotnikRules.prototype(), new BolotnikRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.BOLOTNIK, configuration));
}
