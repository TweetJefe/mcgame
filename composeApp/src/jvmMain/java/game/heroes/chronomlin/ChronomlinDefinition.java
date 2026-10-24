package game.heroes.chronomlin;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Pawn;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class ChronomlinDefinition {
    private ChronomlinDefinition() {}

    public static final HeroDefinition<ChronomlinRules> INSTANCE = new HeroDefinition<>(
            HeroType.CHRONOMLIN, "Chronomlin-Пешка", Set.of(Pawn.class),
            ChronomlinRules.class, ChronomlinRules.prototype(), new ChronomlinRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.CHRONOMLIN, configuration));
}
