package game.heroes.chronomlin;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Pawn;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class ChronomlinDefinition {
    private ChronomlinDefinition() {}

    public static final HeroDefinition<ChronomlinRules> INSTANCE = new HeroDefinition<>(
            HeroType.CHRONOMLIN, "Chronomlin-Пешка", Set.of(Pawn.class),
            ChronomlinRules.class, ChronomlinRules.prototype(), new ChronomlinRulesCodec(),
            ChronomlinHero::new, List.of(HistoricalMechanics.INSTANCE, ChronomlinFormMechanic.INSTANCE))
            .withStatePolicy(new ChronomlinStatePolicy())
            .withDraftProfile(DraftProfile.pickable(12));
}
