package game.heroes.ares;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Bishop;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class AresDefinition {
    private AresDefinition() {}

    public static final HeroDefinition<AresRules> INSTANCE = new HeroDefinition<>(
            HeroType.ARES, "Ares-Слон", Set.of(Bishop.class),
            AresRules.class, AresRules.prototype(), new AresRulesCodec(),
            AresHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new AresStatePolicy())
            .withDraftProfile(DraftProfile.pickable(8));
}
