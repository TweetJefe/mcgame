package game.heroes.clownganger;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Knight;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class ClowngangerDefinition {
    private ClowngangerDefinition() {}

    public static final HeroDefinition<ClowngangerRules> INSTANCE = new HeroDefinition<>(
            HeroType.CLOWNGANGER, "Clownganger-Конь", Set.of(Knight.class),
            ClowngangerRules.class, ClowngangerRules.prototype(), new ClowngangerRulesCodec(),
            ClowngangerHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new ClowngangerStatePolicy())
            .withDraftProfile(DraftProfile.pickable(9));
}
