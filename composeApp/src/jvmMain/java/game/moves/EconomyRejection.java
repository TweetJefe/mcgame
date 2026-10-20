package game.moves;

public enum EconomyRejection implements Rejection {
    ECONOMY_DISABLED("В этой партии экономика отключена"),
    NO_GOLD_TO_STEAL("У соперника нет золота для кражи");

    private final String message;

    EconomyRejection(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
