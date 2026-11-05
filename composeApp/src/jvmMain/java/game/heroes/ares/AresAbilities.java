package game.heroes.ares;

import game.heroes.Abilities;
import game.heroes.AbilityUse;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import game.terrain.TerrainState;
import java.util.List;
import java.util.Map;
import model.Field;
import model.Position;

/** Совместимый фасад; полёт копья и собственная месть реализованы только в AresHero. */
public final class AresAbilities {
    private AresAbilities() {}

    public static List<Abilities> abilities() {
        return AresAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, AresRules rules) {
        return availability(actor, ability, rules, false);
    }

    public static MoveResult availability(PieceState actor, Abilities ability, AresRules rules, boolean borrowed) {
        // Отдельная утилита сохраняет исторические проверки без общего запрета горшка.
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.status() != PieceState.Status.BOARD) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.ARES) || ability != AresAbility.SPEAR)
            return AbilityRejection.NO_ABILITY;
        return !borrowed && actor.cooldown() > 0 ? AbilityRejection.ABILITY_COOLDOWN : Success.SUCCESS;
    }

    public static List<Position> directions(Position from) {
        return AresHero.directions(from);
    }

    /** Старый отдельный расчёт копья не проверял готовность; основной вход — Hero.preview. */
    public static HeroEffect spear(Field field, Map<Long, PieceState> pieces, PieceState actor, Position direction,
                                   long turn, AresRules rules, TerrainState terrain, boolean borrowed) {
        return new AresHero(rules).spearAvailable(pieces, actor, direction, turn, terrain)
                .applyCost(actor, AbilityUse.of(HeroType.ARES, borrowed));
    }

    public static PieceState afterCapture(PieceState actor, AresRules rules) {
        // Старый callback обнулял слот проглатывания и не проверял горшок или личность.
        return actor.withAbilityState(actor.cooldown(), Math.min(rules.revengeCaptures(), actor.ultimateCharge() + 1), null);
    }

    public static boolean armed(PieceState actor, AresRules rules) {
        return AresHero.armed(actor, rules);
    }
}
