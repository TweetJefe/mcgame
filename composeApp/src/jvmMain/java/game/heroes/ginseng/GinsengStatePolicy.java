package game.heroes.ginseng;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Rook;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class GinsengStatePolicy implements HeroStatePolicy<GinsengRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Bishop.class, Knight.class, Rook.class)).withStartingColor().withBog().withStorage();

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, GinsengRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, GinsengRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.displacementCooldown(), rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, GinsengRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
