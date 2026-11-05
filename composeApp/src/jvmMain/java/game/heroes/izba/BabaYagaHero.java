package game.heroes.izba;

import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.NoHeroRules;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.List;
import java.util.Objects;

/** Сущность без активных способностей; жизненный цикл обслуживают её механики. */
public final class BabaYagaHero extends AbstractHero {
    public BabaYagaHero(NoHeroRules rules) {
        super(BabaYagaDefinition.INSTANCE, List.of(), null, 0);
        Objects.requireNonNull(rules, "rules");
    }

    @Override
    protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                           PieceState target, AbilityRequest request) {
        return HeroEffect.rejected(AbilityRejection.NO_ABILITY);
    }
}
