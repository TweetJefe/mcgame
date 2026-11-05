package game.heroes.faun;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Knight;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class FaunDefinition {
    private FaunDefinition() {}

    public static final HeroDefinition<FaunRules> INSTANCE = new HeroDefinition<>(
            HeroType.FAUN, "Faun-Конь", Set.of(Knight.class),
            FaunRules.class, FaunRules.prototype(), new FaunRulesCodec(),
            FaunHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new FaunStatePolicy())
            .withDraftProfile(DraftProfile.pickable(7));
}
