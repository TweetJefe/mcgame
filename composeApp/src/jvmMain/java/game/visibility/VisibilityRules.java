package game.visibility;

import game.heroes.HeroActionContext;
import game.heroes.HeroRegistry;
import game.state.GameSnapshot;
import game.state.PieceState;
import model.FigureColor;
import model.Position;

/** Видимость не снимает скрывающий эффект: за пределами обнаружения он продолжает действовать. */
public final class VisibilityRules {
    private VisibilityRules() {}
    public static HeroActionContext context(GameSnapshot s) {
        return new HeroActionContext(s.field(), s.pieces(), s.turn().number(), s.mechanics(),
                HeroRegistry.fromRules(s.heroRules()).mechanics());
    }
    public static boolean reveals(HeroActionContext context, FigureColor viewer, Position square, Concealment kind) {
        return square != null && context.registry().reveals(context, viewer, square, kind);
    }
    public static boolean pieceVisible(GameSnapshot s, PieceState piece) {
        return !s.faun().hidden(piece, s.active()) || reveals(context(s), s.active(), piece.position(), Concealment.INVISIBILITY);
    }
    public static boolean mineVisible(GameSnapshot s, Position square) {
        var mine = s.mines().cells().get(square);
        return mine != null && (s.pieces().get(mine.ownerId()).color() == s.active()
                || reveals(context(s), s.active(), square, Concealment.MINE));
    }
}
