package game.heroes.phoenix;

import game.heroes.DraftProfile;
import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import java.util.List;
import java.util.Set;
import model.figures.Knight;

public final class PhoenixDefinition {
    private PhoenixDefinition() {}
    public static final HeroDefinition<PhoenixRules> INSTANCE = new HeroDefinition<>(
            HeroType.PHOENIX, "Phoenix", Set.of(Knight.class),
            PhoenixRules.class, PhoenixRules.prototype(), new PhoenixRulesCodec(), PhoenixHero::new,
            List.of(PhoenixMechanic.INSTANCE))
            .withStatePolicy(new PhoenixStatePolicy())
            .withDraftProfile(DraftProfile.pickable(16));
}
