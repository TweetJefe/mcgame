package game.heroes.moleminer;
import game.history.GameEvent;
import game.state.PieceState;
import model.*;
import java.util.*;

/** Mines survive their owner; trigger and blast records are replayed from the journal. */
public record MineState(Map<Position,Mine> cells,List<Trigger> triggers,List<Blast> blasts){
    public record Mine(long ownerId,long plantedTurn){}
    public record Trigger(long ownerId,long victimId,Position square,long turn){}
    public record Blast(long ownerId,long turn,Position landing,Position corner){
        public boolean contains(Position p){return p!=null && p.getRow()>=corner.getRow() && p.getRow()<=corner.getRow()+1 && p.getColumn()>=corner.getColumn() && p.getColumn()<=corner.getColumn()+1;}
        public Position end(){return new Position(corner.getRow()+1,corner.getColumn()+1);}
    }
    public MineState{cells=Map.copyOf(cells);triggers=List.copyOf(triggers);blasts=List.copyOf(blasts);if(cells.size()>64)throw new IllegalArgumentException("Too many mine cells");}
    public static MineState empty(){return new MineState(Map.of(),List.of(),List.of());}
    public boolean hostile(Position square,Map<Long,PieceState> pieces,FigureColor side){var m=cells.get(square);return m!=null && pieces.get(m.ownerId()).color()!=side;}
    public MineState plant(long owner,Position at,long turn){var next=new HashMap<>(cells);if(next.putIfAbsent(at,new Mine(owner,turn))!=null)throw new IllegalArgumentException("Cell already mined");return new MineState(next,triggers,blasts);}
    public MineState trigger(long owner,long victim,Position at,long turn){var next=new HashMap<>(cells);var mine=next.remove(at);if(mine==null || mine.ownerId()!=owner)throw new IllegalArgumentException("Trigger without a mine");var log=new ArrayList<>(triggers);log.add(new Trigger(owner,victim,at,turn));return new MineState(next,log,blasts);}
    public MineState blast(Blast blast){var next=new ArrayList<>(blasts);next.add(blast);return new MineState(cells,triggers,next);}
    public static MineState fromHistory(List<GameEvent> events){
        MineState state=empty();var landings=new HashMap<Long,Position>();
        for(var e:events){
            if(e.kind()==GameEvent.Kind.PLANT_MINE)state=state.plant(e.actorId(),e.to(),e.turn());
            if(e.kind()==GameEvent.Kind.DETONATE)landings.put(e.actorId(),e.to());
            if(e.kind()==GameEvent.Kind.MINE_TRIGGER)state=state.trigger(e.actorId(),e.targetId(),e.to(),e.turn());
            if(e.kind()==GameEvent.Kind.BLAST_AREA){var b=new Blast(e.actorId(),e.turn(),landings.get(e.actorId()),e.from());if(!b.end().equals(e.to()))throw new IllegalArgumentException("Invalid blast footprint");state=state.blast(b);}
        }
        return state;
    }
}
