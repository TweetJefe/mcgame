package game.heroes;

import game.state.PieceState;
import game.terrain.TerrainState;
import game.heroes.faun.FaunState;
import game.heroes.ginseng.SpellState;
import game.economy.EconomyState;
import model.Field;
import model.Position;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable context for hero ability validation and previews.
 * Casts use a consistent field and piece snapshot. During death resolution, pieces are the
 * prepared states and field supplies the detached-piece factory; use pieceAt for occupancy.
 */
public record HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn,
                                TerrainState terrain, FaunState faun, SpellState spells, EconomyState economy) {
    public HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn) {
        this(field, pieces, currentTurn, TerrainState.empty(), FaunState.empty());
    }

    public HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn,
                             TerrainState terrain, FaunState faun) {
        this(field, pieces, currentTurn, terrain, faun, SpellState.empty());
    }

    public HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn,
                             TerrainState terrain, FaunState faun, SpellState spells) {
        this(field, pieces, currentTurn, terrain, faun, spells, EconomyState.disabled());
    }

    public HeroActionContext {
        Objects.requireNonNull(field, "field");
        pieces = Map.copyOf(pieces);
        Objects.requireNonNull(terrain, "terrain");
        Objects.requireNonNull(faun, "faun");
        Objects.requireNonNull(spells, "spells");
        Objects.requireNonNull(economy, "economy");
    }

    /** Finds an active board piece at the supplied square. */
    public PieceState pieceAt(Position position) {
        if (position == null) return null;
        return pieces.values().stream()
                .filter(p -> p.status() == PieceState.Status.BOARD && position.equals(p.position()))
                .findFirst()
                .orElse(null);
    }

    public PieceState pieceById(long id) {
        return pieces.get(id);
    }
}
