package game.heroes.sunwukong;

import game.heroes.RevivalPlan;
import game.heroes.HeroActionContext;
import game.heroes.HeroRules;
import game.heroes.HeroStateConstraints;
import game.heroes.HeroStatePolicy;
import game.heroes.PieceCreationContext;
import game.state.PieceState;
import java.util.Set;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Queen;
import model.figures.Rook;

public final class SunWukongStatePolicy implements HeroStatePolicy<SunWukongRules> {
    private static final HeroStateConstraints CONSTRAINTS = HeroStateConstraints.bodies(
            Set.of(Pawn.class, Bishop.class, Knight.class, Rook.class, Queen.class));

    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, SunWukongRules rules) {
        return CONSTRAINTS.initialize(base, origin);
    }
    @Override public RevivalPlan revival(PieceState corpse, HeroActionContext context,
            HeroRules configuration, SunWukongRules rules) {
        int cooldown = context.pieces().values().stream().filter(p -> p.hero() == corpse.hero()
                && p.color() == corpse.color() && !p.isRemoved()).mapToInt(PieceState::cooldown).max().orElse(0);
        return new RevivalPlan(corpse.type(), corpse.startingColor(), cooldown, null);
    }
    @Override public void validate(PieceState piece, SunWukongRules rules) {
        CONSTRAINTS.validate(piece);
        HeroStateConstraints.validateResources(piece, rules.shapeshiftCooldown(), rules.ultimateCaptures());
    }
}
