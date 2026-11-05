package game.heroes.ginseng;

import game.heroes.Abilities;
import game.heroes.AbilityRequest;
import game.heroes.AbilityUse;
import game.heroes.Hero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroRegistry;
import game.heroes.HeroRules;
import game.moves.MoveResult;
import game.state.PieceState;
import model.Field;
import model.Position;

import java.util.HashMap;

/** Совместимый фасад; копия использует ту же стратегию и настоящего исполнителя. */
public final class BorrowedEffects {
    private BorrowedEffects() {}

    public static MoveResult availability(PieceState actor, Abilities ability, HeroRules rules) {
        Hero owner = HeroRegistry.fromRules(rules).ownerOf(ability);
        return owner.availability(actor, ability, AbilityUse.of(owner.type(), true));
    }

    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square,
                                     Abilities ability, HeroRules rules) {
        return preview(new HeroActionContext(field, pieces(actor, target), 0), actor, target, square, ability, rules);
    }

    public static HeroEffect preview(HeroActionContext context, PieceState actor, PieceState target,
                                     Position square, Abilities ability, HeroRules rules) {
        return preview(context, actor, target,
                new AbilityRequest(actor.position(), ability, square, null, true), rules);
    }

    public static HeroEffect preview(HeroActionContext context, PieceState actor, PieceState target,
                                     AbilityRequest request, HeroRules rules) {
        var states = new HashMap<>(context.pieces());
        states.putAll(pieces(actor, target));
        var detached = new HeroActionContext(context.field(), states, context.currentTurn(), context.mechanics());
        return HeroRegistry.fromRules(rules).ownerOf(request.ability()).preview(detached,
                new AbilityRequest(actor.position(), request.ability(), request.target(), request.destination(), true));
    }

    private static HashMap<Long, PieceState> pieces(PieceState actor, PieceState target) {
        var pieces = new HashMap<Long, PieceState>();
        pieces.put(actor.id(), actor);
        if (target != null) pieces.put(target.id(), target);
        return pieces;
    }
}
