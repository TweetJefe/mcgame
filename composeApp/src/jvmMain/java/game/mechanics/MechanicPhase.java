package game.mechanics;



/** Границы обработки; порядок внутри фазы задаётся зависимостями шагов. */
public enum MechanicPhase {
    SETUP, BEGIN_TURN, SKIP_TURN, MOVE_START, MOVE_PATH, MOVE_ATTACKER, MOVE_TARGET, ABILITY_GUARD, ABILITY_PREPARE, ENTRY, SETTLED, AFTER_SAFETY, AFTER_ACTION,
    AFTER_MOVE, CAN_END, CAN_END_SETTLED, END_TURN, END_TURN_SETTLED, HISTORY, END_HISTORY
}
