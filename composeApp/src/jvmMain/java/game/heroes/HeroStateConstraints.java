package game.heroes;

import game.state.PieceState;
import game.state.SquareColor;
import java.util.Set;
import model.Figure;
import model.Position;

/** Переиспользуемые ограничения полей; наборы героев здесь не перечисляются. */
public record HeroStateConstraints(Set<Class<? extends Figure>> bodies, boolean startingColor,
                                   boolean bog, boolean storage, boolean dissolved) {
    public HeroStateConstraints {
        bodies = Set.copyOf(bodies);
    }

    public static HeroStateConstraints basic() {
        return new HeroStateConstraints(Set.of(), false, false, false, false);
    }

    public static HeroStateConstraints bodies(Set<Class<? extends Figure>> bodies) {
        return new HeroStateConstraints(bodies, false, false, false, false);
    }

    public HeroStateConstraints withStartingColor() {
        return new HeroStateConstraints(bodies, true, bog, storage, dissolved);
    }

    public HeroStateConstraints withBog() {
        return new HeroStateConstraints(bodies, startingColor, true, storage, dissolved);
    }

    public HeroStateConstraints withStorage() {
        return new HeroStateConstraints(bodies, startingColor, bog, true, dissolved);
    }

    public HeroStateConstraints withDissolved() {
        return new HeroStateConstraints(bodies, startingColor, bog, storage, true);
    }

    public PieceState initialize(PieceState base, PieceCreationContext origin) {
        require(!startingColor || origin.square() != null, "Missing original square");
        return base.withStartingColor(startingColor ? SquareColor.at(origin.square()) : null);
    }

    public void validate(PieceState piece) {
        require(bodies.isEmpty() || bodies.contains(piece.type()), "Invalid runtime body for " + piece.hero());
        require(startingColor == (piece.startingColor() != null), "Invalid starting color for " + piece.hero());
        require(bog || piece.status() != PieceState.Status.HIDDEN_BOG, "Hero cannot hide in a bog");
        require(storage || piece.storedPieceId() == null, "Hero cannot store a swallowed piece");
        require(dissolved || piece.status() != PieceState.Status.DISSOLVED, "Hero cannot dissolve");
    }

    public void validateOrigin(PieceState piece, Position origin) {
        require(!startingColor || origin != null, "Missing original square");
        require(!startingColor || piece.startingColor() == SquareColor.at(origin),
                "Starting color differs from original placement");
    }

    public static void validateResources(PieceState piece, int maxCooldown, int maxCharge) {
        require(piece.cooldown() <= maxCooldown && piece.ultimateCharge() <= maxCharge,
                "Resources exceed configured limits for " + piece.hero());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
