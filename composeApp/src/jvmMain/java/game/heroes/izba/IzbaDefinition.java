package game.heroes.izba;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Rook;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class IzbaDefinition {
    private IzbaDefinition() {}

    public static final HeroDefinition<IzbaRules> INSTANCE = new HeroDefinition<>(
            HeroType.IZBA, "Изба-Ладья", Set.of(Rook.class),
            IzbaRules.class, IzbaRules.prototype(), new IzbaRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.IZBA, configuration));
}
