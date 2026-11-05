package game.heroes.pudge;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.Position;
import model.figures.Rook;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class PudgeStatePolicy implements HeroStatePolicy<PudgeRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(Set.of(Rook.class)).withStorage();

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, PudgeRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, PudgeRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.hookCooldown(), rules.ultimateCaptures());
    }

    @Override public void validateOrigin(PieceState piece, Position origin, PudgeRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
