package game.heroes;

import game.state.PieceState;
import java.util.HashMap;

/** Совместимые публичные входы; все стратегии создаются зарегистрированными описаниями. */
public final class LegacyHeroes {
    private LegacyHeroes() {}

    public static Hero create(HeroType type, HeroRules configuration) {
        return HeroRegistry.fromRules(configuration).get(type);
    }

    /** Совместимый вход; игровой путь вызывает стратегию напрямую через реестр партии. */
    public static HeroEffect previewBorrowed(Hero source, HeroActionContext context,
            PieceState actor, PieceState target, AbilityRequest request, HeroRules rules) {
        return previewBorrowedSource(source, context, actor, target, request);
    }

    public static HeroEffect previewBorrowedSource(Hero source, HeroActionContext context,
            PieceState actor, PieceState target, AbilityRequest request) {
        var pieces = new HashMap<>(context.pieces());
        pieces.put(actor.id(), actor);
        if (target != null) pieces.put(target.id(), target);
        return source.preview(new HeroActionContext(context.field(), pieces, context.currentTurn(),
                context.mechanics()), request);
    }
}
