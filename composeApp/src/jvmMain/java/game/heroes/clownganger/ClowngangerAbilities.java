package game.heroes.clownganger;
import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.*;
import model.figures.Empty;
import java.util.*;
public final class ClowngangerAbilities {
    private ClowngangerAbilities(){}
    public static List<Ability> abilities(){return List.of(Ability.BOX,Ability.ILLUSION);}
    public static MoveResult availability(PieceState actor,Ability a,ClowngangerRules rules){
        if(actor==null || actor.position()==null)return MoveResult.EMPTY_SOURCE;
        if(actor.hero()!=HeroType.CLOWNGANGER_KNIGHT || !abilities().contains(a))return MoveResult.NO_ABILITY;
        if(a==Ability.BOX && actor.cooldown()>0)return MoveResult.ABILITY_COOLDOWN;
        if(a==Ability.ILLUSION && actor.ultimateCharge()<rules.ultimateCaptures())return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }
    public static HeroEffect preview(Field field,PieceState actor,PieceState target,Position square,Ability a,long turn,ClowngangerRules rules,boolean borrowed){
        var updates=new HashMap<Long,PieceState>();
        if(a==Ability.BOX){
            if(target==null || target.color()!=actor.color())return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
            if(distance(actor.position(),square)>3)return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
            updates.put(actor.id(),actor.withAbilityState(borrowed?actor.cooldown():rules.boxCooldown(),actor.ultimateCharge(),actor.storedPieceId()));
            var p=updates.getOrDefault(target.id(),target);
            updates.put(p.id(),p.withBox(Math.addExact(turn,2L*rules.boxTurns())));
        }else if(a==Ability.ILLUSION){
            if(distance(actor.position(),square)!=1 || !(field.getFigureByPosition(square) instanceof Empty))return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
            updates.put(actor.id(),actor.withAbilityState(actor.cooldown(),borrowed?actor.ultimateCharge():actor.ultimateCharge()-rules.ultimateCaptures(),actor.storedPieceId()));
        }else return HeroEffect.rejected(MoveResult.NO_ABILITY);
        return HeroEffect.success(updates);
    }
    private static int distance(Position a,Position b){return Math.max(Math.abs(a.getRow()-b.getRow()),Math.abs(a.getColumn()-b.getColumn()));}
}
