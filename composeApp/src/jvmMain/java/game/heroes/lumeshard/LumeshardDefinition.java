package game.heroes.lumeshard;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Bishop;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class LumeshardDefinition {
    private LumeshardDefinition() {}

    public static final HeroDefinition<LumeshardRules> INSTANCE = new HeroDefinition<>(
            HeroType.LUMESHARD, "Lumeshard-Слон", Set.of(Bishop.class),
            LumeshardRules.class, LumeshardRules.prototype(), new LumeshardRulesCodec(),
            LumeshardHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new LumeshardStatePolicy())
            .withDraftProfile(DraftProfile.pickable(2));
}
