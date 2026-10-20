package game.heroes.moleminer;
import game.heroes.*;
import game.history.GameEvent;
import game.state.*;
import model.*;
import model.figures.King;
import java.util.*;
public final class MineValidator{
    private MineValidator(){}
    private static void require(boolean v,String message){if(!v)throw new IllegalArgumentException(message);}
    public static void validate(GameSnapshot s){
        require(s.mines().equals(MineState.fromHistory(s.events())),"Mines differ from history");
        if(s.mode()==GameMode.CLASSIC)require(s.mines().equals(MineState.empty()),"Mines in Classic");
        for(var entry:s.mines().cells().entrySet()){var mine=entry.getValue();var owner=s.pieces().get(mine.ownerId());require(owner!=null && (owner.hero()==HeroType.MOLEMINER_PAWN || owner.hero()==HeroType.GINSENG_BISHOP) && mine.plantedTurn()<=s.turn().number(),"Invalid mine source");var occupant=s.pieceAt(entry.getKey());require(occupant==null || occupant.isPotted() || occupant.color()==owner.color(),"Unresolved enemy on mine");}
        var dead=new HashSet<Long>();
        for(var e:s.events()){
            if(e.kind()==GameEvent.Kind.PLANT_MINE)require(e.from().equals(e.to()) && e.targetId()==null,"Mine must be planted under its owner");
            if(e.kind()==GameEvent.Kind.DETONATE){require(Math.max(Math.abs(e.from().getRow()-e.to().getRow()),Math.abs(e.from().getColumn()-e.to().getColumn()))<=3,"Jump out of range");require(s.pieces().get(e.actorId()).isRemoved(),"Bomber survived self-destruction");require(s.mines().blasts().stream().filter(b->b.ownerId()==e.actorId() && b.turn()==e.turn()).count()==1,"Ultimate without exactly one blast");}
            if(e.kind()==GameEvent.Kind.MINE_TRIGGER){var p=s.pieces().get(e.targetId());var actor=s.pieces().get(e.actorId());require(p!=null && p.type()!=King.class && p.color()!=actor.color(),"Invalid mine entrant");if(p.hero()!=HeroType.ILLUSION)require(s.events().stream().filter(c->c.kind()==GameEvent.Kind.MINE_CAPTURE && c.turn()==e.turn() && Objects.equals(c.actorId(),e.actorId()) && Objects.equals(c.targetId(),e.targetId()) && c.to().equals(e.to())).count()==1,"Mine trigger without exactly one death");}
            if(e.kind()==GameEvent.Kind.MINE_CAPTURE)require(s.events().stream().anyMatch(t->t.kind()==GameEvent.Kind.MINE_TRIGGER && t.turn()==e.turn() && Objects.equals(t.actorId(),e.actorId()) && Objects.equals(t.targetId(),e.targetId()) && t.to().equals(e.to())),"Capture without mine trigger");
            if(e.kind()==GameEvent.Kind.BLAST_CAPTURE || e.kind()==GameEvent.Kind.BLAST_DEATH){
                var p=s.pieces().get(e.targetId());var actor=s.pieces().get(e.actorId());boolean enemy=p.color()!=actor.color();
                require(p.type()!=King.class && p.hero()!=HeroType.ILLUSION && dead.add(p.id()) && p.status()==(enemy?PieceState.Status.CAPTURED:PieceState.Status.DESTROYED),"Invalid blast victim");
                require((e.kind()==GameEvent.Kind.BLAST_CAPTURE)==enemy && s.mines().blasts().stream().anyMatch(b->b.ownerId()==e.actorId() && b.turn()==e.turn() && b.contains(e.to())),"Death outside explosion");
            }
        }
        for(var b:s.mines().blasts())require(b.landing()!=null && MoleminerAbilities.corners(b.landing()).contains(b.corner()) && s.events().stream().anyMatch(e->e.kind()==GameEvent.Kind.DETONATE && e.turn()==b.turn() && e.actorId()==b.ownerId()),"Invalid random explosion area");
        for(var p:s.pieces().values())if(p.status()==PieceState.Status.DESTROYED)require(s.events().stream().anyMatch(e->Objects.equals(e.targetId(),p.id()) && Set.of(GameEvent.Kind.BLAST_DEATH,GameEvent.Kind.COUNTER_CAPTURE,GameEvent.Kind.ARES_REVENGE).contains(e.kind())),"Destroyed piece without a death event");
    }
}
