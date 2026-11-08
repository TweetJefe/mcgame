package game.draft;

import game.heroes.HeroDefinition;
import model.Figure;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;

/** Обязательный состав одного игрока; роль не зависит от порядка выбора. */
public enum DraftRole {
    PAWN(Pawn.class, 2), BISHOP(Bishop.class, 1), KNIGHT(Knight.class, 1), ROOK(Rook.class, 1);

    private final Class<? extends Figure> body;
    private final int required;

    DraftRole(Class<? extends Figure> body, int required) {
        this.body = body;
        this.required = required;
    }

    public int required() { return required; }

    public static DraftRole of(HeroDefinition<?> hero) {
        for (var role : values())
            if (hero.supportedBodies().equals(java.util.Set.of(role.body))) return role;
        throw new IllegalArgumentException("Draft hero must have one supported standard body");
    }
}
