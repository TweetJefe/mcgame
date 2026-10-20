package game.heroes.chronomlin;

import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.*;
import model.figures.*;
import java.util.List;
import java.util.Map;

/** Movement and evolution preserve the caster's identity, inventory and lasting effects. */
public final class ChronomlinAbilities {
    private ChronomlinAbilities(){}
    public static List<Ability> abilities(){return List.of(Ability.GRAPPLE,Ability.EVOLVE);}
    public static Class<? extends Figure> nextType(Class<? extends Figure> type){
        if(type==Pawn.class)return Bishop.class;
        if(type==Bishop.class)return Knight.class;
        if(type==Knight.class)return Rook.class;
        return null;
    }
    public static MoveResult availability(PieceState actor,Ability ability,ChronomlinRules rules,boolean borrowed){
        if(ability==null)return MoveResult.INVALID_ABILITY;
        if(actor==null || actor.position()==null)return MoveResult.EMPTY_SOURCE;
        if(actor.hero()!=(borrowed?HeroType.GINSENG_BISHOP:HeroType.CHRONOMLIN_PAWN) || !abilities().contains(ability))return MoveResult.NO_ABILITY;
        if(ability==Ability.EVOLVE && nextType(actor.type())==null)return MoveResult.MAX_EVOLUTION;
        if(!borrowed && ability==Ability.GRAPPLE && actor.cooldown()>0)return MoveResult.ABILITY_COOLDOWN;
        if(!borrowed && ability==Ability.EVOLVE && actor.ultimateCharge()<rules.ultimateCaptures())return MoveResult.ULTIMATE_NOT_READY;
        return MoveResult.SUCCESS;
    }
    public static HeroEffect preview(Field field,PieceState actor,PieceState target,Ability ability,ChronomlinRules rules,boolean borrowed){
        var ready=availability(actor,ability,rules,borrowed);
        if(!ready.isSuccess())return HeroEffect.rejected(ready);
        if(ability==Ability.EVOLVE){
            var evolved=actor.withAbilityState(actor.cooldown(),borrowed?actor.ultimateCharge():actor.ultimateCharge()-rules.ultimateCaptures(),actor.storedPieceId()).placed(actor.position(),nextType(actor.type()));
            return HeroEffect.success(Map.of(actor.id(),evolved));
        }
        if(target==null || target.position()==null || target.id()==actor.id())return HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
        int dr=target.position().getRow()-actor.position().getRow(),dc=target.position().getColumn()-actor.position().getColumn();
        if(dr!=0 && dc!=0 && Math.abs(dr)!=Math.abs(dc))return HeroEffect.rejected(MoveResult.ABILITY_OUT_OF_RANGE);
        int distance=Math.max(Math.abs(dr),Math.abs(dc));
        if(distance<=1)return HeroEffect.rejected(MoveResult.NO_GRAPPLE_MOVEMENT);
        int rowStep=Integer.signum(dr),colStep=Integer.signum(dc);
        for(int step=1;step<distance;step++){
            var square=new Position(actor.position().getRow()+rowStep*step,actor.position().getColumn()+colStep*step);
            if(!(field.getFigureByPosition(square) instanceof Empty))return HeroEffect.rejected(MoveResult.ABILITY_BLOCKED);
        }
        var landing=new Position(target.position().getRow()-rowStep,target.position().getColumn()-colStep);
        var moved=actor.withAbilityState(borrowed?actor.cooldown():rules.grappleCooldown(),actor.ultimateCharge(),actor.storedPieceId()).placed(landing,actor.type());
        return HeroEffect.success(Map.of(actor.id(),moved));
    }
}
