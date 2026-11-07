package game.mechanics;

import game.creation.PieceBirth;
import game.visibility.Concealment;
import model.Position;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.history.GameEvent;
import model.Figure;
import model.FigureColor;
import java.util.Map;
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
    default PieceBirth birth(GameSnapshot snapshot, PieceState piece) { return null; }
    default boolean dismissed(HeroActionContext context, PieceState piece) { return false; }
    default boolean dismissed(GameSnapshot snapshot, PieceState piece) { return false; }
    default Class<? extends Figure> revivalBody(HeroActionContext context, PieceState corpse) { return null; }
    default boolean reveals(HeroActionContext context, FigureColor viewer, Position square,
            Concealment kind) { return false; }
    default PieceRestrictions restrictions(HeroActionContext context, PieceState piece) { return PieceRestrictions.NONE; }
    default java.util.Set<Position> passageObstacles(HeroActionContext context) { return java.util.Set.of(); }
    default boolean allowsExtraMove(HeroActionContext context, PieceState actor) { return false; }
    /** null означает, что модуль не менял первоначальную принадлежность фигуры. */
    default HeroType initialHero(GameSnapshot snapshot, PieceState piece) { return null; }
    default List<PieceHeroChange> heroChanges(GameEvent event) { return List.of(); }
    default List<PieceBodyChange> bodyChanges(GameEvent event) { return List.of(); }
    /** Исходная карта неизменяема; модуль описывает только переходы собственных событий. */
    default List<PieceBodyChange> bodyChanges(GameEvent event, Map<Long, Class<? extends Figure>> before) {
        return bodyChanges(event);
    }
    default boolean skipsTurn(HeroActionContext context, FigureColor side) { return false; }
    default boolean isTurnSkipEvent(GameEvent event) { return false; }
    default boolean requiresPositionHistory(HeroActionContext context) { return false; }
    default void validate(GameSnapshot snapshot) {}
}
