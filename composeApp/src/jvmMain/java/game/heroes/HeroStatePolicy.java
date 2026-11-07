package game.heroes;

import game.state.PieceState;
import model.Position;

/** Неизменяемые правила собственного состояния героя, отдельно от его способностей. */
public interface HeroStatePolicy<R> {
    default PieceState initialize(PieceState base, PieceCreationContext origin, R rules) {
        return base;
    }

    default RevivalPlan revival(PieceState corpse, HeroActionContext context, HeroRules configuration, R rules) {
        var body = context.registry().revivalBody(context, corpse);
        return new RevivalPlan(body == null ? corpse.type() : body, corpse.startingColor(), 0, null);
    }

    default void validateCreation(PieceState piece, PieceCreationContext origin, R rules) {
        if (!origin.revival()) validateOrigin(piece, origin.square(), rules);
        else if (piece.startingColor() != origin.source().startingColor())
            throw new IllegalArgumentException("Revival lost original square color");
    }

    void validate(PieceState piece, R rules);

    /** Проверяется, когда исходная клетка известна: при создании и восстановлении драфта. */
    default void validateOrigin(PieceState piece, Position origin, R rules) {}

    /** Совместимость прежних конструкторов описания; специальные поля требуют явной политики. */
    static <R> HeroStatePolicy<R> basic() {
        return (piece, rules) -> HeroStateConstraints.basic().validate(piece);
    }
}
