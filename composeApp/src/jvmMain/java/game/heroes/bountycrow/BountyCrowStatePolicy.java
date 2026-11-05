package game.heroes.bountycrow;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Queen;
import model.figures.Rook;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class BountyCrowStatePolicy implements HeroStatePolicy<BountyCrowRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Pawn.class, Queen.class, Rook.class, Bishop.class, Knight.class));

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, BountyCrowRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, BountyCrowRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.bountyCooldown(), rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, BountyCrowRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
