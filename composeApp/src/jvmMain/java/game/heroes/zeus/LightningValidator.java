package game.heroes.zeus;

import game.heroes.*;
import game.history.GameEvent;
import game.state.*;
import model.Position;
import model.figures.King;
import java.util.*;

/** Validates caster ownership, lifetime and the lightning journal as one transaction per cast. */
public final class LightningValidator {
    private LightningValidator() {}
    private static void require(boolean value,String message) { if(!value)throw new IllegalArgumentException(message); }
    private static int distance(Position a,Position b) { return Math.max(Math.abs(a.getRow()-b.getRow()),Math.abs(a.getColumn()-b.getColumn())); }
    public static void validate(GameSnapshot s) {
        require(s.pieces().values().stream().filter(p->p.hero()==HeroType.ZEUS).count()<=1,"Only one Zeus per match");
        require(s.pieces().values().stream().noneMatch(p->p.hero()==HeroType.ZEUS) || s.summons().enabled(),"Zeus requires saved random state");
        require(s.lightning().equals(LightningState.fromHistory(s.events(),s.heroRules().zeus()).clean(s.pieces())),"Lightning differs from history");
        if(s.mode()==GameMode.CLASSIC)require(s.lightning().equals(LightningState.empty()),"Lightning in Classic");
        for(var entry:s.lightning().charges().entrySet()) {
            var actor=s.pieces().get(entry.getKey().casterId());var target=s.pieces().get(entry.getKey().targetId());var charge=entry.getValue();
            require(actor!=null && (actor.hero()==HeroType.ZEUS || actor.hero()==HeroType.GINSENG) && target!=null && !target.isRemoved() && target.type()!=King.class && target.color()!=actor.color(),"Invalid lightning ownership");
            require(charge.count()>=1 && charge.count()<=2 && charge.expiresAfterTurn()>=s.turn().number(),"Invalid lightning count or lifetime");
        }
        LightningState state=LightningState.empty();GameEvent cast=null;Position previous=null;var hit=new HashSet<Long>();boolean reset=false;int discharges=0;
        for(var e:s.events()) {
            if(game.heroes.ginseng.SpellState.activeAbility(e)!=null) {cast=e;previous=e.from();hit.clear();reset=false;discharges=0;}
            if(e.kind()==GameEvent.Kind.END_TURN)state=state.expire(e.turn());
            var kind=switch(e.kind()){case LIGHTNING_HIT->LightningState.Kind.HIT;case LIGHTNING_RESET->LightningState.Kind.RESET;case LIGHTNING_DISCHARGE->LightningState.Kind.DISCHARGE;default->null;};
            if(kind==null)continue;
            require(cast!=null && (cast.isAbility(ZeusAbility.CHAIN_LIGHTNING) || cast.isAbility(ZeusAbility.THUNDERSTORM)) && Objects.equals(cast.actorId(),e.actorId()) && cast.turn()==e.turn() && !reset,"Lightning effect without its active cast");
            var actor=s.pieces().get(e.actorId());var target=s.pieces().get(e.targetId());
            require(actor.color()==e.color(),"Wrong lightning owner color");
            if(kind==LightningState.Kind.HIT) {
                require(cast.isAbility(ZeusAbility.CHAIN_LIGHTNING) && hit.size()<3 && target.type()!=King.class && target.color()!=actor.color() && hit.add(target.id()),"Invalid chain target");
                require(distance(previous,e.to())<=(hit.size()==1?3:2) && (hit.size()!=1 || Objects.equals(cast.targetId(),e.targetId()) && cast.to().equals(e.to())),"Invalid lightning jump");
                previous=e.to();
            } else if(kind==LightningState.Kind.DISCHARGE) {
                require(target.hero()==HeroType.ILLUSION && target.color()!=actor.color() && state.count(actor.id(),target.id())>=(cast.isAbility(ZeusAbility.THUNDERSTORM)?1:3),"Invalid illusion discharge");
                require(++discharges<=(cast.isAbility(ZeusAbility.THUNDERSTORM)?1:3),"Repeated illusion discharge");
            } else {
                var death=s.events().stream().filter(d->d.turn()==e.turn() && d.kind()==GameEvent.Kind.LIGHTNING_CAPTURE && Objects.equals(d.actorId(),e.actorId())).toList();
                require(death.size()==1 && e.targetId().equals(e.actorId()) && e.to().equals(death.getFirst().to()) && state.count(actor.id(),death.getFirst().targetId())>=(cast.isAbility(ZeusAbility.THUNDERSTORM)?1:3),"Reset without a charged lightning kill");
                reset=true;
            }
            state=state.pulse(new LightningState.Pulse(e.actorId(),e.targetId(),e.turn(),e.to(),kind),s.heroRules().zeus());
        }
        for(var e:s.events())if(e.kind()==GameEvent.Kind.LIGHTNING_CAPTURE) {
            var p=s.pieces().get(e.targetId());var actor=s.pieces().get(e.actorId());
            require(p.type()!=King.class && p.hero()!=HeroType.ILLUSION && p.color()!=actor.color() && p.status()==PieceState.Status.CAPTURED && s.events().stream().anyMatch(resetEvent->resetEvent.turn()==e.turn() && resetEvent.kind()==GameEvent.Kind.LIGHTNING_RESET && Objects.equals(resetEvent.actorId(),e.actorId())),"Invalid lightning death");
        }
        for(var e:s.events())if(e.isAbility(ZeusAbility.CHAIN_LIGHTNING) || e.isAbility(ZeusAbility.THUNDERSTORM)) {
            var effects=s.events().stream().filter(p->p.turn()==e.turn() && Objects.equals(p.actorId(),e.actorId())).toList();
            long hits=effects.stream().filter(p->p.kind()==GameEvent.Kind.LIGHTNING_HIT).count();
            long resolutions=effects.stream().filter(p->p.kind()==GameEvent.Kind.LIGHTNING_RESET || p.kind()==GameEvent.Kind.LIGHTNING_DISCHARGE).count();
            require(e.isAbility(ZeusAbility.CHAIN_LIGHTNING) ? hits>=1 && hits<=3 : hits==0 && resolutions==1,"Incomplete lightning cast");
        }
    }
}
