package game.moves;

public enum StatusRejection implements Rejection {
    POTTED("Горшок — неуязвимое препятствие; его можно только передвинуть способностью"),
    BOXED("Фигура защищена коробкой"),
    ILLUSION_CANNOT_CAPTURE("Иллюзия не может брать фигуры"),
    STUNNED("Фигура оглушена и не может действовать"),
    SILENCED("Внутри тучи нельзя применять скиллы и ульты"),
    TARGET_INVISIBLE("Вражеская фигура невидима"),
    BLINDED_CANNOT_CAPTURE("Ослеплённая фигура не может брать фигуры"),
    KING_IMMUNE("Король невосприимчив к способностям");

    private final String message;

    StatusRejection(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
