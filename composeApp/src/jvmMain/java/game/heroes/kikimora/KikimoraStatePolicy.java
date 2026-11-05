package game.heroes.kikimora;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Bishop;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class KikimoraStatePolicy implements HeroStatePolicy<KikimoraRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Bishop.class)).withStartingColor();

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, KikimoraRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, KikimoraRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.yarnCooldown(), rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, KikimoraRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
