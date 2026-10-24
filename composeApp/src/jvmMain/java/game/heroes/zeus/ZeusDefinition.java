package game.heroes.zeus;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import game.heroes.LegacyHeroes;
import model.figures.Rook;

import java.util.Set;

/** Registration metadata; behavior is created separately for each game's rules. */
public final class ZeusDefinition {
    private ZeusDefinition() {}

    public static final HeroDefinition<ZeusRules> INSTANCE = new HeroDefinition<>(
            HeroType.ZEUS, "Zeus-Ладья", Set.of(Rook.class),
            ZeusRules.class, ZeusRules.prototype(), new ZeusRulesCodec(),
            (parameters, configuration) -> LegacyHeroes.create(HeroType.ZEUS, configuration));
}
