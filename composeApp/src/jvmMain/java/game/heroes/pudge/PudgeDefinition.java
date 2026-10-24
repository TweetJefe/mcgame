package game.heroes.pudge;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import model.figures.Rook;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class PudgeDefinition {
    private PudgeDefinition() {}

    public static final HeroDefinition<PudgeRules> INSTANCE = new HeroDefinition<>(
            HeroType.PUDGE, "Пудж-Ладья", Set.of(Rook.class),
            PudgeRules.class, PudgeRules.prototype(), new PudgeRulesCodec(),
            PudgeHero::new);
}
