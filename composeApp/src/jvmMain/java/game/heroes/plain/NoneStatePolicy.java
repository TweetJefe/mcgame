package game.heroes.plain;

import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.NoHeroRules;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import model.Position;

/** Собственные формы, поля и пределы ресурсов; связи с другими фигурами проверяют механики. */
public final class NoneStatePolicy implements HeroStatePolicy<NoHeroRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.basic();

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, NoHeroRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }

    @Override public void validate(PieceState piece, NoHeroRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, 0, 0);
    }

    @Override public void validateOrigin(PieceState piece, Position origin, NoHeroRules rules) {
        CONSTRAINTS.validateOrigin(piece, origin);
    }
}
