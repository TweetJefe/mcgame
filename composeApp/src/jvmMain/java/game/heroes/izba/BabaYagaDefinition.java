package game.heroes.izba;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import game.heroes.NoHeroRules;
import model.figures.Pawn;

import java.util.Set;

public final class BabaYagaDefinition {
    private BabaYagaDefinition() {}

    public static final HeroDefinition<NoHeroRules> INSTANCE = new HeroDefinition<>(
            HeroType.BABA_YAGA, "Баба-яга", Set.of(Pawn.class),
            NoHeroRules.class, NoHeroRules.INSTANCE, null,
            (parameters, configuration) -> LegacyHeroes.create(HeroType.BABA_YAGA, configuration));
}
