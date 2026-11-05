package game.heroes.pudge;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Rook;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class PudgeDefinition {
    private PudgeDefinition() {}

    public static final HeroDefinition<PudgeRules> INSTANCE = new HeroDefinition<>(
            HeroType.PUDGE, "Пудж-Ладья", Set.of(Rook.class),
            PudgeRules.class, PudgeRules.prototype(), new PudgeRulesCodec(),
            PudgeHero::new, List.of(HistoricalMechanics.INSTANCE, SwallowMechanic.INSTANCE))
            .withStatePolicy(new PudgeStatePolicy())
            .withDraftProfile(DraftProfile.pickable(1));
}
