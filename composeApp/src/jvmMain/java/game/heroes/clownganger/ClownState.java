package game.heroes.clownganger;
import game.heroes.HeroType;
import game.history.GameEvent;
import game.state.*;
import model.*;
import java.util.*;

/** Illusion origins are retained after dismissal; movement belongs to one original/illusion group per turn. */
public record ClownState(Map<Long,Illusion> illusions,long movementTurn,Long movementGroup,Set<Long> moved){
    public record Illusion(long id,long sourceId,Position bornAt,long bornTurn,long expiresAfterTurn,boolean extended){}
    public ClownState{illusions=Map.copyOf(illusions);moved=Set.copyOf(moved);}
    public static ClownState empty(){return new ClownState(Map.of(),0,null,Set.of());}
    public ClownState create(long id,long source,Position at,long turn,ClowngangerRules r){
        var next=new HashMap<>(illusions);next.put(id,new Illusion(id,source,at,turn,Math.addExact(turn,2L*r.illusionTurns()),false));
        return new ClownState(next,movementTurn,movementGroup,moved);
    }
    public long group(long id){var i=illusions.get(id);return i==null?id:i.sourceId();}
    public boolean canMove(long id,long turn,boolean used,Map<Long,PieceState> pieces){
        if(!used)return true;
        if(movementTurn!=turn || movementGroup==null || moved.size()>=2 || moved.contains(id) || group(id)!=movementGroup)return false;
        if(!moved.contains(group(id)) && id!=group(id))return false;
        return illusions.values().stream().anyMatch(i->i.sourceId()==movementGroup && pieces.get(i.id())!=null && !pieces.get(i.id()).isRemoved() && (id==i.id() || id==i.sourceId()));
    }
    public ClownState move(long id,long turn){
        var next=movementTurn==turn?new HashSet<>(moved):new HashSet<Long>();next.add(id);
        return new ClownState(illusions,turn,movementTurn==turn?movementGroup:group(id),next);
    }
    public ClownState hit(long id){
        var i=illusions.get(id);if(i==null || i.extended())return this;
        var next=new HashMap<>(illusions);next.put(id,new Illusion(id,i.sourceId(),i.bornAt(),i.bornTurn(),Math.addExact(i.expiresAfterTurn(),2),true));
        return new ClownState(next,movementTurn,movementGroup,moved);
    }
    public Map<Long,PieceState> clean(Map<Long,PieceState> pieces,long endedTurn){
        var next=new HashMap<>(pieces);
        for(var i:illusions.values()){
            var source=pieces.get(i.sourceId());var p=pieces.get(i.id());
            if(p!=null && !p.isRemoved() && !p.isPotted() && (source==null || source.isRemoved() || i.expiresAfterTurn()<=endedTurn))next.put(p.id(),p.hidden(PieceState.Status.DISSOLVED));
        }
        for(var holder:new ArrayList<>(next.values()))if(holder.storedPieceId()!=null && next.get(holder.storedPieceId()).isRemoved())next.put(holder.id(),holder.withAbilityState(holder.cooldown(),holder.ultimateCharge(),null));
        return Map.copyOf(next);
    }
    public static ClownState fromHistory(List<GameEvent> events,ClowngangerRules rules){
        ClownState state=empty();
        for(var e:events){
            if(e.kind()==GameEvent.Kind.ILLUSION)state=state.create(e.targetId(),e.actorId(),e.to(),e.turn(),rules);
            if(e.kind()==GameEvent.Kind.MOVE)state=state.move(e.actorId(),e.turn());
            if(e.kind()==GameEvent.Kind.ILLUSION_HIT)state=state.hit(e.targetId());
        }
        return state;
    }
    public static boolean canMove(GameSnapshot s,long id){return s.clown().canMove(id,s.turn().number(),s.turn().moveUsed(),s.pieces());}
}
