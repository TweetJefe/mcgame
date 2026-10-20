package game.heroes;

import game.moves.AbilityRejection;
import game.heroes.pudge.PudgeRules;
import game.moves.MoveResult;
import game.state.PieceState;
import model.Field;
import model.Position;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Compatibility facade for callers still using the static API. Dispatch belongs to HeroRegistry. */
public final class HeroAbilities {
    private static final HeroRegistry DEFAULTS = HeroRegistry.fromRules(HeroRules.prototype());

    private HeroAbilities() {}

    public static List<Ability> abilities(HeroType hero) {
        return DEFAULTS.get(hero).abilities();
    }

    public static MoveResult availability(PieceState actor, Ability ability, HeroRules rules) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        return HeroRegistry.fromRules(rules).get(actor.hero()).availability(actor, ability);
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square,
                                     Ability ability, HeroRules rules) {
        var pieces = new HashMap<Long, PieceState>();
        pieces.put(actor.id(), actor);
        if (target != null) pieces.put(target.id(), target);
        return HeroRegistry.fromRules(rules).get(actor.hero())
                .preview(new HeroActionContext(field, pieces, 0), actor, target, square, ability);
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Ability ability, PudgeRules rules) {
        return preview(field, actor, target, target == null ? null : target.position(), ability, HeroRules.withPudge(rules));
    }

    public static PieceState afterCapture(PieceState actor, HeroRules rules) {
        return HeroRegistry.fromRules(rules).get(actor.hero()).afterCapture(actor);
    }

    public static HeroEffect captureReaction(Field field, Map<Long, PieceState> pieces, PieceState victim,
                                             long captorId, Position square, HeroRules rules) {
        return CaptureReactions.resolve(field, pieces, victim, captorId, square, rules);
    }
}
