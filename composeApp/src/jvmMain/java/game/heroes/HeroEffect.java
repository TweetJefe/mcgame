package game.heroes;

import game.moves.Capture;
import game.moves.MoveResult;
import game.state.PieceState;

import java.util.List;
import java.util.Map;

/** Immutable proposed changes. They never modify a live Game or the supplied state. */
public record HeroEffect(MoveResult result, Map<Long, PieceState> updates, List<Capture> captures) {
    public HeroEffect { updates = Map.copyOf(updates); captures = List.copyOf(captures); }
    public static HeroEffect rejected(MoveResult result) { return new HeroEffect(result, Map.of(), List.of()); }
    public static HeroEffect success(Map<Long, PieceState> updates) { return new HeroEffect(MoveResult.SUCCESS, updates, List.of()); }
    public static HeroEffect empty() { return success(Map.of()); }
}
