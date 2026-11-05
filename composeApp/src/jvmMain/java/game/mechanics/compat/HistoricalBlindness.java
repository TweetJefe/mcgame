package game.mechanics.compat;

import game.heroes.HeroDefinitions;
import game.state.GameMode;
import game.state.GameSnapshot;
import model.figures.King;

/** Проверка наложенной слепоты в прежней схеме механик. */
final class HistoricalBlindness {
    private HistoricalBlindness() {}
    static void validate(GameSnapshot s) {
        for (var piece : s.pieces().values()) {
            if (s.heroRules().definitions().stream().noneMatch(d -> d == HeroDefinitions.get(piece.hero()))) continue;
            require(piece.blindTurns() <= s.lumeshardRules().blindnessTurns(), "Blindness exceeds duration");
            require(!piece.isBlinded() || (s.mode() == GameMode.MOBA && piece.type() != King.class && !piece.isRemoved()), "Invalid blind piece");
        }
    }
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
