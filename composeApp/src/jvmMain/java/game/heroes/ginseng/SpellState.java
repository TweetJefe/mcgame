package game.heroes.ginseng;

import game.heroes.Ability;
import game.history.GameEvent;
import java.util.*;

/** Last successful active casts and one consumable copy per Ginseng, replayable from the action journal. */
public record SpellState(Map<Long,Ability> lastUsed,Map<Long,BorrowedAbility> borrowed) {
    public SpellState { lastUsed=Map.copyOf(lastUsed);borrowed=Map.copyOf(borrowed); }
    public static SpellState empty() { return new SpellState(Map.of(),Map.of()); }
    public SpellState cast(long actor,Ability ability,boolean useBorrowed,long turn,Long source,Ability copied) {
        var last=new HashMap<>(lastUsed);var copies=new HashMap<>(borrowed);
        if(useBorrowed) {
            BorrowedAbility stored=copies.remove(actor);
            if(stored==null || stored.ability()!=ability || !stored.isReady(turn))throw new IllegalArgumentException("Borrowed cast without a ready copy");
        }
        if(ability==Ability.COPY_SPELL) {
            if(source==null || last.get(source)!=copied)throw new IllegalArgumentException("Copy differs from target's latest active cast");
            if(copied==null)copies.remove(actor);else copies.put(actor,new BorrowedAbility(copied,source,turn));
        } else if(copied!=null)throw new IllegalArgumentException("Unexpected copied ability");
        last.put(actor,ability);return new SpellState(last,copies);
    }
    public static SpellState fromHistory(List<GameEvent> events) {
        SpellState state=empty();
        for(GameEvent event:events) {
            Ability ability=activeAbility(event.kind());
            if(ability!=null) {
                if(event.actorId()==null)throw new IllegalArgumentException("Active cast without actor");
                state=state.cast(event.actorId(),ability,event.borrowed(),event.turn(),event.targetId(),event.copiedAbility());
            } else if(event.borrowed() || event.copiedAbility()!=null)throw new IllegalArgumentException("Spell metadata on non-active event");
        }
        return state;
    }
    public static Ability activeAbility(GameEvent.Kind kind) {
        if(kind==null)return null;
        try { return Ability.valueOf(kind.name()); }catch(IllegalArgumentException ex){return null;}
    }
}
