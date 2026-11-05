package game.heroes.moleminer;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Pawn;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class MoleminerDefinition {
    private MoleminerDefinition() {}

    public static final HeroDefinition<MoleminerRules> INSTANCE = new HeroDefinition<>(
            HeroType.MOLEMINER, "Moleminer-Пешка", Set.of(Pawn.class),
            MoleminerRules.class, MoleminerRules.prototype(), new MoleminerRulesCodec(),
            MoleminerHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new MoleminerStatePolicy())
            .withDraftProfile(DraftProfile.pickable(10));
}
