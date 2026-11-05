package game.heroes.clownganger;

import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.List;
import model.Field;
import model.Position;

/** Совместимый фасад; геометрия и эффект реализованы в ClowngangerHero. */
public final class ClowngangerAbilities {
    private ClowngangerAbilities() {}

    public static List<Abilities> abilities() { return ClowngangerAbility.getAbilities(); }

    public static MoveResult availability(PieceState actor, Abilities ability, ClowngangerRules rules) {
        return availability(actor, ability, rules, false);
    }

    /** Сохраняет порядок проверок прежнего отдельного API. */
    public static MoveResult availability(PieceState actor, Abilities ability, ClowngangerRules rules, boolean borrowed) {
        if (actor == null || actor.position() == null) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.CLOWNGANGER) || !abilities().contains(ability))
            return AbilityRejection.NO_ABILITY;
        if (!borrowed && ability == ClowngangerAbility.BOX && actor.cooldown() > 0)
            return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == ClowngangerAbility.ILLUSION && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square,
            Abilities ability, long turn, ClowngangerRules rules, boolean borrowed) {
        return new ClowngangerHero(rules).detachedPreview(field, actor, target, square, ability, turn, borrowed);
    }
}
