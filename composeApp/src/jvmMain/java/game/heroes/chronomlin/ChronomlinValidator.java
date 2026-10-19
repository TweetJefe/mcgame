package game.heroes.chronomlin;

import game.heroes.HeroType;
import game.history.GameEvent;
import game.state.*;
import model.Figure;
import model.figures.*;
import java.util.*;

/** Forms follow successful upgrades and ordinary pawn promotions, never a mutable level counter. */
public final class ChronomlinValidator {
    private ChronomlinValidator(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalArgumentException(message);}
    private record Replay(Map<Long,Class<? extends Figure>> forms,Map<Long,Class<? extends Figure>> illusions){}
    private static Replay replay(GameSnapshot s){
        var forms=new HashMap<Long,Class<? extends Figure>>();var bodies=new HashMap<Long,Class<? extends Figure>>();
        for(var piece:s.pieces().values()){
            if(piece.hero()==HeroType.CHRONOMLIN_PAWN)forms.put(piece.id(),Pawn.class);
            if(piece.hero()==HeroType.GINSENG_BISHOP)forms.put(piece.id(),Bishop.class);
        }
        int moveIndex=0;
        for(var e:s.events()){
            if(e.kind()==GameEvent.Kind.MOVE){
                var move=s.moves().get(moveIndex++);
                if(forms.containsKey(e.actorId()) && move.getPromotion()!=null){
                    require(forms.get(e.actorId())==Pawn.class,"Only a pawn can promote on the last rank");
                    forms.put(e.actorId(),move.getPromotion().getFigureType());
                }
            }
            if(e.kind()==GameEvent.Kind.EVOLVE){
                var next=ChronomlinAbilities.nextType(forms.get(e.actorId()));
                require(next!=null && e.targetId()==null && e.from().equals(e.to()),"Invalid evolution step");
                forms.put(e.actorId(),next);
            }
            if(e.kind()==GameEvent.Kind.ILLUSION)bodies.put(e.targetId(),forms.getOrDefault(e.actorId(),Knight.class));
            if(e.kind()==GameEvent.Kind.GRAPPLE){
                require(e.targetId()!=null && forms.containsKey(e.actorId()),"Invalid grapple source or anchor");
                int dr=Math.abs(e.from().getRow()-e.to().getRow()),dc=Math.abs(e.from().getColumn()-e.to().getColumn());
                require((dr==0 || dc==0 || dr==dc) && Math.max(dr,dc)>1,"Invalid grapple ray");
            }
        }
        return new Replay(Map.copyOf(forms),Map.copyOf(bodies));
    }
    public static Map<Long,Class<? extends Figure>> illusionBodies(GameSnapshot s){return replay(s).illusions();}
    public static void validate(GameSnapshot s){
        var forms=replay(s).forms();
        for(var entry:forms.entrySet())require(s.pieces().get(entry.getKey()).type()==entry.getValue(),"Hero form differs from evolution and promotion history");
    }
}
