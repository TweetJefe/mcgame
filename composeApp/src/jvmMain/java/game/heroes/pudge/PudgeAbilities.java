package game.heroes.pudge;

import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.moves.MoveResult;
import game.state.PieceState;
import model.Field;
import model.Position;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Compatibility facade. Pudge's behavior is implemented exclusively by PudgeHero. */
public final class PudgeAbilities {
    private PudgeAbilities() {}

    public static List<Abilities> abilities() {
        return PudgeAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, PudgeRules rules) {
        return new PudgeHero(rules).availability(actor, ability);
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Abilities ability, PudgeRules rules) {
        return new PudgeHero(rules).legacyPreview(field, actor, target, ability);
    }

    public static PieceState afterCapture(PieceState actor, PudgeRules rules) {
        return new PudgeHero(rules).afterCapture(actor);
    }

    public static HeroEffect captureReaction(Field field, Map<Long, PieceState> pieces, PieceState victim,
                                             long captorId, Position square, UnaryOperator<PieceState> onCapture) {
        return new PudgeHero(PudgeRules.prototype()).legacyCaptureReaction(field, pieces, victim,
                captorId, square, onCapture);
    }
}
