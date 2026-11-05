package game.heroes.chronomlin;

import game.heroes.Abilities;
import game.heroes.AbilityUse;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.HashMap;
import java.util.List;
import model.Field;
import model.Figure;
import model.figures.Empty;

/** Совместимые входы для прежних клиентов; геометрия и правила находятся в ChronomlinHero. */
public final class ChronomlinAbilities {
    private ChronomlinAbilities() {}

    public static List<Abilities> abilities() {
        return ChronomlinAbility.getAbilities();
    }

    public static Class<? extends Figure> nextType(Class<? extends Figure> type) {
        return ChronomlinHero.nextType(type);
    }

    public static MoveResult availability(PieceState actor, Abilities ability, ChronomlinRules rules, boolean borrowed) {
        // Сохранён порядок проверок отдельной утилиты; игровой вход использует шаблон базы.
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.position() == null) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.CHRONOMLIN) || !abilities().contains(ability))
            return AbilityRejection.NO_ABILITY;
        if (ability == ChronomlinAbility.EVOLVE && nextType(actor.type()) == null)
            return AbilityRejection.MAX_EVOLUTION;
        if (!borrowed && ability == ChronomlinAbility.GRAPPLE && actor.cooldown() > 0)
            return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == ChronomlinAbility.EVOLVE && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Abilities ability,
                                     ChronomlinRules rules, boolean borrowed) {
        MoveResult ready = availability(actor, ability, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        var pieces = new HashMap<Long, PieceState>();
        pieces.put(actor.id(), actor);
        if (target != null) pieces.put(target.id(), target);
        var context = new HeroActionContext(field, pieces, 0);
        return new ChronomlinHero(rules).geometryAvailable(context, actor, target, ability,
                square -> field.getFigureByPosition(square) instanceof Empty)
                .applyCost(actor, AbilityUse.of(HeroType.CHRONOMLIN, borrowed));
    }
}
