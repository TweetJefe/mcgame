package game.core;

import game.economy.EconomyState;
import game.mechanics.BuiltInStates;
import game.mechanics.MechanicStates;
import game.state.CastlingRights;
import game.state.GameMode;
import game.summoning.SummonState;
import model.Field;

/** Стратегия ограничений сессии; поведение конкретных фигур и героев ей неизвестно. */
public enum GamePolicy {
    MATCH {
        @Override public Field initialField(int size) { return new Field(size); }
        @Override public CastlingRights initialRights() { return CastlingRights.initial(); }
        @Override public boolean limitsMoves() { return true; }
        @Override public boolean limitsAbilities() { return true; }
        @Override public boolean requiresContinuation() { return true; }
        @Override public boolean requiresMoveToEnd() { return true; }
        @Override public boolean evaluatesOutcome() { return true; }
        @Override public boolean allowsEditing() { return false; }
    },
    PRACTICE {
        @Override public Field initialField(int size) {
            if (size != 8) throw new IllegalArgumentException("Practice requires an 8x8 board");
            return Field.emptySandbox();
        }
        @Override public CastlingRights initialRights() { return CastlingRights.none(); }
        @Override public boolean limitsMoves() { return false; }
        @Override public boolean limitsAbilities() { return false; }
        @Override public boolean requiresContinuation() { return false; }
        @Override public boolean requiresMoveToEnd() { return false; }
        @Override public boolean evaluatesOutcome() { return false; }
        @Override public boolean allowsEditing() { return true; }
        @Override public MechanicStates initialMechanics(GameMode mode) {
            return super.initialMechanics(mode).with(BuiltInStates.SUMMONS, SummonState.initial(System.nanoTime()));
        }
    };

    /** Сбрасывается только счётчик обычных перемещений, иллюзии и их состояния сохраняются. */
    public MechanicStates prepareContextMechanics(MechanicStates states) {
        if (limitsMoves()) return states;
        var movement = states.get(BuiltInStates.CLOWN);
        return states.with(BuiltInStates.CLOWN, new game.heroes.clownganger.ClownState(
                movement.illusions(), 0, null, java.util.Set.of()));
    }

    public abstract Field initialField(int size);
    public abstract CastlingRights initialRights();
    public abstract boolean limitsMoves();
    public abstract boolean limitsAbilities();
    public abstract boolean requiresContinuation();
    public abstract boolean requiresMoveToEnd();
    public abstract boolean evaluatesOutcome();
    public abstract boolean allowsEditing();

    public MechanicStates initialMechanics(GameMode mode) {
        return mode == GameMode.MOBA
                ? MechanicStates.empty().with(BuiltInStates.ECONOMY, EconomyState.initial()) : MechanicStates.empty();
    }
}
