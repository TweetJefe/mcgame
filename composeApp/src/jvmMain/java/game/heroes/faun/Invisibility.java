package game.heroes.faun;

public record Invisibility(long castTurn, long expiresAfterTurn) {
    public Invisibility {
        if (castTurn < 1 || expiresAfterTurn <= castTurn) throw new IllegalArgumentException("Invalid invisibility");
    }
}
