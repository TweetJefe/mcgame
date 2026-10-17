package game.terrain;

import game.heroes.HeroType;
import game.history.GameEvent;
import game.state.*;
import model.Position;
import java.util.HashSet;
import java.util.Objects;

/** Cross-record checks for terrain ownership, expiry and hidden Bolotnik identities. */
public final class TerrainValidator {
    private TerrainValidator() {}
    public static void validate(GameSnapshot s) {
        Objects.requireNonNull(s.terrain(), "Missing terrain");
        require(s.mode() == GameMode.MOBA || s.terrain().cells().isEmpty(), "Terrain in classic game");
        var hiddenOwners = new HashSet<Long>();
        for (var entry : s.terrain().cells().entrySet()) {
            MudCell cell = entry.getValue(); PieceState owner = s.pieces().get(cell.ownerId());
            require(owner != null && (owner.hero() == HeroType.BOLOTNIK_KNIGHT || owner.hero()==HeroType.GINSENG_BISHOP) && !owner.isRemoved(), "Invalid terrain owner");
            PieceState occupant = s.pieceAt(entry.getKey());
            require(occupant == null || occupant.color() == owner.color(), "Unresolved enemy on mud");
            if (cell.kind() == MudCell.Kind.MUD) {
                require(cell.expiresAfterTurn() >= s.turn().number() && cell.expiresAfterTurn()-s.turn().number() <= 2L*s.bolotnikRules().mudTurns(), "Invalid mud expiry");
                require(s.events().stream().anyMatch(e -> e.kind() == GameEvent.Kind.MUD && Objects.equals(e.actorId(), owner.id())
                        && entry.getKey().equals(e.to()) && cell.expiresAfterTurn()-e.turn() == 2L*s.bolotnikRules().mudTurns()), "Mud without matching cast event");
            } else {
                require(owner.status() == PieceState.Status.HIDDEN_BOG, "Ambush without hidden hero"); hiddenOwners.add(owner.id());
            }
        }
        for (PieceState piece : s.pieces().values()) {
            require((piece.status() == PieceState.Status.HIDDEN_BOG) == hiddenOwners.contains(piece.id()), "Hidden hero differs from ambush terrain");
            if (piece.status() != PieceState.Status.HIDDEN_BOG) continue;
            GameEvent cast = null;
            for (GameEvent event : s.events()) if (event.kind() == GameEvent.Kind.AMBUSH && Objects.equals(event.actorId(), piece.id())) cast = event;
            require(cast != null, "Hidden hero has no ultimate event");
            var footprint = new HashSet<Position>();
            for (int c=Math.max(0,cast.from().getColumn()-1); c<=Math.min(7,cast.from().getColumn()+1); c++) footprint.add(new Position(cast.from().getRow(),c));
            var actual = new HashSet<Position>();
            s.terrain().cells().forEach((p,c) -> { if (c.ownerId()==piece.id() && c.kind()==MudCell.Kind.AMBUSH) actual.add(p); });
            require(footprint.equals(actual), "Incomplete or misplaced ambush footprint");
        }
        for (GameEvent event : s.events()) if (event.kind() == GameEvent.Kind.BOG_CAPTURE || event.kind() == GameEvent.Kind.MUD || event.kind() == GameEvent.Kind.AMBUSH)
            require(s.mode() == GameMode.MOBA && event.actorId() != null && (s.pieces().get(event.actorId()).hero() == HeroType.BOLOTNIK_KNIGHT || s.pieces().get(event.actorId()).hero()==HeroType.GINSENG_BISHOP)
                    && s.pieces().get(event.actorId()).color() == event.color(), "Invalid Bolotnik event source");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalArgumentException(message); }
}
