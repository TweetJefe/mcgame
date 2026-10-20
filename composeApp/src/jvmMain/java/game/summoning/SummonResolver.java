package game.summoning;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
import game.heroes.*;
import game.moves.*;
import game.state.*;
import game.terrain.*;
import model.*;
import model.figures.*;

import java.util.*;

/**
 * Pure creation, removal without a capture, and reactions after the ordinary capture chain.
 */
public final class SummonResolver {
    private SummonResolver() {
    }

    public record Result(MoveResult result, Map<Long, PieceState> pieces, SummonState summons,
                         TerrainState terrain, List<Capture> captures,List<Long> illusionHits) {
        public Result(MoveResult result,Map<Long,PieceState> pieces,SummonState summons,TerrainState terrain,List<Capture> captures){this(result,pieces,summons,terrain,captures,List.of());}
        public Result {
            pieces = Map.copyOf(pieces);
            captures = List.copyOf(captures);illusionHits=List.copyOf(illusionHits);
        }
    }

    public static Result cast(Map<Long, PieceState> prepared, SummonState summons, PieceState actor, long turn,
                              FigureColor actingSide, HeroRules rules, TerrainState terrain) {
        var cells = new ArrayList<Position>();
        int first = actor.color() == FigureColor.WHITE ? 0 : 6;
        for (int row = first; row < first + 2; row++)
            for (int col = 0; col < 8; col++) {
                Position p = new Position(row, col);
                if (at(prepared, p) == null) cells.add(p);
            }
        if (cells.isEmpty()) for (int row = first; row < first + 2; row++)
            for (int col = 0; col < 8; col++) {
                Position p = new Position(row, col);
                if (at(prepared, p).type() != King.class && !at(prepared,p).isBoxed() && !at(prepared,p).isPotted()) cells.add(p);
            }
        return spawn(prepared, summons, actor, turn, actingSide, rules, terrain, cells, SummonRecord.Cause.ULTIMATE);
    }

    private static Result spawn(Map<Long, PieceState> prepared, SummonState summons, PieceState source, long turn,
                                FigureColor actingSide, HeroRules rules, TerrainState terrain, List<Position> cells, SummonRecord.Cause cause) {
        if (cells.isEmpty()) return new Result(cause==SummonRecord.Cause.ULTIMATE?AbilityRejection.NO_SUMMON_SPACE:Success.SUCCESS, prepared, summons, terrain, List.of());
        if (prepared.size() >= SummonState.MAX_IDENTITIES)
            return new Result(MovementRejection.INVALID_BOARD, prepared, summons, terrain, List.of());
        var choice = summons.choose(cells.size());
        Position square = cells.get(choice.index());
        var states = new HashMap<>(prepared);
        PieceState occupant = at(states, square);
        if (occupant != null) remove(states, occupant); // Replacement is neither a capture nor a death reaction.
        long id = Math.addExact(states.keySet().stream().mapToLong(Long::longValue).max().orElse(0), 1);
        Field factory = new Field(8);
        PieceState baby = PieceState.initial(id, factory.createFigure(Pawn.class, square, source.color()), HeroType.BABA_YAGA_PAWN);
        states.put(id, baby);
        var records = new HashMap<>(summons.records());
        long deadline = Math.addExact(turn, 2L * rules.izba().lifetimeTurns() - (source.color() == actingSide ? 0 : 1));
        records.put(id, new SummonRecord(id, source.id(), turn, deadline, square, cause, false));
        return new Result(Success.SUCCESS, states, new SummonState(true, choice.nextState(), records), terrain, List.of());
    }

    private static void remove(Map<Long, PieceState> states, PieceState victim) {
        Long passenger = victim.storedPieceId();
        states.put(victim.id(), victim.hidden(PieceState.Status.REPLACED));
        if (passenger != null) remove(states, states.get(passenger));
    }

    public static Result resolve(Field factory, Map<Long, PieceState> prepared, SummonState summons, TerrainState terrain,
                                 List<Capture> captures, long turn, FigureColor actingSide, HeroRules rules) {
        return resolve(factory,prepared,summons,terrain,captures,turn,actingSide,rules,new HashSet<>());
    }
    public static Result resolve(Field factory,Map<Long,PieceState> prepared,SummonState summons,TerrainState terrain,List<Capture> captures,long turn,FigureColor actingSide,HeroRules rules,Set<Long> handled){
        var illusionHits=new ArrayList<Long>();
        var states = prepared;
        var all = new ArrayList<>(captures);
        while (true) {
            Capture death = all.stream().filter(c -> !handled.contains(c.victimId()) && statesHero(prepared, c.victimId()) == HeroType.IZBA_ROOK).findFirst().orElse(null);
            if (death == null) break;
            handled.add(death.victimId());
            PieceState source = states.get(death.victimId());
            var cells = new ArrayList<Position>();
            Position center = death.square();
            for (int row = Math.max(0, center.getRow() - 1); row <= Math.min(7, center.getRow() + 1); row++)
                for (int col = Math.max(0, center.getColumn() - 1); col <= Math.min(7, center.getColumn() + 1); col++) {
                    Position p = new Position(row, col);
                    if (!p.equals(center) && at(states, p) == null) cells.add(p);
                }
            var spawned = spawn(states, summons, source, turn, actingSide, rules, terrain, cells, SummonRecord.Cause.DEATH);
            if (!spawned.result().isSuccess()) return spawned;
            var resolved = TerrainResolver.resolve(factory, states, spawned.pieces(), terrain, all, List.of(), rules);
            if (!resolved.result().isSuccess())
                return new Result(resolved.result(), prepared, summons, terrain, captures);
            illusionHits.addAll(resolved.illusionHits());
            states = resolved.pieces();
            terrain = resolved.terrain();
            all = new ArrayList<>(resolved.captures());
            summons = spawned.summons();
        }
        return new Result(Success.SUCCESS, states, summons, terrain, all,illusionHits);
    }

    private static HeroType statesHero(Map<Long, PieceState> states, long id) {
        return states.get(id) == null ? HeroType.NONE : states.get(id).hero();
    }

    public static Result mature(Map<Long, PieceState> pieces, SummonState summons, long endedTurn, FigureColor owner, TerrainState terrain) {
        var states = new HashMap<>(pieces);
        var records = new HashMap<>(summons.records());
        for (var record : summons.records().values()) {
            PieceState p = states.get(record.id());
            if (!record.transformed() && record.transformsAfterTurn() <= endedTurn && p.color() == owner && !p.isRemoved() && !p.isPotted()) {
                states.put(p.id(), new PieceState(p.id(), Rook.class, p.color(), p.position(), p.status(), HeroType.IZBA_ROOK, 0, 0, p.inventory(), null, p.blindTurns(), null, p.stunnedThroughTurn(),p.boxedThroughTurn()));
                records.put(record.id(), record.mature());
            }
        }
        return new Result(Success.SUCCESS, states, new SummonState(summons.enabled(), summons.randomState(), records), terrain, List.of());
    }

    private static PieceState at(Map<Long, PieceState> pieces, Position square) {
        return pieces.values().stream().filter(p -> p.status() == PieceState.Status.BOARD && p.position().equals(square)).findFirst().orElse(null);
    }
}
