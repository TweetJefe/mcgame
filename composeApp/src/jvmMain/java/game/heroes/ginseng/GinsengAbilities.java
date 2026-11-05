package game.heroes.ginseng;

import game.heroes.Abilities;
import game.heroes.AbilityUse;
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
import model.figures.Empty;

/** Совместимый фасад; единственная реализация сдвига и получения копии находится в GinsengHero. */
public final class GinsengAbilities {
    private GinsengAbilities() {}

    public static List<Abilities> abilities() {
        return GinsengAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, GinsengRules rules, boolean borrowed) {
        // Сырой исторический API не проверял горшок и разрешал только настоящего Ginseng.
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != HeroType.GINSENG || !abilities().contains(ability)) return AbilityRejection.NO_ABILITY;
        if (!borrowed && ability == GinsengAbility.DISPLACE && actor.cooldown() > 0)
            return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == GinsengAbility.COPY_SPELL && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect displace(Field field, PieceState actor, PieceState target, Position destination,
                                      GinsengRules rules, boolean borrowed) {
        var hero = new GinsengHero(rules);
        var use = AbilityUse.of(HeroType.GINSENG, borrowed);
        MoveResult ready = availability(actor, GinsengAbility.DISPLACE, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return hero.displaceAvailable(actor, target, destination,
                square -> field.getFigureByPosition(square) instanceof Empty).applyCost(actor, use);
    }

    public static HeroEffect copy(PieceState actor, PieceState source, Abilities last, GinsengRules rules, boolean borrowed) {
        var hero = new GinsengHero(rules);
        var use = AbilityUse.of(HeroType.GINSENG, borrowed);
        MoveResult ready = availability(actor, GinsengAbility.COPY_SPELL, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return hero.copyAvailable(actor, source, last).applyCost(actor, use);
    }

    public static PieceState afterCapture(PieceState actor, GinsengRules rules) {
        // Старый отдельный callback начислял заряд без проверки личности и горшка.
        return actor.withAbilityState(actor.cooldown(), Math.min(rules.ultimateCaptures(), actor.ultimateCharge() + 1),
                actor.storedPieceId());
    }
}
