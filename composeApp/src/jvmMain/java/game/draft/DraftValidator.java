package game.draft;

import game.heroes.HeroRegistry;
import game.heroes.HeroType;
import game.state.GameMode;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.HashMap;
import java.util.List;
import model.FigureColor;

public final class DraftValidator {
    private DraftValidator() {
    }

    public static void validate(GameSnapshot game) {
        validate(game, HeroRegistry.fromRules(game.heroRules()));
    }

    public static void validate(GameSnapshot game, HeroRegistry heroes) {
        DraftSnapshot draft = game.draft();
        if (draft == null) return;
        if (game.mode() != GameMode.MOBA || !draft.isComplete())
            throw new IllegalArgumentException("A game requires a completed Moba draft");
        Draft.fromSnapshot(draft, game.heroRules());
        for (FigureColor side : List.of(FigureColor.WHITE, FigureColor.BLACK)) {
            var expected = new HashMap<HeroType, Integer>();
            var actual = new HashMap<HeroType, Integer>();
            for (HeroPick pick : draft.picksFor(side)) expected.merge(pick.hero(), 1, Integer::sum);
            for (PieceState piece : game.pieces().values())
                if (piece.color() == side && piece.hero() != HeroType.NONE
                        && !game.clown().illusions().containsKey(piece.id())
                        && !game.summons().records().containsKey(piece.id())) {
                    HeroType initialHero = heroes.mechanics().initialHero(game, piece);
                    if (initialHero == HeroType.NONE) continue;
                    actual.merge(initialHero, 1, Integer::sum);
                    HeroPick origin = draft.picksFor(side).stream()
                            .filter(p -> p.hero() == initialHero).findFirst().orElse(null);
                    if (origin != null) heroes.validateOrigin(piece, origin.square(), initialHero);
                }
            if (!expected.equals(actual))
                throw new IllegalArgumentException("Game heroes differ from initial draft picks");
        }
    }
}
