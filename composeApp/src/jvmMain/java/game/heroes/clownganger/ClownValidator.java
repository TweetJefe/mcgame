package game.heroes.clownganger;
import game.heroes.*;
import game.history.GameEvent;
import game.state.*;
import model.figures.*;
import java.util.*;
public final class ClownValidator {
    private ClownValidator(){}
    private static void require(boolean ok,String msg){if(!ok)throw new IllegalArgumentException(msg);}
    public static void validate(GameSnapshot s){
        require(s.clown().equals(ClownState.fromHistory(s.events(),s.heroRules().clownganger())),"Clownganger state differs from history");
        var bodies=game.heroes.chronomlin.ChronomlinValidator.illusionBodies(s);
        var boxes=new HashMap<Long,Long>();var moved=new HashMap<Long,Set<Long>>();var born=new HashSet<Long>();
        for(var e:s.events()){
            if(e.kind()==GameEvent.Kind.BOX){
                var actor=s.pieces().get(e.actorId());var target=s.pieces().get(e.targetId());
                require(target!=null && actor.color()==target.color() && Math.max(Math.abs(e.from().getRow()-e.to().getRow()),Math.abs(e.from().getColumn()-e.to().getColumn()))<=3,"Invalid box target");
                boxes.put(target.id(),Math.addExact(e.turn(),2L*s.heroRules().clownganger().boxTurns()));
            }
            if(e.kind()==GameEvent.Kind.ILLUSION){require(e.targetId()!=null && born.add(e.targetId()) && Math.max(Math.abs(e.from().getRow()-e.to().getRow()),Math.abs(e.from().getColumn()-e.to().getColumn()))<=1,"Invalid illusion birth");}
            if(e.kind()==GameEvent.Kind.END_TURN)boxes.values().removeIf(t->t<=e.turn());
            if(e.kind()==GameEvent.Kind.MOVE){
                var ids=moved.computeIfAbsent(e.turn(),k->new HashSet<>());require(ids.add(e.actorId()) && ids.size()<=2,"Piece moved twice or too many moves");
                if(ids.size()==2){var it=ids.iterator();long a=it.next(),b=it.next();long group=s.clown().group(a);require(group==s.clown().group(b) && (a==group || b==group) && (born.contains(a) || born.contains(b)),"Two unrelated moves in one turn");}
            }
            if(e.kind()==GameEvent.Kind.COPY_SPELL && e.copiedAbility()==null)require(s.pieces().get(e.targetId()).hero()==HeroType.ILLUSION,"Empty copy from a real hero");
            if(e.kind()==GameEvent.Kind.ILLUSION_HIT){
                require(born.contains(e.targetId()) && (s.events().stream().anyMatch(t->t.turn()==e.turn() && java.util.Objects.equals(t.actorId(),e.actorId()) && ((t.kind()==GameEvent.Kind.MINE_TRIGGER || t.kind()==GameEvent.Kind.LIGHTNING_DISCHARGE) && java.util.Objects.equals(t.targetId(),e.targetId()) || t.kind()==GameEvent.Kind.BLAST_AREA)) || s.events().stream().anyMatch(m->m.turn()==e.turn() && m.kind()==GameEvent.Kind.MOVE && Objects.equals(m.actorId(),e.actorId()) && m.to().equals(e.to())) || ((s.pieces().get(e.actorId()).hero()==HeroType.BOLOTNIK_KNIGHT || s.pieces().get(e.actorId()).hero()==HeroType.GINSENG_BISHOP) && s.events().stream().anyMatch(t->t.sequence()<e.sequence() && java.util.Objects.equals(t.actorId(),e.actorId()) && (t.kind()==GameEvent.Kind.MUD || t.kind()==GameEvent.Kind.AMBUSH)))),"Illusion hit without a move or trap");
            }
        }
        for(var i:s.clown().illusions().values()){
            var p=s.pieces().get(i.id());var source=s.pieces().get(i.sourceId());
            require(p!=null && source!=null && (source.hero()==HeroType.CLOWNGANGER_KNIGHT || source.hero()==HeroType.GINSENG_BISHOP) && p.hero()==HeroType.ILLUSION && p.type()==bodies.get(i.id()) && p.color()==source.color(),"Invalid illusion identity");
            require(p.status()!=PieceState.Status.CAPTURED && (p.isRemoved() || p.isPotted() || (i.expiresAfterTurn()>=s.turn().number() && !source.isRemoved())),"Overdue or captured illusion");
        }
        for(var p:s.pieces().values()){
            require((p.hero()==HeroType.ILLUSION)==s.clown().illusions().containsKey(p.id()),"Illusion without origin");
            require(p.status()!=PieceState.Status.DISSOLVED || p.hero()==HeroType.ILLUSION,"Real piece dissolved");
            require(p.boxedThroughTurn()==(p.isRemoved()?0:boxes.getOrDefault(p.id(),0L)),"Box duration differs from history");
        }
        if(s.mode()==GameMode.CLASSIC)require(s.clown().illusions().isEmpty() && boxes.isEmpty(),"Clownganger effects in Classic");
    }
}
