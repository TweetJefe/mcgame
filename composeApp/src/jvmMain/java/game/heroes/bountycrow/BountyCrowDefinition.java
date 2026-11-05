package game.heroes.bountycrow;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Pawn;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class BountyCrowDefinition {
    private BountyCrowDefinition() {}

    public static final HeroDefinition<BountyCrowRules> INSTANCE = new HeroDefinition<>(
            HeroType.BOUNTY_CROW, "Bounty Crow-Пешка", Set.of(Pawn.class),
            BountyCrowRules.class, BountyCrowRules.prototype(), new BountyCrowRulesCodec(),
            BountyCrowHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new BountyCrowStatePolicy())
            .withDraftProfile(DraftProfile.pickable(3));
}
