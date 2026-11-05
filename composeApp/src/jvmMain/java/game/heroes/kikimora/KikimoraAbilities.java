package game.heroes.kikimora;

import game.heroes.Abilities;
import game.heroes.AbilityRequest;
import game.heroes.AbilityUse;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.PieceState;
import java.util.List;
import java.util.Map;
import model.Field;
import model.FigureColor;

/** Совместимый фасад расчётов KikimoraHero, используемый также восстановлением журнала. */
public final class KikimoraAbilities {
    private KikimoraAbilities() {}

    public static List<Abilities> abilities() {
        return KikimoraAbility.getAbilities();
    }

    public static MoveResult availability(PieceState actor, Abilities ability, KikimoraRules rules, boolean borrowed) {
        // Этот совместимый вход сохраняет прежний приоритет личности перед состоянием горшка.
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if (actor == null || actor.position() == null) return MovementRejection.EMPTY_SOURCE;
        if (actor.hero() != (borrowed ? HeroType.GINSENG : HeroType.KIKIMORA) || !abilities().contains(ability))
            return AbilityRejection.NO_ABILITY;
        if (actor.isPotted()) return StatusRejection.POTTED;
        if (!borrowed && ability == KikimoraAbility.YARN && actor.cooldown() > 0)
            return AbilityRejection.ABILITY_COOLDOWN;
        if (!borrowed && ability == KikimoraAbility.CLAY_POT && actor.ultimateCharge() < rules.ultimateCaptures())
            return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }

    public static List<PieceState> yarnTargets(Map<Long, PieceState> pieces, PieceState actor, PieceState first) {
        return KikimoraHero.yarnTargets(pieces, actor, first);
    }

    public static long stunDeadline(long turn, FigureColor caster, FigureColor owner, int turns) {
        return KikimoraHero.stunDeadline(turn, caster, owner, turns);
    }

    public static HeroEffect preview(Map<Long, PieceState> pieces, PieceState actor, PieceState target,
                                     Abilities ability, long turn, KikimoraRules rules, boolean borrowed) {
        MoveResult ready = availability(actor, ability, rules, borrowed);
        if (!ready.isSuccess()) return HeroEffect.rejected(ready);
        return new KikimoraHero(rules).previewAvailable(new HeroActionContext(new Field(8), pieces, turn), actor, target,
                new AbilityRequest(actor.position(), ability, target == null ? null : target.position(), null, borrowed))
                .applyCost(actor, AbilityUse.of(HeroType.KIKIMORA, borrowed));
    }
}
