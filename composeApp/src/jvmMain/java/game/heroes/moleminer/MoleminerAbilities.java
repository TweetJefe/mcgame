package game.heroes.moleminer;
import game.heroes.*;
import game.moves.*;
import game.state.PieceState;
import game.summoning.SummonState;
import model.*;
import model.figures.King;
import java.util.*;
public final class MoleminerAbilities{
    private MoleminerAbilities(){}
    public static List<Abilities> abilities() {
        return MoleminerAbility.getAbilities();
    }
    public static MoveResult availability(PieceState actor,Abilities a,MoleminerRules rules){
        if(actor==null || actor.position()==null)return MovementRejection.EMPTY_SOURCE;
        if(actor.hero()!=HeroType.MOLEMINER || !abilities().contains(a))return AbilityRejection.NO_ABILITY;
        if(a==MoleminerAbility.PLANT_MINE && actor.cooldown()>0)return AbilityRejection.ABILITY_COOLDOWN;
        if(a==MoleminerAbility.DETONATE && actor.ultimateCharge()<rules.ultimateCaptures())return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }
    public static HeroEffect preview(PieceState actor,Position square,Abilities a,MoleminerRules rules,boolean borrowed){
        if(a==MoleminerAbility.PLANT_MINE)return HeroEffect.success(Map.of(actor.id(),actor.withAbilityState(borrowed?actor.cooldown():rules.mineCooldown(),actor.ultimateCharge(),actor.storedPieceId())));
        if(a!=MoleminerAbility.DETONATE)return HeroEffect.rejected(AbilityRejection.NO_ABILITY);
        if(Math.max(Math.abs(square.getRow()-actor.position().getRow()),Math.abs(square.getColumn()-actor.position().getColumn()))>3)return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        return HeroEffect.success(Map.of(actor.id(),actor.withAbilityState(actor.cooldown(),borrowed?actor.ultimateCharge():actor.ultimateCharge()-rules.ultimateCaptures(),actor.storedPieceId()).placed(square,actor.type())));
    }
    public static List<Position> corners(Position landing){var result=new ArrayList<Position>();for(int r=landing.getRow()-1;r<=landing.getRow();r++)for(int c=landing.getColumn()-1;c<=landing.getColumn();c++)if(r>=0 && r<7 && c>=0 && c<7)result.add(new Position(r,c));return List.copyOf(result);}
    public record Explosion(Map<Long,PieceState> pieces,MineState mines,SummonState summons,List<Capture> captures,List<Long> illusionHits){}
    public static Explosion explode(Field factory,Map<Long,PieceState> prepared,long actorId,Position landing,MineState mines,SummonState summons,List<Capture> previous,long turn,HeroRules rules){
        var corners=corners(landing);var choice=summons.choose(corners.size());var blast=new MineState.Blast(actorId,turn,landing,corners.get(choice.index()));
        var states=new HashMap<>(prepared);var captures=new ArrayList<>(previous);var hits=new ArrayList<Long>();var actor=states.get(actorId);
        var victims=prepared.values().stream().filter(p->p.position()!=null && blast.contains(p.position()) && p.type()!=King.class && !p.isBoxed() && !p.isPotted()).sorted(Comparator.comparingLong(PieceState::id)).toList();
        for(var p:victims){if(p.hero()==HeroType.ILLUSION){if(p.color()!=actor.color())hits.add(p.id());continue;}states.put(p.id(),p.hidden(p.color()==actor.color()?PieceState.Status.DESTROYED:PieceState.Status.CAPTURED));}
        // All blast victims die together. Reactions run only after their removal.
        for(var p:victims)if(p.hero()!=HeroType.ILLUSION){
            captures.add(new Capture(actorId,p.id(),actor.color(),factory.createFigure(p.type(),p.position(),p.color()),p.position(),true,Capture.Cause.BLAST));
            if(p.color()!=actor.color())states.put(actorId,HeroAbilities.afterCapture(states.get(actorId),rules));
        }
        for(var p:victims)if(p.hero()!=HeroType.ILLUSION){var reaction=HeroAbilities.captureReaction(factory,states,p,actorId,p.position(),rules);if(!reaction.result().isSuccess())throw new IllegalArgumentException("Invalid explosion reaction");states.putAll(reaction.updates());captures.addAll(reaction.captures());}
        return new Explosion(Map.copyOf(states),mines.blast(blast),new SummonState(true,choice.nextState(),summons.records()),List.copyOf(captures),List.copyOf(hits));
    }
}
