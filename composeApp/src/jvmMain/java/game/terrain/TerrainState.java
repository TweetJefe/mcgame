package game.terrain;

import game.state.PieceState;
import model.FigureColor;
import model.Position;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/** Terrain is separate from pieces: a friendly piece may stand on mud. */
public record TerrainState(Map<Position, MudCell> cells) {
    public TerrainState {
        cells = Map.copyOf(cells);
        if (cells.size() > 64 || cells.keySet().stream().anyMatch(p -> p.getRow() < 0 || p.getRow() > 7 || p.getColumn() < 0 || p.getColumn() > 7))
            throw new IllegalArgumentException("Invalid terrain placement");
    }
    public static TerrainState empty() { return new TerrainState(Map.of()); }
    public Set<Position> hostileSquares(Map<Long, PieceState> pieces, FigureColor side) {
        return cells.entrySet().stream().filter(e -> pieces.containsKey(e.getValue().ownerId())
                && pieces.get(e.getValue().ownerId()).color() != side).map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
    }
    public TerrainState expire(long endedTurn) {
        var next = new HashMap<>(cells);
        next.entrySet().removeIf(e -> e.getValue().kind() == MudCell.Kind.MUD && e.getValue().expiresAfterTurn() <= endedTurn);
        return new TerrainState(next);
    }
}
