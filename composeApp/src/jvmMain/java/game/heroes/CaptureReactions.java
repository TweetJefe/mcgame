package game.heroes;
import game.heroes.ares.AresAbilities;
import game.moves.*;
import game.state.PieceState;
import model.*;
import model.figures.King;
import java.util.*;

/** Resolves deaths in order, including loaded holders and Ares, without recursive stack growth. */
public final class CaptureReactions {
    private CaptureReactions(){}
    private record Death(PieceState victim,long killer,Position square){}
    public static HeroEffect resolve(Field factory,Map<Long,PieceState> pieces,PieceState victim,long captor,Position initialSquare,HeroRules rules) {
        var work=new HashMap<>(pieces);var updates=new HashMap<Long,PieceState>();var captures=new ArrayList<Capture>();
        var pending=new ArrayDeque<Death>();pending.add(new Death(victim,captor,initialSquare));int processed=0;
        while(!pending.isEmpty()) {
            if(++processed>pieces.size()*2L+1)return HeroEffect.rejected(MoveResult.INVALID_BOARD);
            Death death=pending.remove();Position square=death.square();PieceState dead=death.victim(),killer=work.get(death.killer());
            if(AresAbilities.armed(dead,rules.ares())) {
                PieceState spent=work.get(dead.id()).withAbilityState(dead.cooldown(),0,null);work.put(dead.id(),spent);updates.put(dead.id(),spent);
                if(killer==null || killer.isRemoved() || killer.isPotted())continue;
                if(killer.type()==King.class)return HeroEffect.rejected(MoveResult.KING_IN_CHECK);
                PieceState killed=killer.hidden(killer.color()==dead.color()?PieceState.Status.DESTROYED:PieceState.Status.CAPTURED);work.put(killed.id(),killed);updates.put(killed.id(),killed);
                captures.add(new Capture(dead.id(),killer.id(),dead.color(),factory.createFigure(killer.type(),killer.position()==null?square:killer.position(),killer.color()),killer.position()==null?square:killer.position(),true,Capture.Cause.ARES));
                pending.addFirst(new Death(killer,dead.id(),killer.position()==null?square:killer.position()));
            } else if(dead.storedPieceId()!=null) {
                PieceState ally=work.get(dead.storedPieceId());
                if(ally==null || ally.status()!=PieceState.Status.SWALLOWED)return HeroEffect.rejected(MoveResult.INVALID_BOARD);
                PieceState returned=ally.placed(square,ally.type());
                if(killer!=null && !killer.isRemoved() && !killer.isPotted()) {
                    if(killer.type()==King.class)return HeroEffect.rejected(MoveResult.KING_IN_CHECK);
                    PieceState killed=killer.hidden(killer.color()==dead.color()?PieceState.Status.DESTROYED:PieceState.Status.CAPTURED);work.put(killed.id(),killed);updates.put(killed.id(),killed);
                    captures.add(new Capture(ally.id(),killer.id(),ally.color(),factory.createFigure(killer.type(),killer.position()==null?square:killer.position(),killer.color()),killer.position()==null?square:killer.position(),true,Capture.Cause.PUDGE));
                    if(killer.color()!=ally.color())returned=HeroAbilities.afterCapture(returned,rules);pending.addFirst(new Death(killer,returned.id(),killer.position()==null?square:killer.position()));
                }
                work.put(returned.id(),returned);updates.put(returned.id(),returned);
            }
        }
        return new HeroEffect(MoveResult.SUCCESS,updates,captures);
    }
}
