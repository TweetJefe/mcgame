package game.heroes.kikimora;
import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.*;
import model.figures.King;
import java.util.*;

public final class KikimoraAbilities{
    private KikimoraAbilities(){}
    public static List<Ability> abilities(){return List.of(Ability.YARN,Ability.CLAY_POT);}
    public static MoveResult availability(PieceState actor,Ability ability,KikimoraRules rules,boolean borrowed){
        if(ability==null)return MoveResult.INVALID_ABILITY;
        if(actor==null || actor.position()==null)return MoveResult.EMPTY_SOURCE;
        if(actor.hero()!=(borrowed?HeroType.GINSENG_BISHOP:HeroType.KIKIMORA_BISHOP) || !abilities().contains(ability))return MoveResult.NO_ABILITY;
        if(actor.isPotted())return MoveResult.POTTED;
        if(!borrowed && ability==Ability.YARN && actor.cooldown()>0)return MoveResult.ABILITY_COOLDOWN;
        if(!borrowed && ability==Ability.CLAY_POT && actor.ultimateCharge()<rules.ultimateCaptures())return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }
    /** Exact lattice ray, including slopes such as (1,2), follows the flight from caster to target. */
    public static List<PieceState> yarnTargets(Map<Long,PieceState> pieces,PieceState actor,PieceState first){
        var result=new ArrayList<PieceState>();result.add(first);
        int dr=first.position().getRow()-actor.position().getRow(),dc=first.position().getColumn()-actor.position().getColumn();
        int divisor=gcd(Math.abs(dr),Math.abs(dc));int sr=dr/divisor,sc=dc/divisor;
        for(int n=1;n*Math.max(Math.abs(sr),Math.abs(sc))<=2;n++){
            var square=new Position(first.position().getRow()+sr*n,first.position().getColumn()+sc*n);
            if(square.getRow()<0 || square.getRow()>7 || square.getColumn()<0 || square.getColumn()>7)break;
            var next=pieces.values().stream().filter(p->square.equals(p.position())).findFirst().orElse(null);
            if(next!=null){if(next.type()!=King.class && !next.isBoxed() && !next.isPotted())result.add(next);break;}
        }
        return List.copyOf(result);
    }
    public static long stunDeadline(long turn,FigureColor caster,FigureColor owner,int turns){return Math.addExact(turn,2L*turns-(caster==owner?0:1));}
    public static HeroEffect preview(Map<Long,PieceState> pieces,PieceState actor,PieceState target,Ability ability,long turn,KikimoraRules rules,boolean borrowed){
        var ready=availability(actor,ability,rules,borrowed);if(!ready.isSuccess())return HeroEffect.rejected(ready);
        if(target==null || target.position()==null || target.id()==actor.id())return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if(target.type()==King.class)return HeroEffect.rejected(MoveResult.KING_IMMUNE);
        if(target.isPotted())return HeroEffect.rejected(MoveResult.POTTED);
        if(target.isBoxed())return HeroEffect.rejected(MoveResult.BOXED);
        int distance=Math.max(Math.abs(target.position().getRow()-actor.position().getRow()),Math.abs(target.position().getColumn()-actor.position().getColumn()));
        if(distance>(ability==Ability.YARN?2:1))return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
        var updates=new HashMap<Long,PieceState>();
        if(ability==Ability.CLAY_POT){
            updates.put(actor.id(),actor.withAbilityState(actor.cooldown(),borrowed?actor.ultimateCharge():actor.ultimateCharge()-rules.ultimateCaptures(),actor.storedPieceId()));
            updates.put(target.id(),target.asPot());
        }else{
            if(target.color()==actor.color())return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
            var targets=yarnTargets(pieces,actor,target);int duration=targets.size()==2?2:1;
            for(var p:targets)updates.put(p.id(),p.withStun(Math.max(p.stunnedThroughTurn(),stunDeadline(turn,actor.color(),p.color(),duration))));
            updates.put(actor.id(),actor.withAbilityState(borrowed?actor.cooldown():rules.yarnCooldown(),actor.ultimateCharge(),actor.storedPieceId()));
        }
        return HeroEffect.success(updates);
    }
    private static int gcd(int a,int b){while(b!=0){int t=a%b;a=b;b=t;}return a;}
}
