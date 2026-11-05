package game.heroes.lumeshard;

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

/** Совместимый публичный фасад; расчёт способностей принадлежит LumeshardHero. */
public final class LumeshardAbilities {
    private LumeshardAbilities() {
    }

    public static List<Abilities> abilities() {
        return LumeshardAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, LumeshardRules rules) {
        return availability(actor, ability, rules, false);
    }

    public static MoveResult availability(PieceState actor, Abilities ability, LumeshardRules rules, boolean borrowed) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.LUMESHARD) || !abilities().contains(ability)) return AbilityRejection.NO_ABILITY;
        if (!borrowed && ability == LumeshardAbility.LASER && actor.cooldown() > 0) return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == LumeshardAbility.TELEPORT && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square, Abilities ability, LumeshardRules rules) {
        return preview(field, actor, target, square, ability, rules, false);
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square, Abilities ability, LumeshardRules rules, boolean borrowed) {
        MoveResult ready = availability(actor, ability, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return new LumeshardHero(rules).legacyPreview(actor, target, square, ability, borrowed);
    }

    public static PieceState afterCapture(PieceState actor, LumeshardRules rules) {
        return new LumeshardHero(rules).legacyAfterCapture(actor);
    }
}
