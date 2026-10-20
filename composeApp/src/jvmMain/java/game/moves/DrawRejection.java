package game.moves;

public enum DrawRejection implements Rejection {
    INVALID_DRAW_CLAIM("Условий для заявления ничьей нет"),
    NO_DRAW_OFFER("Нет предложения ничьей от соперника"),
    DRAW_OFFER_PENDING("Предложение ничьей уже ожидает ответа"),
    DRAW_AGREEMENT_TOO_EARLY("Для соглашения на ничью обе стороны должны сделать хотя бы один ход");

    private final String message;

    DrawRejection(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
