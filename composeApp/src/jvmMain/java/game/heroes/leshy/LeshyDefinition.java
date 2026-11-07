package game.heroes.leshy;

import game.heroes.DraftProfile;
import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import java.util.List;
import java.util.Set;
import model.figures.Pawn;

public final class LeshyDefinition {
    private LeshyDefinition() {}
    public static final HeroDefinition<LeshyRules> INSTANCE = new HeroDefinition<>(
            HeroType.LESHY, "Leshy", Set.of(Pawn.class), LeshyRules.class, LeshyRules.prototype(),
            new LeshyRulesCodec(), LeshyHero::new, List.of(LeshyMechanic.INSTANCE))
            .withStatePolicy(new LeshyStatePolicy()).withDraftProfile(DraftProfile.pickable(17));
}
