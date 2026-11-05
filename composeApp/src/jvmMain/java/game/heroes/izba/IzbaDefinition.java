package game.heroes.izba;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Rook;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class IzbaDefinition {
    private IzbaDefinition() {}

    public static final HeroDefinition<IzbaRules> INSTANCE = new HeroDefinition<>(
            HeroType.IZBA, "Изба-Ладья", Set.of(Rook.class),
            IzbaRules.class, IzbaRules.prototype(), new IzbaRulesCodec(),
            IzbaHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new IzbaStatePolicy())
            .withDraftProfile(DraftProfile.pickable(5));
}
