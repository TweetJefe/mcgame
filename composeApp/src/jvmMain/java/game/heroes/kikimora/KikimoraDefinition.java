package game.heroes.kikimora;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Bishop;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class KikimoraDefinition {
    private KikimoraDefinition() {}

    public static final HeroDefinition<KikimoraRules> INSTANCE = new HeroDefinition<>(
            HeroType.KIKIMORA, "Kikimora-Слон", Set.of(Bishop.class),
            KikimoraRules.class, KikimoraRules.prototype(), new KikimoraRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.KIKIMORA, configuration));
}
