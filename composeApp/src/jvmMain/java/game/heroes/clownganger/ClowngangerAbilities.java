package game.heroes.clownganger;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
import game.heroes.*;
import game.moves.MoveResult;
import game.state.PieceState;
import model.*;
import model.figures.Empty;
import java.util.*;
public final class ClowngangerAbilities {
    private ClowngangerAbilities(){}
    public static List<Abilities> abilities() {
        return ClowngangerAbility.getAbilities();
    }
    public static MoveResult availability(PieceState actor,Abilities a,ClowngangerRules rules){
        if(actor==null || actor.position()==null)return MovementRejection.EMPTY_SOURCE;
        if(actor.hero()!=HeroType.CLOWNGANGER || !abilities().contains(a))return AbilityRejection.NO_ABILITY;
        if(a==ClowngangerAbility.BOX && actor.cooldown()>0)return AbilityRejection.ABILITY_COOLDOWN;
        if(a==ClowngangerAbility.ILLUSION && actor.ultimateCharge()<rules.ultimateCaptures())return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }
    public static HeroEffect preview(Field field,PieceState actor,PieceState target,Position square,Abilities a,long turn,ClowngangerRules rules,boolean borrowed){
        var updates=new HashMap<Long,PieceState>();
        if(a==ClowngangerAbility.BOX){
            if(target==null || target.color()!=actor.color())return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            if(distance(actor.position(),square)>3)return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
            updates.put(actor.id(),actor.withAbilityState(borrowed?actor.cooldown():rules.boxCooldown(),actor.ultimateCharge(),actor.storedPieceId()));
            var p=updates.getOrDefault(target.id(),target);
            updates.put(p.id(),p.withBox(Math.addExact(turn,2L*rules.boxTurns())));
        }else if(a==ClowngangerAbility.ILLUSION){
            if(distance(actor.position(),square)!=1 || !(field.getFigureByPosition(square) instanceof Empty))return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            updates.put(actor.id(),actor.withAbilityState(actor.cooldown(),borrowed?actor.ultimateCharge():actor.ultimateCharge()-rules.ultimateCaptures(),actor.storedPieceId()));
        }else return HeroEffect.rejected(AbilityRejection.NO_ABILITY);
        return HeroEffect.success(updates);
    }
    private static int distance(Position a,Position b){return Math.max(Math.abs(a.getRow()-b.getRow()),Math.abs(a.getColumn()-b.getColumn()));}
}
