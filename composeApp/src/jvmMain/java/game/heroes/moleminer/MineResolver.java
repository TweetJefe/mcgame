package game.heroes.moleminer;
import game.heroes.*;
import game.moves.*;
import game.state.PieceState;
import model.*;
import model.figures.King;
import java.util.*;
public final class MineResolver{
    private MineResolver(){}
    public record Result(MoveResult result,Map<Long,PieceState> pieces,MineState mines,List<Capture> captures,List<Long> illusionHits){}
    public static Result resolve(Field factory,Map<Long,PieceState> before,Map<Long,PieceState> prepared,MineState mines,List<Capture> previous,long turn,HeroRules rules){
        var states=new HashMap<>(prepared);var captures=new ArrayList<>(previous);var hits=new ArrayList<Long>();var entries=new ArrayDeque<Long>();
        prepared.values().stream().filter(p->p.position()!=null && (before.get(p.id())==null || !p.position().equals(before.get(p.id()).position()))).sorted(Comparator.comparingLong(PieceState::id)).forEach(p->entries.add(p.id()));
        long reactions=0;
        while(!entries.isEmpty()){
            var p=states.get(entries.remove());if(p==null || p.position()==null || p.isBoxed() || p.isPotted() || !mines.hostile(p.position(),states,p.color()))continue;
            if(p.type()==King.class)return new Result(MoveResult.KING_IMMUNE,before,mines,previous,List.of());
            if(++reactions>states.size()*2L+64)return new Result(MoveResult.INVALID_BOARD,before,mines,previous,List.of());
            var owner=states.get(mines.cells().get(p.position()).ownerId());mines=mines.trigger(owner.id(),p.id(),p.position(),turn);
            if(p.hero()==HeroType.ILLUSION){hits.add(p.id());continue;}
            states.put(p.id(),p.hidden(PieceState.Status.CAPTURED));states.put(owner.id(),HeroAbilities.afterCapture(owner,rules));
            captures.add(new Capture(owner.id(),p.id(),owner.color(),factory.createFigure(p.type(),p.position(),p.color()),p.position(),true,Capture.Cause.MINE));
            var reaction=HeroAbilities.captureReaction(factory,states,p,owner.id(),p.position(),rules);if(!reaction.result().isSuccess())return new Result(reaction.result(),before,mines,previous,List.of());
            states.putAll(reaction.updates());captures.addAll(reaction.captures());for(var update:reaction.updates().values())if(update.position()!=null)entries.add(update.id());
        }
        return new Result(MoveResult.SUCCESS,Map.copyOf(states),mines,List.copyOf(captures),List.copyOf(hits));
    }
}
