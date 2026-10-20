package game.moves;

public enum ItemRejection implements Rejection {
    INVALID_ITEM_SLOT("У фигуры только три слота предметов"),
    EMPTY_ITEM_SLOT("В этом слоте нет предмета"),
    ITEM_NOT_AVAILABLE("Этот предмет ещё не реализован");

    private final String message;

    ItemRejection(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
