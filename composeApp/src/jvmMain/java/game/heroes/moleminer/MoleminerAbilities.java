package game.heroes.moleminer;

import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.mechanics.MechanicSession;
import game.moves.AbilityRejection;
import game.moves.Capture;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.Success;
import game.state.PieceState;
import game.summoning.SummonState;
import java.util.List;
import java.util.Map;
import model.Field;
import model.Position;

/** Совместимый фасад над стратегией и модулем мин. */
public final class MoleminerAbilities {
    private MoleminerAbilities() {}

    public static List<Abilities> abilities() { return MoleminerAbility.getAbilities(); }

    public static MoveResult availability(PieceState actor, Abilities ability, MoleminerRules rules) {
        return availability(actor, ability, rules, false);
    }

    /** Сохраняет порядок проверок прежнего отдельного API. */
    public static MoveResult availability(PieceState actor, Abilities ability, MoleminerRules rules, boolean borrowed) {
        if (actor == null || actor.position() == null) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.MOLEMINER) || !abilities().contains(ability))
            return AbilityRejection.NO_ABILITY;
        if (!borrowed && ability == MoleminerAbility.PLANT_MINE && actor.cooldown() > 0)
            return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == MoleminerAbility.DETONATE && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static HeroEffect preview(PieceState actor, Position square, Abilities ability,
                                     MoleminerRules rules, boolean borrowed) {
        return new MoleminerHero(rules).detachedPreview(actor, square, ability, borrowed);
    }

    public static List<Position> corners(Position landing) { return MoleminerHero.corners(landing); }

    public record Explosion(Map<Long, PieceState> pieces, MineState mines, SummonState summons,
                            List<Capture> captures, List<Long> illusionHits) {}

    public static Explosion explode(Field factory, Map<Long, PieceState> prepared, long actorId, Position landing,
            MineState mines, SummonState summons, List<Capture> previous, long turn, HeroRules rules) {
        return explode(factory, prepared, actorId, landing, mines, summons, previous, turn, rules, null);
    }

    public static Explosion explode(Field factory, Map<Long, PieceState> prepared, long actorId, Position landing,
            MineState mines, SummonState summons, List<Capture> previous, long turn, HeroRules rules,
            MechanicSession session) {
        MineMechanic.Explosion result = MineMechanic.explode(factory, prepared, actorId, landing, mines,
                summons, previous, turn, rules, session);
        return new Explosion(result.pieces(), result.mines(), result.summons(), result.captures(), result.illusionHits());
    }
}
