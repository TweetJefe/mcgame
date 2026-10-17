package game.terrain;

public record MudCell(long ownerId, Kind kind, long expiresAfterTurn) {
    public enum Kind { MUD, AMBUSH }
    public MudCell {
        if (ownerId <= 0 || kind == null || (kind == Kind.MUD ? expiresAfterTurn <= 0 : expiresAfterTurn != 0))
            throw new IllegalArgumentException("Invalid mud cell");
    }
}
