package game.heroes.faun;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Knight;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class FaunStatePolicy implements HeroStatePolicy<FaunRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Knight.class));

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, FaunRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, FaunRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.cloudCooldown(), rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, FaunRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
