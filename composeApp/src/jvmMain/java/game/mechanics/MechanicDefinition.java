package game.mechanics;

import game.heroes.HeroActionContext;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.List;

/** Единственная точка регистрации состояния, операций, реакций, событий и проверки. */
public interface MechanicDefinition<S> {
    MechanicKey<S> key();
    default List<MechanicDefinition<?>> dependencies() { return List.of(); }
    default List<MechanicHandler<?>> handlers() { return List.of(); }
    default List<MechanicStep> steps() { return List.of(); }
    default List<MechanicCaptureReaction> captureReactions() { return List.of(); }
    default List<MechanicEventType<?>> events() { return List.of(); }
    default MechanicStateCodec<S> codec() { return null; }
    default boolean legacyWireState() { return false; }
    default S restore(GameSnapshot snapshot) { return key().initial(); }
    default boolean allowsExtraMove(HeroActionContext context, PieceState actor) { return false; }
    default void validate(GameSnapshot snapshot) {}
}
