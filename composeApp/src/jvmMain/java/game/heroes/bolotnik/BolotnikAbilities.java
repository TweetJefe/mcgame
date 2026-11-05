package game.heroes.bolotnik;

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

/** Совместимый публичный фасад; расчёт способностей принадлежит BolotnikHero. */
public final class BolotnikAbilities {
    private BolotnikAbilities() {
    }

    public static List<Abilities> abilities() {
        return BolotnikAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, BolotnikRules rules) {
        return availability(actor, ability, rules, false);
    }

    public static MoveResult availability(PieceState actor, Abilities ability, BolotnikRules rules, boolean borrowed) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.BOLOTNIK) || !abilities().contains(ability)) return AbilityRejection.NO_ABILITY;
        if (!borrowed && ability == BolotnikAbility.MUD && actor.cooldown() > 0) return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == BolotnikAbility.AMBUSH && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, PieceState target, Position square, Abilities ability, BolotnikRules rules) {
        return preview(actor, target, square, ability, rules, false);
    }

    public static HeroEffect preview(PieceState actor, PieceState target, Position square, Abilities ability, BolotnikRules rules, boolean borrowed) {
        MoveResult ready = availability(actor, ability, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return new BolotnikHero(rules).legacyPreview(actor, target, square, ability, borrowed);
    }

    public static PieceState afterCapture(PieceState actor, BolotnikRules rules) {
        return new BolotnikHero(rules).legacyAfterCapture(actor);
    }
}
