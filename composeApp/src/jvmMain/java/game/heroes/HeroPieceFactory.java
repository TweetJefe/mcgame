package game.heroes;

import game.state.ItemSlot;
import game.state.PieceState;
import java.util.List;
import java.util.Objects;
import model.Figure;

/** Единая инициализация нового героя с реестром и настройками конкретной партии. */
public final class HeroPieceFactory {
    private final HeroRegistry heroes;

    public HeroPieceFactory(HeroRegistry heroes) {
        this.heroes = Objects.requireNonNull(heroes, "heroes");
    }

    public PieceState initial(long id, Figure figure, HeroType hero) {
        PieceState base = new PieceState(id, figure.getClass(), figure.getColor(), figure.getPosition(),
                PieceState.Status.BOARD, hero, 0, 0,
                List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), null);
        return create(base, PieceCreationContext.initial(figure.getPosition()));
    }

    /** Вызывающий модуль определяет перенос инвентаря, ресурсов и воздействий в base. */
    public PieceState create(PieceState base, PieceCreationContext origin) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(origin, "origin");
        HeroDefinition<?> definition = heroes.get(base.hero()).definition();
        PieceState result = definition.initializeState(base, origin, heroes.rules());
        if (result.id() != base.id() || result.hero() != base.hero() || result.type() != base.type()
                || result.color() != base.color() || result.status() != base.status()
                || !Objects.equals(result.position(), base.position()))
            throw new IllegalArgumentException("Initialization changed piece identity or placement");
        heroes.validateState(result);
        definition.validateCreation(result, origin, heroes.rules());
        return result;
    }
}
