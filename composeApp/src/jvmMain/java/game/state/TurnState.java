package game.state;


/** A player turn can contain one move, one skill OR ultimate, and independent item commands. */
public record TurnState(long number, boolean moveUsed, boolean abilityUsed) {
    public boolean canEndTurn() { return moveUsed; }
}
