package game.heroes.bountycrow;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Pawn;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class BountyCrowDefinition {
    private BountyCrowDefinition() {}

    public static final HeroDefinition<BountyCrowRules> INSTANCE = new HeroDefinition<>(
            HeroType.BOUNTY_CROW, "Bounty Crow-Пешка", Set.of(Pawn.class),
            BountyCrowRules.class, BountyCrowRules.prototype(), new BountyCrowRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.BOUNTY_CROW, configuration));
}
