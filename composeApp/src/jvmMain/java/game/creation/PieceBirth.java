package game.creation;

import game.heroes.HeroType;
import game.heroes.RevivalPlan;
import game.state.SquareColor;
import java.util.Objects;
import model.Position;

/** Каждая жизнь имеет новый ID; прежняя смерть и её награды остаются неизменными. */
public record PieceBirth(long id, long caster, Long corpse, long turn, HeroType hero, CreatedBody body,
                         SquareColor startingColor, int cooldown, Position square, String ability,
                         RevivalPlan.Evolution evolution) {
    public PieceBirth {
        if (id < 1 || caster < 1 || id <= caster || corpse != null && (corpse < 1 || id <= corpse)
                || turn < 1 || cooldown < 0 || square == null || square.getRow() < 0 || square.getRow() > 7
                || square.getColumn() < 0 || square.getColumn() > 7 || ability == null || ability.isBlank())
            throw new IllegalArgumentException("Invalid piece birth");
        Objects.requireNonNull(hero); Objects.requireNonNull(body);
    }
}
