package game.heroes.zeus;
import game.heroes.*;
import game.moves.*;
import game.state.PieceState;
import game.summoning.SummonState;
import model.*;
import model.figures.King;
import java.util.*;
public final class ZeusAbilities{
    private ZeusAbilities(){}
    public static List<Abilities> abilities() {
        return ZeusAbility.getAbilities();
    }
    public static MoveResult availability(PieceState actor,Abilities ability,ZeusRules rules){
        if(actor==null || actor.position()==null)return MovementRejection.EMPTY_SOURCE;
        if(actor.hero()!=HeroType.ZEUS || !abilities().contains(ability))return AbilityRejection.NO_ABILITY;
        if(ability==ZeusAbility.CHAIN_LIGHTNING && actor.cooldown()>0)return AbilityRejection.ABILITY_COOLDOWN;
        if(ability==ZeusAbility.THUNDERSTORM && actor.ultimateCharge()<rules.ultimateCaptures())return AbilityRejection.ULTIMATE_NOT_READY;
        return Success.SUCCESS;
    }
    public static List<PieceState> charged(Map<Long,PieceState> pieces,LightningState lightning,PieceState actor){return pieces.values().stream().filter(p->eligible(p,actor) && lightning.count(actor.id(),p.id())>0).sorted(Comparator.comparingLong(PieceState::id)).toList();}
    public record Result(HeroEffect effect,LightningState lightning,SummonState summons,List<Long> illusionHits){}
    private static boolean eligible(PieceState p,PieceState actor){return p.position()!=null && p.color()!=actor.color() && p.type()!=King.class && !p.isBoxed() && !p.isPotted();}
    public static Result cast(Field factory,Map<Long,PieceState> pieces,PieceState actor,PieceState first,Abilities ability,LightningState lightning,SummonState summons,long turn,HeroRules rules,boolean borrowed){
        var states=new HashMap<>(pieces);var updates=new HashMap<Long,PieceState>();var captures=new ArrayList<Capture>();var illusions=new ArrayList<Long>();
        if(ability==ZeusAbility.CHAIN_LIGHTNING){
            if(first==null || !eligible(first,actor))return rejected(AbilityRejection.INVALID_ABILITY_TARGET,lightning,summons);
            if(distance(actor.position(),first.position())>3)return rejected(AbilityRejection.ABILITY_OUT_OF_RANGE,lightning,summons);
        }else if(ability!=ZeusAbility.THUNDERSTORM)return rejected(AbilityRejection.NO_ABILITY,lightning,summons);
        var nextActor=actor.withAbilityState(borrowed?actor.cooldown():ability==ZeusAbility.CHAIN_LIGHTNING?rules.zeus().lightningCooldown():actor.cooldown(),borrowed || ability!=ZeusAbility.THUNDERSTORM?actor.ultimateCharge():actor.ultimateCharge()-rules.zeus().ultimateCaptures(),actor.storedPieceId());states.put(actor.id(),nextActor);updates.put(actor.id(),nextActor);
        PieceState target=first;
        if(ability==ZeusAbility.THUNDERSTORM){
            var candidates=charged(states,lightning,actor);if(candidates.isEmpty())return rejected(AbilityRejection.NO_CHARGED_TARGETS,lightning,summons);
            int pick=0;if(candidates.size()>1){var choice=summons.choose(candidates.size());pick=choice.index();summons=new SummonState(true,choice.nextState(),summons.records());}target=candidates.get(pick);
        }
        var seen=new HashSet<Long>();
        for(int jump=0;jump<(ability==ZeusAbility.CHAIN_LIGHTNING?3:1);jump++){
            seen.add(target.id());Position square=target.position();
            if(ability==ZeusAbility.CHAIN_LIGHTNING)lightning=lightning.pulse(new LightningState.Pulse(actor.id(),target.id(),turn,square,LightningState.Kind.HIT),rules.zeus());
            if(ability==ZeusAbility.THUNDERSTORM || lightning.count(actor.id(),target.id())>=3){
                if(target.hero()==HeroType.ILLUSION){
                    illusions.add(target.id());lightning=lightning.pulse(new LightningState.Pulse(actor.id(),target.id(),turn,square,LightningState.Kind.DISCHARGE),rules.zeus());
                }else{
                    var dead=target.hidden(PieceState.Status.CAPTURED);states.put(dead.id(),dead);updates.put(dead.id(),dead);
                    var charged=HeroAbilities.afterCapture(states.get(actor.id()),rules);states.put(actor.id(),charged);updates.put(actor.id(),charged);
                    captures.add(new Capture(actor.id(),dead.id(),actor.color(),factory.createFigure(target.type(),square,target.color()),square,true,Capture.Cause.LIGHTNING));
                    lightning=lightning.pulse(new LightningState.Pulse(actor.id(),actor.id(),turn,square,LightningState.Kind.RESET),rules.zeus());
                    var reaction=HeroAbilities.captureReaction(factory,states,target,actor.id(),square,rules);if(!reaction.result().isSuccess())return rejected(reaction.result(),lightning,summons);states.putAll(reaction.updates());updates.putAll(reaction.updates());captures.addAll(reaction.captures());
                    break; // The first real lightning kill ends the chain.
                }
            }
            if(ability==ZeusAbility.THUNDERSTORM || jump==2)break;
            Position previous=square;var candidates=states.values().stream().filter(p->eligible(p,actor) && !seen.contains(p.id()) && distance(previous,p.position())<=2).toList();
            if(candidates.isEmpty())break;int nearest=candidates.stream().mapToInt(p->distance(previous,p.position())).min().orElseThrow();
            var tied=candidates.stream().filter(p->distance(previous,p.position())==nearest).sorted(Comparator.comparingLong(PieceState::id)).toList();int pick=0;
            if(tied.size()>1){var choice=summons.choose(tied.size());pick=choice.index();summons=new SummonState(true,choice.nextState(),summons.records());}target=tied.get(pick);
        }
        return new Result(new HeroEffect(Success.SUCCESS,updates,captures),lightning.clean(states),summons,List.copyOf(illusions));
    }
    private static int distance(Position a,Position b){return Math.max(Math.abs(a.getRow()-b.getRow()),Math.abs(a.getColumn()-b.getColumn()));}
    private static Result rejected(MoveResult result,LightningState state,SummonState summons){return new Result(HeroEffect.rejected(result),state,summons,List.of());}
}
