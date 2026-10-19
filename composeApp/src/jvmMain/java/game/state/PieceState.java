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
                         List<ItemSlot> inventory, Long storedPieceId, int blindTurns, SquareColor startingColor, long stunnedThroughTurn, long boxedThroughTurn, boolean potted) {
    public PieceState(long id, Class<? extends Figure> type, FigureColor color, Position position,
                         Status status, HeroType hero, int cooldown, int ultimateCharge,
                         List<ItemSlot> inventory, Long storedPieceId, int blindTurns, SquareColor startingColor, long stunnedThroughTurn, long boxedThroughTurn){this(id,type,color,position,status,hero,cooldown,ultimateCharge,inventory,storedPieceId,blindTurns,startingColor,stunnedThroughTurn,boxedThroughTurn,false);}
    public enum Status { BOARD, SWALLOWED, CAPTURED, HIDDEN_BOG, REPLACED, DISSOLVED, DESTROYED }
    public PieceState {
        if (id <= 0 || cooldown < 0 || ultimateCharge < 0 || blindTurns < 0 || stunnedThroughTurn < 0 || boxedThroughTurn < 0) throw new IllegalArgumentException("Invalid piece state");
        if(potted && (status!=Status.BOARD || type==model.figures.King.class))throw new IllegalArgumentException("Only a board nonking can be a pot");
        Objects.requireNonNull(type); Objects.requireNonNull(color); Objects.requireNonNull(status); Objects.requireNonNull(hero);
        if (!Field.supports(type) || type == Empty.class || (color != FigureColor.WHITE && color != FigureColor.BLACK))
            throw new IllegalArgumentException("A piece state must describe a supported nonempty piece");
        if (status == Status.HIDDEN_BOG && hero != HeroType.BOLOTNIK_KNIGHT && hero != HeroType.GINSENG_BISHOP) throw new IllegalArgumentException("Only Bolotnik can hide in a bog");
        if ((hero == HeroType.BOLOTNIK_KNIGHT || hero == HeroType.FAUN_KNIGHT || hero == HeroType.CLOWNGANGER_KNIGHT) && type != Knight.class) throw new IllegalArgumentException("Bolotnik is a knight");
        if ((hero == HeroType.ZEUS_ROOK || hero == HeroType.PUDGE_ROOK || hero == HeroType.IZBA_ROOK) && type != Rook.class) throw new IllegalArgumentException("Pudge is a rook");
        if ((hero == HeroType.KIKIMORA_BISHOP || hero == HeroType.LUMESHARD_BISHOP || hero == HeroType.ARES_BISHOP) && (type != Bishop.class || startingColor == null))
            throw new IllegalArgumentException("Lumeshard needs a bishop and its starting square color");
        if ((hero == HeroType.CHRONOMLIN_PAWN || hero == HeroType.MOLEMINER_PAWN || hero == HeroType.BOUNTY_CROW_PAWN || hero == HeroType.BABA_YAGA_PAWN) && type != Pawn.class && type != Queen.class && type != Rook.class && type != Bishop.class && type != Knight.class)
            throw new IllegalArgumentException("Crow must be a pawn or its promoted piece");
        if (hero != HeroType.KIKIMORA_BISHOP && hero != HeroType.LUMESHARD_BISHOP && hero != HeroType.GINSENG_BISHOP && hero != HeroType.ARES_BISHOP && startingColor != null) throw new IllegalArgumentException("Unexpected starting color");
        if(hero==HeroType.GINSENG_BISHOP && ((type!=Bishop.class && type!=Knight.class && type!=Rook.class) || startingColor==null))throw new IllegalArgumentException("Invalid Ginseng form");
        if(hero==HeroType.ILLUSION && type!=Knight.class && type!=Bishop.class && type!=Rook.class)throw new IllegalArgumentException("Invalid illusion body");
        if(status==Status.DISSOLVED && hero!=HeroType.ILLUSION)throw new IllegalArgumentException("Only an illusion can dissolve");
        if ((status == Status.BOARD) != (position != null)) throw new IllegalArgumentException("Inconsistent piece placement");
        if (position != null && (position.getRow() < 0 || position.getRow() > 7 || position.getColumn() < 0 || position.getColumn() > 7))
            throw new IllegalArgumentException("Piece position outside board");
        if (storedPieceId != null && (storedPieceId <= 0 || storedPieceId == id || (hero != HeroType.PUDGE_ROOK && hero != HeroType.GINSENG_BISHOP)))
            throw new IllegalArgumentException("Invalid swallowed piece reference");
        inventory = List.copyOf(inventory);
        if (inventory.size() != 3) throw new IllegalArgumentException("Every piece has exactly three item slots");
    }
    public boolean isPotted(){return potted;}
    public PieceState asPot(){return new PieceState(id,type,color,position,status,hero,cooldown,ultimateCharge,inventory,storedPieceId,blindTurns,startingColor,stunnedThroughTurn,boxedThroughTurn,true);}
    public boolean isBoxed(){return boxedThroughTurn>0;}
    public PieceState withBox(long until){return new PieceState(id,type,color,position,status,hero,cooldown,ultimateCharge,inventory,storedPieceId,blindTurns,startingColor,stunnedThroughTurn,until,potted);}
    public PieceState(long id,Class<? extends Figure> type,FigureColor color,Position position,Status status,HeroType hero,int cooldown,int ultimateCharge,List<ItemSlot> inventory,Long storedPieceId,int blindTurns,SquareColor startingColor,long stunnedThroughTurn) {
        this(id,type,color,position,status,hero,cooldown,ultimateCharge,inventory,storedPieceId,blindTurns,startingColor,stunnedThroughTurn,0);
    }
    public PieceState(long id,Class<? extends Figure> type,FigureColor color,Position position,Status status,HeroType hero,int cooldown,int ultimateCharge,List<ItemSlot> inventory,Long storedPieceId,int blindTurns,SquareColor startingColor) {
        this(id,type,color,position,status,hero,cooldown,ultimateCharge,inventory,storedPieceId,blindTurns,startingColor,0);
    }
    public boolean isStunned(){return stunnedThroughTurn>0;}
    public PieceState withStun(long until){return new PieceState(id,type,color,position,status,hero,cooldown,ultimateCharge,inventory,storedPieceId,blindTurns,startingColor,until,boxedThroughTurn,potted);}
    /** Compatibility constructor for states without the Lumeshard extension. */
    public PieceState(long id, Class<? extends Figure> type, FigureColor color, Position position,
                      Status status, HeroType hero, int cooldown, int ultimateCharge, List<ItemSlot> inventory, Long storedPieceId) {
        this(id, type, color, position, status, hero, cooldown, ultimateCharge, inventory, storedPieceId, 0, null);
    }
    public static PieceState initial(long id, Figure figure, HeroType hero) {
        return new PieceState(id, figure.getClass(), figure.getColor(), figure.getPosition(), Status.BOARD,
                hero, 0, 0, List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), null, 0,
                (hero == HeroType.KIKIMORA_BISHOP || hero == HeroType.LUMESHARD_BISHOP || hero == HeroType.GINSENG_BISHOP || hero == HeroType.ARES_BISHOP) ? SquareColor.at(figure.getPosition()) : null);
    }
    public PieceState placed(Position square, Class<? extends Figure> newType) {
        return new PieceState(id, newType, color, square, Status.BOARD, hero, cooldown, ultimateCharge, inventory, storedPieceId, blindTurns, startingColor,stunnedThroughTurn,boxedThroughTurn,potted);
    }
    public PieceState hidden(Status newStatus) {
        return new PieceState(id, type, color, null, newStatus, hero, cooldown, ultimateCharge, inventory,
                (newStatus == Status.CAPTURED || newStatus == Status.REPLACED || newStatus == Status.DISSOLVED || newStatus == Status.DESTROYED) ? null : storedPieceId, (newStatus == Status.CAPTURED || newStatus == Status.REPLACED || newStatus == Status.DISSOLVED || newStatus == Status.DESTROYED) ? 0 : blindTurns, startingColor,(newStatus == Status.CAPTURED || newStatus == Status.REPLACED || newStatus == Status.DISSOLVED || newStatus == Status.DESTROYED)?0:stunnedThroughTurn,0,potted);
    }
    public PieceState withAbilityState(int nextCooldown, int nextCharge, Long nextStored) {
        return new PieceState(id, type, color, position, status, hero, nextCooldown, nextCharge, inventory, nextStored, blindTurns, startingColor,stunnedThroughTurn,boxedThroughTurn,potted);
    }
    public boolean isRemoved() { return status == Status.CAPTURED || status == Status.REPLACED || status == Status.DISSOLVED || status == Status.DESTROYED; }
    public boolean isBlinded() { return blindTurns > 0; }
    public PieceState withBlindness(int turns) {
        return new PieceState(id, type, color, position, status, hero, cooldown, ultimateCharge, inventory, storedPieceId, turns, startingColor,stunnedThroughTurn,boxedThroughTurn,potted);
    }
}
