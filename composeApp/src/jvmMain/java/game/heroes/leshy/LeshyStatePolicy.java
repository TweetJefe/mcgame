package game.heroes.leshy;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Queen;
import model.figures.Rook;

public final class LeshyStatePolicy implements HeroStatePolicy<LeshyRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(
            Set.of(Pawn.class, Bishop.class, Knight.class, Rook.class, Queen.class));
    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, LeshyRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }
    @Override public void validate(PieceState piece, LeshyRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.vineCooldown(), rules.ultimateCaptures());
    }
}
