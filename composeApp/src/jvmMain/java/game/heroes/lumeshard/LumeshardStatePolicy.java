package game.heroes.lumeshard;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Bishop;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class LumeshardStatePolicy implements HeroStatePolicy<LumeshardRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Bishop.class)).withStartingColor();

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, LumeshardRules rules) {
        return origin.revival() ? base.withStartingColor(origin.source().startingColor()) : CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, LumeshardRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.laserCooldown(), rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, LumeshardRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
