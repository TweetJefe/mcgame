package game.core;
import game.moves.MovementRejection;
import game.moves.Success;
import game.heroes.*;
import game.heroes.moleminer.*;
import game.moves.*;
import game.state.PieceState;
import game.summoning.*;
import game.terrain.*;
import model.*;
import java.util.*;

/** Resolves new entries and death reactions until mines, mud and summons settle. */
final class EffectResolver {
    private EffectResolver(){}
    record Result(MoveResult result,Map<Long,PieceState> pieces,TerrainState terrain,SummonState summons,MineState mines,List<Capture> captures,List<Long> illusionHits){}
    static Result resolve(Field factory,Map<Long,PieceState> before,Map<Long,PieceState> prepared,TerrainState terrain,SummonState summons,MineState mines,List<Capture> captures,List<Long> immediate,long turn,FigureColor side,HeroRules rules,Long bomber,Position landing){
        Map<Long,PieceState> states=prepared,entryBefore=before;var hits=new ArrayList<Long>();var handled=new HashSet<Long>();boolean blastPending=bomber!=null;
        for(long step=0;step<states.size()*2L+64;step++){
            var old=states;var oldMines=mines;var oldTerrain=terrain;var oldSummons=summons;
            var mined=MineResolver.resolve(factory,entryBefore,states,mines,captures,turn,rules);
            if(!mined.result().isSuccess())return new Result(mined.result(),before,terrain,summons,mines,captures,List.of());
            states=mined.pieces();mines=mined.mines();captures=mined.captures();hits.addAll(mined.illusionHits());
            var muddied=TerrainResolver.resolve(factory,entryBefore,states,terrain,captures,step==0?immediate:List.of(),rules);
            if(!muddied.result().isSuccess())return new Result(muddied.result(),before,terrain,summons,mines,captures,List.of());
            states=muddied.pieces();terrain=muddied.terrain();captures=muddied.captures();hits.addAll(muddied.illusionHits());
            if(blastPending){
                var blast=MoleminerAbilities.explode(factory,states,bomber,landing,mines,summons,captures,turn,rules);
                states=blast.pieces();mines=blast.mines();summons=blast.summons();captures=blast.captures();hits.addAll(blast.illusionHits());blastPending=false;
            }
            var summoned=SummonResolver.resolve(factory,states,summons,terrain,captures,turn,side,rules,handled);
            if(!summoned.result().isSuccess())return new Result(summoned.result(),before,terrain,summons,mines,captures,List.of());
            states=summoned.pieces();terrain=summoned.terrain();summons=summoned.summons();captures=summoned.captures();hits.addAll(summoned.illusionHits());
            if(states.equals(old) && mines.equals(oldMines) && terrain.equals(oldTerrain) && summons.equals(oldSummons))return new Result(Success.SUCCESS,states,terrain,summons,mines,captures,List.copyOf(hits));
            entryBefore=old;
        }
        return new Result(MovementRejection.INVALID_BOARD,before,terrain,summons,mines,captures,List.of());
    }
}
