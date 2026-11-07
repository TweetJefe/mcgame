package game.heroes;

import game.state.SquareColor;
import java.util.Objects;
import model.Figure;

/** Герой задаёт форму новой жизни и собственный отложенный жизненный цикл. */
public record RevivalPlan(Class<? extends Figure> body, SquareColor startingColor, int cooldown, Evolution evolution) {
    public record Evolution(HeroType hero, Class<? extends Figure> body, int ownTurns) {
        public Evolution { Objects.requireNonNull(hero); Objects.requireNonNull(body); if (ownTurns < 1) throw new IllegalArgumentException("Invalid life cycle"); }
    }
    public RevivalPlan { Objects.requireNonNull(body); if (cooldown < 0) throw new IllegalArgumentException("Negative revival cooldown"); }
}
