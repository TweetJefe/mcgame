package game.heroes.chronomlin;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
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
    public static List<Abilities> abilities() {
        return ChronomlinAbility.getAbilities();
    }
    public static Class<? extends Figure> nextType(Class<? extends Figure> type){
        if(type==Pawn.class)return Bishop.class;
        if(type==Bishop.class)return Knight.class;
        if(type==Knight.class)return Rook.class;
        return null;
    }
    public static MoveResult availability(PieceState actor,Abilities ability,ChronomlinRules rules,boolean borrowed){
        if(ability==null)return AbilityRejection.INVALID_ABILITY;
        if(actor==null || actor.position()==null)return MovementRejection.EMPTY_SOURCE;
        if(actor.hero()!=(borrowed?HeroType.GINSENG:HeroType.CHRONOMLIN) || !abilities().contains(ability))return AbilityRejection.NO_ABILITY;
        if(ability==ChronomlinAbility.EVOLVE && nextType(actor.type())==null)return AbilityRejection.MAX_EVOLUTION;
        if(!borrowed && ability==ChronomlinAbility.GRAPPLE && actor.cooldown()>0)return AbilityRejection.ABILITY_COOLDOWN;
        if(!borrowed && ability==ChronomlinAbility.EVOLVE && actor.ultimateCharge()<rules.ultimateCaptures())return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }
    public static HeroEffect preview(Field field,PieceState actor,PieceState target,Abilities ability,ChronomlinRules rules,boolean borrowed){
        var ready=availability(actor,ability,rules,borrowed);
        if(!ready.isSuccess())return HeroEffect.rejected(ready);
        if(ability==ChronomlinAbility.EVOLVE){
            var evolved=actor.withAbilityState(actor.cooldown(),borrowed?actor.ultimateCharge():actor.ultimateCharge()-rules.ultimateCaptures(),actor.storedPieceId()).placed(actor.position(),nextType(actor.type()));
            return HeroEffect.success(Map.of(actor.id(),evolved));
        }
        if(target==null || target.position()==null || target.id()==actor.id())return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        int dr=target.position().getRow()-actor.position().getRow(),dc=target.position().getColumn()-actor.position().getColumn();
        if(dr!=0 && dc!=0 && Math.abs(dr)!=Math.abs(dc))return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        int distance=Math.max(Math.abs(dr),Math.abs(dc));
        if(distance<=1)return HeroEffect.rejected(AbilityRejection.NO_GRAPPLE_MOVEMENT);
        int rowStep=Integer.signum(dr),colStep=Integer.signum(dc);
        for(int step=1;step<distance;step++){
            var square=new Position(actor.position().getRow()+rowStep*step,actor.position().getColumn()+colStep*step);
            if(!(field.getFigureByPosition(square) instanceof Empty))return HeroEffect.rejected(AbilityRejection.ABILITY_BLOCKED);
        }
        var landing=new Position(target.position().getRow()-rowStep,target.position().getColumn()-colStep);
        var moved=actor.withAbilityState(borrowed?actor.cooldown():rules.grappleCooldown(),actor.ultimateCharge(),actor.storedPieceId()).placed(landing,actor.type());
        return HeroEffect.success(Map.of(actor.id(),moved));
    }
}
