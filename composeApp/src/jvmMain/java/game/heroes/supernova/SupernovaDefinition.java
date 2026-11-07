package game.heroes.supernova;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import java.util.List;
import java.util.Set;
import model.figures.Archbishop;

public final class SupernovaDefinition {
    private SupernovaDefinition() {}
    public static final HeroDefinition<SupernovaRules> INSTANCE = new HeroDefinition<>(
            HeroType.SUPERNOVA, "Supernova", Set.of(Archbishop.class),
            SupernovaRules.class, SupernovaRules.prototype(), new SupernovaRulesCodec(), SupernovaHero::new,
            List.of(SupernovaMechanic.INSTANCE))
            .withStatePolicy(new SupernovaStatePolicy());
}
