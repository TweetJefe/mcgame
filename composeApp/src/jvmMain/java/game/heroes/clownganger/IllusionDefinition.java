package game.heroes.clownganger;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import game.heroes.NoHeroRules;
import model.figures.Knight;
import model.figures.Bishop;
import model.figures.Rook;

import java.util.Set;

public final class IllusionDefinition {
    private IllusionDefinition() {}

    public static final HeroDefinition<NoHeroRules> INSTANCE = new HeroDefinition<>(
            HeroType.ILLUSION, "Иллюзия", Set.of(Knight.class, Bishop.class, Rook.class),
            NoHeroRules.class, NoHeroRules.INSTANCE, null,
            (parameters, configuration) -> LegacyHeroes.create(HeroType.ILLUSION, configuration));
}
