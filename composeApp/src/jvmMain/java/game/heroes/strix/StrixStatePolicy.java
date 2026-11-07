package game.heroes.strix;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.figures.Pawn;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Rook;
import model.figures.Queen;

public final class StrixStatePolicy implements HeroStatePolicy<StrixRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(
            Set.of(Pawn.class, Bishop.class, Knight.class, Rook.class, Queen.class));

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, StrixRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }
    @Override public void validate(PieceState piece, StrixRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.rewindCooldown(), rules.ultimateCaptures());
    }
}
