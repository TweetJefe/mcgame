package game.mechanics;

import game.heroes.HeroType;
import java.util.Objects;

public record PieceHeroChange(long pieceId, HeroType before, HeroType after) {
    public PieceHeroChange {
        if (pieceId < 1) throw new IllegalArgumentException("Invalid hero identity");
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
    }
}
