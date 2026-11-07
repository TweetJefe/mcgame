package game.heroes;

import game.mechanics.MechanicRegistry;

import game.mechanics.PieceRestriction;
import game.mechanics.PieceRestrictions;

import game.economy.EconomyState;
import game.heroes.clownganger.ClownState;
import game.heroes.faun.FaunState;
import game.heroes.ginseng.SpellState;
import game.heroes.moleminer.MineState;
import game.heroes.zeus.LightningState;
import game.mechanics.BuiltInStates;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicStates;
import game.state.PieceState;
import game.summoning.SummonState;
import game.terrain.TerrainState;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.Position;

/**
 * Immutable context for hero ability validation and previews.
 * Casts use a consistent field and piece snapshot. During death resolution, pieces are the
 * prepared states and field supplies the detached-piece factory; use pieceAt for occupancy.
 */
public record HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn,
                                MechanicStates mechanics, MechanicRegistry registry) {
    public HeroActionContext {
        Objects.requireNonNull(field, "field");
        pieces = Map.copyOf(pieces);
        Objects.requireNonNull(mechanics, "mechanics");
        Objects.requireNonNull(registry, "registry");
    }
    public HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn, MechanicStates mechanics) {
        this(field, pieces, currentTurn, mechanics, MechanicRegistry.EMPTY);
    }
    public PieceRestrictions restrictions(PieceState piece) { return registry.restrictions(this, piece); }
    public boolean blocks(PieceState piece, PieceRestriction restriction) {
        return restrictions(piece).blocks(restriction);
    }
    public java.util.Set<Position> passageObstacles() { return registry.passageObstacles(this); }

    public HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn) {
        this(field, pieces, currentTurn, MechanicStates.empty());
    }
    public HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn,
                             TerrainState terrain, FaunState faun) {
        this(field, pieces, currentTurn, terrain, faun, SpellState.empty());
    }
    public HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn,
                             TerrainState terrain, FaunState faun, SpellState spells) {
        this(field, pieces, currentTurn, terrain, faun, spells, EconomyState.disabled());
    }
    public HeroActionContext(Field field, Map<Long, PieceState> pieces, long currentTurn,
                             TerrainState terrain, FaunState faun, SpellState spells, EconomyState economy) {
        this(field, pieces, currentTurn, MechanicStates.empty()
                .with(BuiltInStates.TERRAIN, terrain).with(BuiltInStates.FAUN, faun)
                .with(BuiltInStates.SPELLS, spells).with(BuiltInStates.ECONOMY, economy));
    }
    public <S> S state(MechanicKey<S> key) { return mechanics.get(key); }
    public TerrainState terrain() { return state(BuiltInStates.TERRAIN); }
    public FaunState faun() { return state(BuiltInStates.FAUN); }
    public SpellState spells() { return state(BuiltInStates.SPELLS); }
    public EconomyState economy() { return state(BuiltInStates.ECONOMY); }
    public SummonState summons() { return state(BuiltInStates.SUMMONS); }
    public ClownState clown() { return state(BuiltInStates.CLOWN); }
    public MineState mines() { return state(BuiltInStates.MINES); }
    public LightningState lightning() { return state(BuiltInStates.LIGHTNING); }

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
