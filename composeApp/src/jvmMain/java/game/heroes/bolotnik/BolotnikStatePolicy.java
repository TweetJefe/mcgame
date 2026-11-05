package game.heroes.bolotnik;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Knight;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class BolotnikStatePolicy implements HeroStatePolicy<BolotnikRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Knight.class)).withBog();

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, BolotnikRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, BolotnikRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.mudCooldown(), rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, BolotnikRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
