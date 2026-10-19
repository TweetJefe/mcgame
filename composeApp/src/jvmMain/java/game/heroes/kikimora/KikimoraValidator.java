package game.heroes.kikimora;
import game.heroes.ginseng.SpellState;
import game.history.GameEvent;
import game.state.*;
import model.figures.King;
import java.util.*;
public final class KikimoraValidator{
    private KikimoraValidator(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalArgumentException(message);}
    public static void validate(GameSnapshot s){
        var pots=new HashSet<Long>();
        for(var e:s.events()){
            if(SpellState.activeAbility(e.kind())!=null || e.kind()==GameEvent.Kind.MOVE)require(!pots.contains(e.actorId()),"A pot cannot act");
            if(e.targetId()!=null && Set.of(GameEvent.Kind.MOVE,GameEvent.Kind.COUNTER_CAPTURE,GameEvent.Kind.BOG_CAPTURE,GameEvent.Kind.ARES_REVENGE,GameEvent.Kind.MINE_CAPTURE,GameEvent.Kind.LIGHTNING_CAPTURE,GameEvent.Kind.BLAST_CAPTURE,GameEvent.Kind.BLAST_DEATH,GameEvent.Kind.REPLACE,GameEvent.Kind.YARN_STUN).contains(e.kind()))require(!pots.contains(e.targetId()),"A pot cannot be captured, replaced or stunned");
            if(e.kind()==GameEvent.Kind.CLAY_POT){
                require(e.targetId()!=null && !Objects.equals(e.actorId(),e.targetId()) && pots.add(e.targetId()) && s.pieces().get(e.targetId()).type()!=King.class && distance(e.from(),e.to())==1,"Invalid clay pot conversion");
            }
            if(e.kind()==GameEvent.Kind.YARN){
                var target=s.pieces().get(e.targetId());var actor=s.pieces().get(e.actorId());
                require(target!=null && target.type()!=King.class && target.color()!=actor.color() && distance(e.from(),e.to())<=2 && distance(e.from(),e.to())>0 && !pots.contains(target.id()),"Invalid yarn target");
                var effects=s.events().stream().filter(p->p.kind()==GameEvent.Kind.YARN_STUN && p.turn()==e.turn() && Objects.equals(p.actorId(),e.actorId())).toList();
                require(effects.size()>=1 && effects.size()<=2 && Objects.equals(effects.getFirst().targetId(),e.targetId()) && effects.getFirst().to().equals(e.to()),"Yarn without matching first stun");
                if(effects.size()==2){
                    var second=effects.get(1);int ar=e.to().getRow()-e.from().getRow(),ac=e.to().getColumn()-e.from().getColumn();int br=second.to().getRow()-e.to().getRow(),bc=second.to().getColumn()-e.to().getColumn();
                    require(!Objects.equals(second.targetId(),e.targetId()) && distance(e.to(),second.to())<=2 && distance(e.to(),second.to())>0 && ar*bc==ac*br && ar*br+ac*bc>0,"Second yarn target is not behind the first on the flight ray");
                }
            }
        }
        for(var p:s.pieces().values()){
            require(p.isPotted()==pots.contains(p.id()),"Pot state differs from history");
            if(p.isPotted() && p.type()==model.figures.Rook.class && p.position().getRow()==(p.color()==model.FigureColor.WHITE?0:7) && (p.position().getColumn()==0 || p.position().getColumn()==7))require(!s.castlingRights().allows(p.color(),p.position().getColumn()==7),"A pot cannot retain rook castling rights");
            if(p.isPotted() && s.enPassantTarget()!=null)require(!p.position().equals(new model.Position(s.enPassantTarget().getRow()==2?3:4,s.enPassantTarget().getColumn())),"A pot cannot be an en passant victim");
        }
    }
    private static int distance(model.Position a,model.Position b){return Math.max(Math.abs(a.getRow()-b.getRow()),Math.abs(a.getColumn()-b.getColumn()));}
}
