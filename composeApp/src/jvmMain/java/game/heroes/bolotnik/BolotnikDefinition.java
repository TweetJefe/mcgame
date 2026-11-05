package game.heroes.bolotnik;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Knight;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class BolotnikDefinition {
    private BolotnikDefinition() {}

    public static final HeroDefinition<BolotnikRules> INSTANCE = new HeroDefinition<>(
            HeroType.BOLOTNIK, "Bolotnik-Конь", Set.of(Knight.class),
            BolotnikRules.class, BolotnikRules.prototype(), new BolotnikRulesCodec(),
            BolotnikHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new BolotnikStatePolicy())
            .withDraftProfile(DraftProfile.pickable(4));
}
