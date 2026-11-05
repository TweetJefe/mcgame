package game.heroes.izba;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Rook;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class IzbaStatePolicy implements HeroStatePolicy<IzbaRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Rook.class));

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, IzbaRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, IzbaRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, 0, rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, IzbaRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
