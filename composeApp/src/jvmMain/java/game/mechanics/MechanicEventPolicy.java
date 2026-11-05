package game.mechanics;



/** Структура внешних ссылок события; смысл типизированных данных проверяет модуль. */
public record MechanicEventPolicy(boolean activeTurn, boolean actorRequired,
                                  boolean targetRequired, boolean squaresRequired) {
    public static MechanicEventPolicy action() { return new MechanicEventPolicy(true, true, false, true); }
}
