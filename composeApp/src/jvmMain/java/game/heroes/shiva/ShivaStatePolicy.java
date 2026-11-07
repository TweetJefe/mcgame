package game.heroes.shiva;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.figures.Rook;

public final class ShivaStatePolicy implements HeroStatePolicy<ShivaRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Rook.class));
    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, ShivaRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }
    @Override public void validate(PieceState piece, ShivaRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.lotusCooldown(), rules.ultimateCaptures());
    }
}
