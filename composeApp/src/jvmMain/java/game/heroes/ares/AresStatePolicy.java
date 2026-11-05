package game.heroes.ares;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Bishop;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class AresStatePolicy implements HeroStatePolicy<AresRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Bishop.class)).withStartingColor();

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, AresRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, AresRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.spearCooldown(), rules.revengeCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, AresRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
