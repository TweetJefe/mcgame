package game.heroes.shiva;

import game.heroes.DraftProfile;
import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import java.util.List;
import java.util.Set;
import model.figures.Rook;

public final class ShivaDefinition {
    private ShivaDefinition() {}
    public static final HeroDefinition<ShivaRules> INSTANCE = new HeroDefinition<>(
            HeroType.SHIVA, "Shiva", Set.of(Rook.class), ShivaRules.class, ShivaRules.prototype(),
            new ShivaRulesCodec(), ShivaHero::new, List.of(ShivaMechanic.INSTANCE))
            .withStatePolicy(new ShivaStatePolicy()).withDraftProfile(DraftProfile.pickable(18));
}
