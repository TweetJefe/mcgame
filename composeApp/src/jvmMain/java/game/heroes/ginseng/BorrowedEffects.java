package game.heroes.ginseng;

import game.heroes.chronomlin.ChronomlinAbility;
import game.heroes.kikimora.KikimoraAbility;
import game.heroes.*;
import game.heroes.pudge.*;
import game.heroes.lumeshard.*;
import game.heroes.bountycrow.*;
import game.heroes.bolotnik.*;
import game.heroes.izba.*;
import game.moves.MoveResult;
import game.state.*;
import model.*;
import model.figures.*;

import java.util.*;

/**
 * Reuses each original spell's geometry through a detached caster view, then restores Ginseng's identity and resources.
 */
public final class BorrowedEffects {
    private BorrowedEffects() {
    }

    private static PieceState caster(PieceState actual, Abilities ability) {
        HeroType hero = HeroRegistry.standard().ownerOf(ability).type();
        Class<? extends Figure> type = switch (hero) {
            case ZEUS,PUDGE, IZBA -> Rook.class;
            case LUMESHARD, ARES -> Bishop.class;
            case MOLEMINER,BOUNTY_CROW -> Pawn.class;
            default -> Knight.class;
        };
        return new PieceState(actual.id(), type, actual.color(), actual.position(), actual.status(), hero, 0, 0, actual.inventory(),
                hero == HeroType.PUDGE ? actual.storedPieceId() : null, actual.blindTurns(), (hero == HeroType.LUMESHARD || hero == HeroType.ARES) ? actual.startingColor() : null);
    }

    private static HeroRules freeRules(HeroRules rules) {
        return new HeroRules(new PudgeRules(0, 0, rules.pudge().hookDiagonals(), rules.pudge().swallowDiagonals()),
                new LumeshardRules(0,
                        rules.lumeshard().blindnessTurns(),
                        0),
                new BountyCrowRules(0,
                        rules.crow().bountyTurns(),
                        0),
                rules.gold(),
                new BolotnikRules(0,
                        rules.bolotnik().mudTurns(),
                        0),
                new IzbaRules(0,
                        rules.izba().lifetimeTurns()),
                rules.ginseng(),
                new game.heroes.faun.FaunRules(0, rules.faun().cloudTurns(), rules.faun().invisibilityTurns(), 0), new game.heroes.ares.AresRules(0, rules.ares().revengeCaptures()),new game.heroes.clownganger.ClowngangerRules(0,rules.clownganger().boxTurns(),rules.clownganger().illusionTurns(),0),new game.heroes.moleminer.MoleminerRules(0,0),new game.heroes.zeus.ZeusRules(0,rules.zeus().chargeTurns(),0),new game.heroes.chronomlin.ChronomlinRules(0,0));
    }

    public static MoveResult availability(PieceState actual, Abilities ability, HeroRules rules) {
        if(ability==KikimoraAbility.YARN || ability==KikimoraAbility.CLAY_POT)return game.heroes.kikimora.KikimoraAbilities.availability(actual,ability,rules.kikimora(),true);
        if(ability==ChronomlinAbility.GRAPPLE || ability==ChronomlinAbility.EVOLVE)return game.heroes.chronomlin.ChronomlinAbilities.availability(actual,ability,rules.chronomlin(),true);
        if (ability == GinsengAbility.DISPLACE || ability == GinsengAbility.COPY_SPELL)
            return GinsengAbilities.availability(actual, ability, rules.ginseng(), true);
        return HeroAbilities.availability(caster(actual, ability), ability, freeRules(rules));
    }

    public static HeroEffect preview(Field field, PieceState actual, PieceState target, Position square, Abilities ability, HeroRules rules) {
        var pieces = new HashMap<Long, PieceState>();
        pieces.put(actual.id(), actual);
        if (target != null) pieces.put(target.id(), target);
        return preview(new HeroActionContext(field, pieces, 0), actual, target, square, ability, rules);
    }

    public static HeroEffect preview(HeroActionContext context, PieceState actual, PieceState target,
                                      Position square, Abilities ability, HeroRules rules) {
        if(ability==ChronomlinAbility.GRAPPLE || ability==ChronomlinAbility.EVOLVE)return game.heroes.chronomlin.ChronomlinAbilities.preview(context.field(),actual,target,ability,rules.chronomlin(),true);
        PieceState viewActor = caster(actual, ability);
        HeroEffect effect = HeroRegistry.fromRules(freeRules(rules)).get(viewActor.hero())
                .preview(context, viewActor, target, square, ability);
        if (!effect.result().isSuccess()) return effect;
        var updates = new HashMap<>(effect.updates());
        PieceState view = updates.get(actual.id());

        if (view != null) updates.put(actual.id(), new PieceState(actual.id(),
                actual.type(),
                actual.color(),
                view.position(),
                view.status(),
                actual.hero(),
                actual.cooldown(),
                actual.ultimateCharge(),
                actual.inventory(),
                ability == PudgeAbility.SWALLOW ? view.storedPieceId() : actual.storedPieceId(),
                view.blindTurns(),
                actual.startingColor(), actual.stunnedThroughTurn(),actual.boxedThroughTurn()));

        return new HeroEffect(effect.result(), updates, effect.captures());
    }
}
