package game.draft;

import game.heroes.HeroType;
import model.Position;
import java.util.Objects;

public record HeroPick(HeroType hero, Position square) {
    public HeroPick { Objects.requireNonNull(hero);Objects.requireNonNull(square); }
}
