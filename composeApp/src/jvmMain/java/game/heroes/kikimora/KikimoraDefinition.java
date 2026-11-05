package game.heroes.kikimora;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Bishop;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class KikimoraDefinition {
    private KikimoraDefinition() {}

    public static final HeroDefinition<KikimoraRules> INSTANCE = new HeroDefinition<>(
            HeroType.KIKIMORA, "Kikimora-Слон", Set.of(Bishop.class),
            KikimoraRules.class, KikimoraRules.prototype(), new KikimoraRulesCodec(),
            KikimoraHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new KikimoraStatePolicy())
            .withDraftProfile(DraftProfile.pickable(13));
}
