package game.heroes.ginseng;

import game.heroes.*;
import game.history.GameEvent;
import game.state.*;

public final class SpellValidator {
    private SpellValidator() {}
    public static void validate(GameSnapshot s) {
        if(s.spells()==null || !s.spells().equals(SpellState.fromHistory(s.events())))throw new IllegalArgumentException("Spell memory differs from action history");
        for(GameEvent event:s.events()) {
            Ability ability=SpellState.activeAbility(event.kind());if(ability==null)continue;
            PieceState actor=s.pieces().get(event.actorId());
            if(event.borrowed()) {
                if(actor.hero()!=HeroType.GINSENG_BISHOP)throw new IllegalArgumentException("Borrowed cast by non-Ginseng");
            } else if(!HeroAbilities.abilities(actor.hero()).contains(ability))throw new IllegalArgumentException("Hero cannot cast this native ability");
            if(ability==Ability.COPY_SPELL) {
                PieceState source=s.pieces().get(event.targetId());
                if(source==null || source.hero()==HeroType.NONE || source.hero()==HeroType.BABA_YAGA_PAWN)
                    throw new IllegalArgumentException("Invalid copy source");
            }
        }
        for(var entry:s.spells().borrowed().entrySet()) {
            PieceState p=s.pieces().get(entry.getKey());
            if(p==null || p.hero()!=HeroType.GINSENG_BISHOP || !s.pieces().containsKey(entry.getValue().sourceId()))
                throw new IllegalArgumentException("Orphan copied ability");
        }
    }
}
