package game.heroes.clownganger;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Knight;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class ClowngangerStatePolicy implements HeroStatePolicy<ClowngangerRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Knight.class));

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, ClowngangerRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, ClowngangerRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.boxCooldown(), rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, ClowngangerRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
