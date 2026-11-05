package game.heroes.faun;

import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.List;
import model.Position;

/** Совместимый публичный фасад; расчёт способностей принадлежит FaunHero. */
public final class FaunAbilities {
    private FaunAbilities() {
    }

    public static List<Abilities> abilities() {
        return FaunAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, FaunRules rules) {
        return availability(actor, ability, rules, false);
    }

    public static MoveResult availability(PieceState actor, Abilities ability, FaunRules rules, boolean borrowed) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.FAUN) || !abilities().contains(ability)) return AbilityRejection.NO_ABILITY;
        if (!borrowed && ability == FaunAbility.CLOUD && actor.cooldown() > 0) return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == FaunAbility.INVISIBILITY && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, Position center, Abilities ability, FaunRules rules) {
        return preview(actor, center, ability, rules, false);
    }

    public static HeroEffect preview(PieceState actor, Position center, Abilities ability, FaunRules rules, boolean borrowed) {
        MoveResult ready = availability(actor, ability, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return new FaunHero(rules).legacyPreview(actor, center, ability, borrowed);
    }

    public static PieceState afterCapture(PieceState actor, FaunRules rules) {
        return new FaunHero(rules).legacyAfterCapture(actor);
    }
}
