package game.heroes.supernova;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.figures.Archbishop;

/** Наследует предметы и действующие воздействия прежнего героя, включая хранившего союзника. */
public final class SupernovaStatePolicy implements HeroStatePolicy<SupernovaRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Archbishop.class))
            .withStorage().withBog();
    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, SupernovaRules rules) {
        return base;
    }
    @Override public void validate(PieceState piece, SupernovaRules rules) {
        CONSTRAINTS.validate(piece.withStartingColor(null));
        HeroStateConstraints.validateResources(piece, rules.pullCooldown(), rules.ultimateCaptures());
    }
}
