package game;

import model.Figure;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.Empty;
import model.figures.Rook;
import java.util.List;
import java.util.Objects;

/** Immutable per-piece state. Its id survives movement, swallowing, release and promotion. */
public record PieceState(long id, Class<? extends Figure> type, FigureColor color, Position position,
                         Status status, HeroType hero, int cooldown, int ultimateCharge,
                         List<ItemSlot> inventory, Long storedPieceId) {
    public enum Status { BOARD, SWALLOWED, CAPTURED }
    public PieceState {
        if (id <= 0 || cooldown < 0 || ultimateCharge < 0) throw new IllegalArgumentException("Invalid piece state");
        Objects.requireNonNull(type); Objects.requireNonNull(color); Objects.requireNonNull(status); Objects.requireNonNull(hero);
        if (!Field.supports(type) || type == Empty.class || (color != FigureColor.WHITE && color != FigureColor.BLACK))
            throw new IllegalArgumentException("A piece state must describe a supported nonempty piece");
        if (hero == HeroType.PUDGE_ROOK && type != Rook.class) throw new IllegalArgumentException("Pudge is a rook");
        if ((status == Status.BOARD) != (position != null)) throw new IllegalArgumentException("Inconsistent piece placement");
        if (position != null && (position.getRow() < 0 || position.getRow() > 7 || position.getColumn() < 0 || position.getColumn() > 7))
            throw new IllegalArgumentException("Piece position outside board");
        if (storedPieceId != null && (storedPieceId <= 0 || storedPieceId == id || hero != HeroType.PUDGE_ROOK))
            throw new IllegalArgumentException("Invalid swallowed piece reference");
        inventory = List.copyOf(inventory);
        if (inventory.size() != 3) throw new IllegalArgumentException("Every piece has exactly three item slots");
    }
    static PieceState initial(long id, Figure figure, HeroType hero) {
        return new PieceState(id, figure.getClass(), figure.getColor(), figure.getPosition(), Status.BOARD,
                hero, 0, 0, List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), null);
    }
    PieceState placed(Position square, Class<? extends Figure> newType) {
        return new PieceState(id, newType, color, square, Status.BOARD, hero, cooldown, ultimateCharge, inventory, storedPieceId);
    }
    PieceState hidden(Status newStatus) {
        return new PieceState(id, type, color, null, newStatus, hero, cooldown, ultimateCharge, inventory,
                newStatus == Status.CAPTURED ? null : storedPieceId);
    }
    PieceState withAbilityState(int nextCooldown, int nextCharge, Long nextStored) {
        return new PieceState(id, type, color, position, status, hero, nextCooldown, nextCharge, inventory, nextStored);
    }
}
