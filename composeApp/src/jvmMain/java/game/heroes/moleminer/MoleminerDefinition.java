package game.heroes.moleminer;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Pawn;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class MoleminerDefinition {
    private MoleminerDefinition() {}

    public static final HeroDefinition<MoleminerRules> INSTANCE = new HeroDefinition<>(
            HeroType.MOLEMINER, "Moleminer-Пешка", Set.of(Pawn.class),
            MoleminerRules.class, MoleminerRules.prototype(), new MoleminerRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.MOLEMINER, configuration));
}
