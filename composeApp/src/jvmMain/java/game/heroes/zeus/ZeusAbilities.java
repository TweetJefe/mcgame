package game.heroes.zeus;

import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.mechanics.MechanicSession;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import game.summoning.SummonState;
import java.util.List;
import java.util.Map;
import model.Field;

/** Совместимый фасад; расчёт молний принадлежит ZeusHero. */
public final class ZeusAbilities {
    private ZeusAbilities() {}

    public static List<Abilities> abilities() { return ZeusAbility.getAbilities(); }

    public static MoveResult availability(PieceState actor, Abilities ability, ZeusRules rules) {
        return availability(actor, ability, rules, false);
    }

    /** Сохраняет порядок проверок прежнего отдельного API. */
    public static MoveResult availability(PieceState actor, Abilities ability, ZeusRules rules, boolean borrowed) {
        if (actor == null || actor.position() == null) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.ZEUS) || !abilities().contains(ability))
            return AbilityRejection.NO_ABILITY;
        if (!borrowed && ability == ZeusAbility.CHAIN_LIGHTNING && actor.cooldown() > 0)
            return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == ZeusAbility.THUNDERSTORM && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static List<PieceState> charged(Map<Long, PieceState> pieces, LightningState lightning, PieceState actor) {
        return ZeusHero.charged(pieces, lightning, actor);
    }

    public record Result(HeroEffect effect, LightningState lightning, SummonState summons, List<Long> illusionHits) {}

    public static Result cast(Field factory, Map<Long, PieceState> pieces, PieceState actor, PieceState first,
            Abilities ability, LightningState lightning, SummonState summons, long turn, HeroRules rules,
            boolean borrowed) {
        return cast(factory, pieces, actor, first, ability, lightning, summons, turn, rules, borrowed, null);
    }

    public static Result cast(Field factory, Map<Long, PieceState> pieces, PieceState actor, PieceState first,
            Abilities ability, LightningState lightning, SummonState summons, long turn, HeroRules rules,
            boolean borrowed, MechanicSession session) {
        ZeusHero.CastResult result = new ZeusHero(rules.zeus()).detachedCast(factory, pieces, actor, first,
                ability, lightning, summons, turn, rules, borrowed, session);
        return new Result(result.effect(), result.lightning(), result.summons(), result.illusionHits());
    }
}
