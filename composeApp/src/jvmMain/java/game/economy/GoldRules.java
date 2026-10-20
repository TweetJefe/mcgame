package game.economy;

import game.heroes.HeroType;
import game.state.PieceState;
import model.figures.*;

/**
 * Capture prices in whole gold. Wallet arithmetic uses integer half-gold units.
 */
public record GoldRules(int pawn, int heroPawn, int minor, int rook, int queen, int neutral) {
    public GoldRules {
        if (pawn < 0 || heroPawn < 0 || minor < 0 || rook < 0 || queen < 0 || neutral < 0
                || pawn > 1000 || heroPawn > 1000 || minor > 1000 || rook > 1000 || queen > 1000 || neutral > 1000)
            throw new IllegalArgumentException("Invalid capture prices");
    }

    public static GoldRules prototype() {
        return new GoldRules(1, 2, 3, 5, 6, 2);
    }

    public long rewardHalves(PieceState victim) {
        if (victim.type() == Pawn.class) return 2L * (victim.hero() == HeroType.NONE ? pawn : heroPawn);
        if (victim.type() == Knight.class || victim.type() == Bishop.class) return 2L * minor;
        if (victim.type() == Rook.class) return 2L * rook;
        if (victim.type() == Queen.class) return 2L * queen;
        throw new IllegalArgumentException("This piece cannot earn capture gold");
    }

    public static String format(long halves) {
        return (halves / 2) + (halves % 2 == 0 ? "" : ",5");
    }
}
