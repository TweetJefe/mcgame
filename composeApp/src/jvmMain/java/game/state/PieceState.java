package game.state;

import game.heroes.HeroType;

import model.Figure;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.Empty;
import model.figures.Rook;
import model.figures.Bishop;
import model.figures.Pawn;
import model.figures.Queen;
import model.figures.Knight;
import java.util.List;
import java.util.Objects;

/** Immutable per-piece state. Its id survives movement, swallowing, release and promotion. */
public record PieceState(long id, Class<? extends Figure> type, FigureColor color, Position position,
                         Status status, HeroType hero, int cooldown, int ultimateCharge,
                         List<ItemSlot> inventory, Long storedPieceId, int blindTurns, SquareColor startingColor) {
    public enum Status { BOARD, SWALLOWED, CAPTURED, HIDDEN_BOG, REPLACED }
    public PieceState {
        if (id <= 0 || cooldown < 0 || ultimateCharge < 0 || blindTurns < 0) throw new IllegalArgumentException("Invalid piece state");
        Objects.requireNonNull(type); Objects.requireNonNull(color); Objects.requireNonNull(status); Objects.requireNonNull(hero);
        if (!Field.supports(type) || type == Empty.class || (color != FigureColor.WHITE && color != FigureColor.BLACK))
            throw new IllegalArgumentException("A piece state must describe a supported nonempty piece");
        if (status == Status.HIDDEN_BOG && hero != HeroType.BOLOTNIK_KNIGHT && hero != HeroType.GINSENG_BISHOP) throw new IllegalArgumentException("Only Bolotnik can hide in a bog");
        if (hero == HeroType.BOLOTNIK_KNIGHT && type != Knight.class) throw new IllegalArgumentException("Bolotnik is a knight");
        if ((hero == HeroType.PUDGE_ROOK || hero == HeroType.IZBA_ROOK) && type != Rook.class) throw new IllegalArgumentException("Pudge is a rook");
        if ((hero == HeroType.LUMESHARD_BISHOP || hero == HeroType.GINSENG_BISHOP) && (type != Bishop.class || startingColor == null))
            throw new IllegalArgumentException("Lumeshard needs a bishop and its starting square color");
        if ((hero == HeroType.BOUNTY_CROW_PAWN || hero == HeroType.BABA_YAGA_PAWN) && type != Pawn.class && type != Queen.class && type != Rook.class && type != Bishop.class && type != Knight.class)
            throw new IllegalArgumentException("Crow must be a pawn or its promoted piece");
        if (hero != HeroType.LUMESHARD_BISHOP && hero != HeroType.GINSENG_BISHOP && startingColor != null) throw new IllegalArgumentException("Unexpected starting color");
        if ((status == Status.BOARD) != (position != null)) throw new IllegalArgumentException("Inconsistent piece placement");
        if (position != null && (position.getRow() < 0 || position.getRow() > 7 || position.getColumn() < 0 || position.getColumn() > 7))
            throw new IllegalArgumentException("Piece position outside board");
        if (storedPieceId != null && (storedPieceId <= 0 || storedPieceId == id || (hero != HeroType.PUDGE_ROOK && hero != HeroType.GINSENG_BISHOP)))
            throw new IllegalArgumentException("Invalid swallowed piece reference");
        inventory = List.copyOf(inventory);
        if (inventory.size() != 3) throw new IllegalArgumentException("Every piece has exactly three item slots");
    }
    /** Compatibility constructor for states without the Lumeshard extension. */
    public PieceState(long id, Class<? extends Figure> type, FigureColor color, Position position,
                      Status status, HeroType hero, int cooldown, int ultimateCharge, List<ItemSlot> inventory, Long storedPieceId) {
        this(id, type, color, position, status, hero, cooldown, ultimateCharge, inventory, storedPieceId, 0, null);
    }
    public static PieceState initial(long id, Figure figure, HeroType hero) {
        return new PieceState(id, figure.getClass(), figure.getColor(), figure.getPosition(), Status.BOARD,
                hero, 0, 0, List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), null, 0,
                (hero == HeroType.LUMESHARD_BISHOP || hero == HeroType.GINSENG_BISHOP) ? SquareColor.at(figure.getPosition()) : null);
    }
    public PieceState placed(Position square, Class<? extends Figure> newType) {
        return new PieceState(id, newType, color, square, Status.BOARD, hero, cooldown, ultimateCharge, inventory, storedPieceId, blindTurns, startingColor);
    }
    public PieceState hidden(Status newStatus) {
        return new PieceState(id, type, color, null, newStatus, hero, cooldown, ultimateCharge, inventory,
                (newStatus == Status.CAPTURED || newStatus == Status.REPLACED) ? null : storedPieceId, (newStatus == Status.CAPTURED || newStatus == Status.REPLACED) ? 0 : blindTurns, startingColor);
    }
    public PieceState withAbilityState(int nextCooldown, int nextCharge, Long nextStored) {
        return new PieceState(id, type, color, position, status, hero, nextCooldown, nextCharge, inventory, nextStored, blindTurns, startingColor);
    }
    public boolean isRemoved() { return status == Status.CAPTURED || status == Status.REPLACED; }
    public boolean isBlinded() { return blindTurns > 0; }
    public PieceState withBlindness(int turns) {
        return new PieceState(id, type, color, position, status, hero, cooldown, ultimateCharge, inventory, storedPieceId, turns, startingColor);
    }
}
