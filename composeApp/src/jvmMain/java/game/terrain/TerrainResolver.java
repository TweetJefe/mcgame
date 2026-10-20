package game.terrain;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.heroes.*;
import game.moves.Capture;
import game.moves.MoveResult;
import game.state.PieceState;
import model.*;
import model.figures.King;

import java.util.*;

/**
 * Pure placement and entry reactions, including releases from Pudge. Never mutates a live game.
 */
public final class TerrainResolver {
    private TerrainResolver() {
    }

    public record Placement(MoveResult result, TerrainState terrain, List<Long> immediateEntries) {
        public Placement {
            immediateEntries = List.copyOf(immediateEntries);
        }
    }

    public record Result(MoveResult result, Map<Long, PieceState> pieces, TerrainState terrain,
                         List<Capture> captures, List<Long> illusionHits) {
        public Result(MoveResult result,Map<Long,PieceState> pieces,TerrainState terrain,List<Capture> captures){this(result,pieces,terrain,captures,List.of());}
        public Result {
            pieces = Map.copyOf(pieces);
            captures = List.copyOf(captures); illusionHits=List.copyOf(illusionHits);
        }
    }

    public static Placement place(TerrainState terrain, Map<Long, PieceState> before, PieceState actor,
                                  Position square, Ability ability, long turn, HeroRules rules) {
        if (ability != Ability.MUD && ability != Ability.AMBUSH)
            return new Placement(Success.SUCCESS, terrain, List.of());
        var cells = new HashMap<>(terrain.cells());
        var footprint = new ArrayList<Position>();
        if (ability == Ability.MUD) footprint.add(square);
        else
            for (int c = Math.max(0, actor.position().getColumn() - 1); c <= Math.min(7, actor.position().getColumn() + 1); c++)
                footprint.add(new Position(actor.position().getRow(), c));
        for (Position p : footprint) {
            PieceState occupant = at(before, p);
            if (occupant != null && occupant.isBoxed())return new Placement(StatusRejection.BOXED,terrain,List.of());
            if (occupant != null && occupant.type() == King.class)
                return new Placement(StatusRejection.KING_IMMUNE, terrain, List.of());
            MudCell old = cells.get(p);
            if (old != null && old.ownerId() != actor.id())
                return new Placement(AbilityRejection.TERRAIN_OCCUPIED, terrain, List.of());
        }
        Long firstEnemy = null;
        for (Position p : footprint) {
            cells.put(p, new MudCell(actor.id(), ability == Ability.MUD ? MudCell.Kind.MUD : MudCell.Kind.AMBUSH,
                    ability == Ability.MUD ? Math.addExact(turn, 2L * rules.bolotnik().mudTurns()) : 0));
            PieceState occupant = at(before, p);
            if (firstEnemy == null && occupant != null && !occupant.isPotted() && occupant.color() != actor.color()) firstEnemy = occupant.id();
        }
        return new Placement(Success.SUCCESS, new TerrainState(cells), firstEnemy == null ? List.of() : List.of(firstEnemy));
    }

    public static Result resolve(Field factory, Map<Long, PieceState> before, Map<Long, PieceState> prepared,
                                 TerrainState terrain, List<Capture> initialCaptures, List<Long> immediateEntries, HeroRules rules) {
        var states = new HashMap<>(prepared);
        var cells = new HashMap<>(terrain.cells());
        var illusionHits=new ArrayList<Long>();
        var captures = new ArrayList<>(initialCaptures);
        var entries = new ArrayDeque<Long>(immediateEntries);
        prepared.values().stream().filter(p -> p.status() == PieceState.Status.BOARD).sorted(Comparator.comparingLong(PieceState::id))
                .filter(p -> before.get(p.id()) == null || before.get(p.id()).status() != PieceState.Status.BOARD
                        || !p.position().equals(before.get(p.id()).position())).forEach(p -> entries.add(p.id()));
        int reactions = 0;
        while (!entries.isEmpty()) {
            removeDeadOwners(cells, states);
            PieceState entrant = states.get(entries.remove());
            if (entrant == null || entrant.status() != PieceState.Status.BOARD) continue;
            Position square = entrant.position();
            MudCell trap = cells.get(square);
            if (trap == null) continue;
            PieceState owner = states.get(trap.ownerId());
            if (entrant.isBoxed() || entrant.isPotted())continue;
            if (owner == null || owner.isRemoved() || owner.isPotted() || owner.color() == entrant.color()) continue;
            if(entrant.hero()==HeroType.ILLUSION){illusionHits.add(entrant.id());continue;}
            if (entrant.type() == King.class)
                return new Result(StatusRejection.KING_IMMUNE, before, terrain, initialCaptures);
            if (++reactions > 64) return new Result(MovementRejection.INVALID_BOARD, before, terrain, initialCaptures);
            boolean hidden = owner.status() == PieceState.Status.HIDDEN_BOG;
            cells.remove(square);
            if (trap.kind() == MudCell.Kind.AMBUSH || hidden)
                cells.entrySet().removeIf(e -> e.getValue().ownerId() == owner.id() && e.getValue().kind() == MudCell.Kind.AMBUSH);
            // A living Bolotnik swallowed by Pudge is still the same hero: detach it before emerging.
            if (owner.status() == PieceState.Status.SWALLOWED) {
                for (PieceState holder : new ArrayList<>(states.values()))
                    if (Objects.equals(holder.storedPieceId(), owner.id()))
                        states.put(holder.id(), holder.withAbilityState(holder.cooldown(), holder.ultimateCharge(), null));
            }
            states.put(entrant.id(), entrant.hidden(PieceState.Status.CAPTURED));
            states.put(owner.id(), HeroAbilities.afterCapture(owner.placed(square, owner.type()), rules));
            captures.add(new Capture(owner.id(), entrant.id(), owner.color(), factory.createFigure(entrant.type(), square, entrant.color()),
                    square, true, Capture.Cause.BOG));
            HeroEffect reaction = HeroAbilities.captureReaction(factory, states, entrant, owner.id(), square, rules);
            if (!reaction.result().isSuccess()) return new Result(reaction.result(), before, terrain, initialCaptures);
            states.putAll(reaction.updates());
            captures.addAll(reaction.captures());
            reaction.updates().values().stream().filter(p -> p.status() == PieceState.Status.BOARD).forEach(p -> entries.add(p.id()));
        }
        removeDeadOwners(cells, states);
        return new Result(Success.SUCCESS, states, new TerrainState(cells), captures,illusionHits);
    }

    private static void removeDeadOwners(Map<Position, MudCell> cells, Map<Long, PieceState> states) {
        cells.entrySet().removeIf(e -> !states.containsKey(e.getValue().ownerId()) || states.get(e.getValue().ownerId()).isRemoved());
    }

    private static PieceState at(Map<Long, PieceState> states, Position square) {
        return states.values().stream().filter(p -> p.status() == PieceState.Status.BOARD && p.position().equals(square)).findFirst().orElse(null);
    }
}
