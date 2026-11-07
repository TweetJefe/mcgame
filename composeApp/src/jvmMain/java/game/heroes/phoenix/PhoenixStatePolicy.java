package game.heroes.phoenix;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.figures.Egg;
import model.figures.Knight;

public final class PhoenixStatePolicy implements HeroStatePolicy<PhoenixRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Knight.class, Egg.class));
    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, PhoenixRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }
    @Override public void validate(PieceState piece, PhoenixRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, 0, rules.ultimateCaptures());
    }
}
