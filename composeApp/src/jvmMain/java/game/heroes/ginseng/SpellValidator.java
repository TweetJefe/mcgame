package game.heroes.ginseng;

import game.heroes.*;
import game.history.GameEvent;
import game.state.*;

public final class SpellValidator {
    private SpellValidator() {
    }

    public static void validate(GameSnapshot s) {
        if (s.spells() == null || !s.spells().equals(SpellState.fromHistory(s.events())))
            throw new IllegalArgumentException("Spell memory differs from action history");
        HeroRegistry heroes = HeroRegistry.fromRules(s.heroRules());
        for (GameEvent event : s.events()) {
            Abilities ability = SpellState.activeAbility(event);
            if (ability == null) continue;
            heroes.ownerOf(ability);
            PieceState actor = s.pieces().get(event.actorId());
            if (event.borrowed()) {
                if (actor.hero() != HeroType.GINSENG)
                    throw new IllegalArgumentException("Borrowed cast by non-Ginseng");
            } else if (!heroes.get(actor.hero()).abilities().contains(ability))
                throw new IllegalArgumentException("Hero cannot cast this native ability");
            if (ability == GinsengAbility.COPY_SPELL) {
                PieceState source = s.pieces().get(event.targetId());
                if (source == null || source.hero() == HeroType.NONE || source.hero() == HeroType.BABA_YAGA)
                    throw new IllegalArgumentException("Invalid copy source");
            }
        }
        for (var entry : s.spells().borrowed().entrySet()) {
            PieceState p = s.pieces().get(entry.getKey());
            if (p == null || p.hero() != HeroType.GINSENG || !s.pieces().containsKey(entry.getValue().sourceId()))
                throw new IllegalArgumentException("Orphan copied ability");
        }
    }
}
