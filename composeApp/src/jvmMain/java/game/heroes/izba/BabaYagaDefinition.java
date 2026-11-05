package game.heroes.izba;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.NoHeroRules;
import game.mechanics.compat.HistoricalMechanics;
import java.util.List;
import java.util.Set;
import model.figures.Pawn;

public final class BabaYagaDefinition {
    private BabaYagaDefinition() {}

    public static final HeroDefinition<NoHeroRules> INSTANCE = new HeroDefinition<>(
            HeroType.BABA_YAGA, "Баба-яга", Set.of(Pawn.class),
            NoHeroRules.class, NoHeroRules.INSTANCE, null,
            BabaYagaHero::new, List.of(HistoricalMechanics.INSTANCE))
            .withStatePolicy(new BabaYagaStatePolicy());
}
