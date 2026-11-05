package game.heroes.ginseng;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Bishop;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class GinsengDefinition {
    private GinsengDefinition() {}

    public static final HeroDefinition<GinsengRules> INSTANCE = new HeroDefinition<>(
            HeroType.GINSENG, "Ginseng-Слон", Set.of(Bishop.class),
            GinsengRules.class, GinsengRules.prototype(), new GinsengRulesCodec(),
            GinsengHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new GinsengStatePolicy())
            .withDraftProfile(DraftProfile.pickable(6));
}
