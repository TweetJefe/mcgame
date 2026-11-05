package game.heroes.zeus;

import game.heroes.HeroDefinition;
import game.heroes.DraftProfile;
import game.heroes.HeroType;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Rook;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class ZeusDefinition {
    private ZeusDefinition() {}

    public static final HeroDefinition<ZeusRules> INSTANCE = new HeroDefinition<>(
            HeroType.ZEUS, "Zeus-Ладья", Set.of(Rook.class),
            ZeusRules.class, ZeusRules.prototype(), new ZeusRulesCodec(),
            ZeusHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new ZeusStatePolicy())
            .withDraftProfile(DraftProfile.pickable(11));
}
