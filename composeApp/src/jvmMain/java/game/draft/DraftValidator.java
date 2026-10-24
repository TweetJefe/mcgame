package game.draft;

import game.heroes.HeroType;
import game.state.*;
import model.FigureColor;

import java.util.List;
import java.util.HashMap;

public final class DraftValidator {
    private DraftValidator() {
    }

    public static void validate(GameSnapshot game) {
        DraftSnapshot draft = game.draft();
        if (draft == null) return;
        if (game.mode() != GameMode.MOBA || !draft.isComplete())
            throw new IllegalArgumentException("A game requires a completed Moba draft");
        Draft.fromSnapshot(draft);
        for (FigureColor side : List.of(FigureColor.WHITE, FigureColor.BLACK)) {
            var expected = new HashMap<HeroType, Integer>();
            var actual = new HashMap<HeroType, Integer>();
            for (HeroPick pick : draft.picksFor(side)) expected.merge(pick.hero(), 1, Integer::sum);
            for (PieceState piece : game.pieces().values())
                if (piece.color() == side && piece.hero() != HeroType.NONE && piece.hero()!=HeroType.ILLUSION && !game.summons().records().containsKey(piece.id())) {
                    actual.merge(piece.hero(), 1, Integer::sum);
                    if (piece.hero()==HeroType.KIKIMORA || piece.hero() == HeroType.LUMESHARD || piece.hero() == HeroType.GINSENG || piece.hero() == HeroType.ARES) {
                        HeroPick origin = draft.picksFor(side).stream().filter(p -> p.hero() == piece.hero()).findFirst().orElseThrow(() -> new IllegalArgumentException("Unpicked Lumeshard"));
                        if (piece.startingColor() != SquareColor.at(origin.square()))
                            throw new IllegalArgumentException("Lumeshard starting color differs from its draft placement");
                    }
                }
            if (!expected.equals(actual))
                throw new IllegalArgumentException("Game heroes differ from initial draft picks");
        }
    }
}
