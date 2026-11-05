package game.heroes.clownganger;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.NoHeroRules;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Rook;

public final class IllusionDefinition {
    private IllusionDefinition() {}

    public static final HeroDefinition<NoHeroRules> INSTANCE = new HeroDefinition<>(
            HeroType.ILLUSION, "Иллюзия", Set.of(Knight.class, Bishop.class, Rook.class),
            NoHeroRules.class, NoHeroRules.INSTANCE, null,
            IllusionHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new IllusionStatePolicy());
}
