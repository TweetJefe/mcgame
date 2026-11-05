package game.mechanics;



/** Границы обработки; порядок внутри фазы задаётся зависимостями шагов. */
public enum MechanicPhase {
    SETUP, MOVE_START, MOVE_PATH, MOVE_ATTACKER, MOVE_TARGET, ABILITY_GUARD, ABILITY_PREPARE, ENTRY, SETTLED, AFTER_SAFETY, AFTER_ACTION,
    AFTER_MOVE, CAN_END, END_TURN, HISTORY, END_HISTORY
}
