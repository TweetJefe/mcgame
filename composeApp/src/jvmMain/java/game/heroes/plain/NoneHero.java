package game.heroes.plain;

import game.heroes.Abilities;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.NoHeroRules;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.List;
import java.util.Objects;
import model.Field;
import model.Position;

/** Сущность без активных способностей; жизненный цикл обслуживают её механики. */
public final class NoneHero extends AbstractHero implements LegacyPreview {
    public NoneHero(NoHeroRules rules) {
        super(NoneDefinition.INSTANCE, List.of(), null, 0);
        Objects.requireNonNull(rules, "rules");
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return HeroEffect.rejected(AbilityRejection.NO_ABILITY);
    }

    @Override
    protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                           PieceState target, AbilityRequest request) {
        return HeroEffect.rejected(AbilityRejection.NO_ABILITY);
    }
}
