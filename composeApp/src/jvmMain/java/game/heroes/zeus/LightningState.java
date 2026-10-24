package game.heroes.zeus;

import game.heroes.kikimora.KikimoraAbility;
import game.history.GameEvent;
import game.state.PieceState;
import model.Position;
import java.util.*;

/** Every caster owns an independent pool, including a Ginseng using a copied spell. */
public record LightningState(Map<Key,Charge> charges,List<Pulse> pulses){
    public record Key(long casterId,long targetId){}
    public record Charge(int count,long expiresAfterTurn){}
    public enum Kind{HIT,RESET,DISCHARGE}
    public record Pulse(long casterId,long targetId,long turn,Position square,Kind kind){}
    public LightningState{charges=Map.copyOf(charges);pulses=List.copyOf(pulses);}
    public static LightningState empty(){return new LightningState(Map.of(),List.of());}
    public int count(long caster,long target){var c=charges.get(new Key(caster,target));return c==null?0:c.count();}
    public LightningState pulse(Pulse pulse,ZeusRules rules){
        var next=new HashMap<>(charges);var key=new Key(pulse.casterId(),pulse.targetId());
        switch(pulse.kind()){
            case HIT -> next.put(key,new Charge(count(key.casterId(),key.targetId())+1,Math.addExact(pulse.turn(),2L*rules.chargeTurns())));
            case RESET -> next.keySet().removeIf(k->k.casterId()==pulse.casterId());
            case DISCHARGE -> next.remove(key);
        }
        var log=new ArrayList<>(pulses);log.add(pulse);return new LightningState(next,log);
    }
    public LightningState expire(long turn){var next=new HashMap<>(charges);next.values().removeIf(c->c.expiresAfterTurn()<=turn);return new LightningState(next,pulses);}
    public LightningState clean(Map<Long,PieceState> pieces){var next=new HashMap<>(charges);next.keySet().removeIf(k->pieces.get(k.targetId())==null || pieces.get(k.targetId()).isRemoved() || pieces.get(k.targetId()).isPotted());return new LightningState(next,pulses);}
    public static LightningState fromHistory(List<GameEvent> events,ZeusRules rules){
        LightningState state=empty();
        for(var e:events){
            Kind kind=switch(e.kind()){case LIGHTNING_HIT->Kind.HIT;case LIGHTNING_RESET->Kind.RESET;case LIGHTNING_DISCHARGE->Kind.DISCHARGE;default->null;};
            if(kind!=null)state=state.pulse(new Pulse(e.actorId(),e.targetId(),e.turn(),e.to(),kind),rules);
            if(e.kind()==GameEvent.Kind.END_TURN)state=state.expire(e.turn());
            if(e.targetId()!=null && (Set.of(GameEvent.Kind.MOVE, GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE, GameEvent.Kind.ARES_REVENGE, GameEvent.Kind.MINE_CAPTURE, GameEvent.Kind.BLAST_CAPTURE, GameEvent.Kind.BLAST_DEATH, GameEvent.Kind.LIGHTNING_CAPTURE, GameEvent.Kind.REPLACE).contains(e.kind()) || e.isAbility(KikimoraAbility.CLAY_POT))){
                var next=new HashMap<>(state.charges());next.keySet().removeIf(k->k.targetId()==e.targetId());state=new LightningState(next,state.pulses());
            }
        }
        return state;
    }
}
