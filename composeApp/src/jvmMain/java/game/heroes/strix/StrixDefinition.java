package game.heroes.strix;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.DraftProfile;
import java.util.List;
import java.util.Set;
import model.figures.Pawn;

public final class StrixDefinition {
    private StrixDefinition() {}
    public static final HeroDefinition<StrixRules> INSTANCE = new HeroDefinition<>(
            HeroType.STRIX, "Strix-Пешка", Set.of(Pawn.class), StrixRules.class, StrixRules.prototype(),
            new StrixRulesCodec(), StrixHero::new, List.of(StrixMechanic.INSTANCE))
            .withStatePolicy(new StrixStatePolicy())
            .withDraftProfile(DraftProfile.pickable(15));
}
