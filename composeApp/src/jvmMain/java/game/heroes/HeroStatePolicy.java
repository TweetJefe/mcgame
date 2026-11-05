package game.heroes;

import game.state.PieceState;
import model.Position;

/** Неизменяемые правила собственного состояния героя, отдельно от его способностей. */
public interface HeroStatePolicy<R> {
    default PieceState initialize(PieceState base, PieceCreationContext origin, R rules) {
        return base;
    }

    void validate(PieceState piece, R rules);

    /** Проверяется, когда исходная клетка известна: при создании и восстановлении драфта. */
    default void validateOrigin(PieceState piece, Position origin, R rules) {}

    /** Совместимость прежних конструкторов описания; специальные поля требуют явной политики. */
    static <R> HeroStatePolicy<R> basic() {
        return (piece, rules) -> HeroStateConstraints.basic().validate(piece);
    }
}
