package game.heroes.ginseng;

import game.heroes.*;
import game.heroes.pudge.*;
import game.heroes.lumeshard.*;
import game.heroes.bountycrow.*;
import game.heroes.bolotnik.*;
import game.heroes.izba.*;
import game.moves.MoveResult;
import game.state.*;
import model.*;
import model.figures.*;
import java.util.*;

/** Reuses each original spell's geometry through a detached caster view, then restores Ginseng's identity and resources. */
public final class BorrowedEffects {
    private BorrowedEffects() {}
    private static PieceState caster(PieceState actual,Ability ability) {
        HeroType hero=switch(ability) {
            case HOOK,SWALLOW -> HeroType.PUDGE_ROOK;case LASER,TELEPORT -> HeroType.LUMESHARD_BISHOP;
            case BOUNTY,STEAL -> HeroType.BOUNTY_CROW_PAWN;case MUD,AMBUSH -> HeroType.BOLOTNIK_KNIGHT;
            case SUMMON_YAGA -> HeroType.IZBA_ROOK;default -> throw new IllegalArgumentException("Native Ginseng spell");
        };
        Class<? extends Figure> type=switch(hero) {case PUDGE_ROOK,IZBA_ROOK->Rook.class;case LUMESHARD_BISHOP->Bishop.class;case BOUNTY_CROW_PAWN->Pawn.class;default->Knight.class;};
        return new PieceState(actual.id(),type,actual.color(),actual.position(),actual.status(),hero,0,0,actual.inventory(),
                hero==HeroType.PUDGE_ROOK?actual.storedPieceId():null,actual.blindTurns(),hero==HeroType.LUMESHARD_BISHOP?actual.startingColor():null);
    }
    private static HeroRules freeRules(HeroRules rules) {
        return new HeroRules(new PudgeRules(0,0,rules.pudge().hookDiagonals(),rules.pudge().swallowDiagonals()),
                new LumeshardRules(0,rules.lumeshard().blindnessTurns(),0),
                new BountyCrowRules(0,rules.crow().bountyTurns(),0),rules.gold(),
                new BolotnikRules(0,rules.bolotnik().mudTurns(),0),new IzbaRules(0,rules.izba().lifetimeTurns()),rules.ginseng());
    }
    public static MoveResult availability(PieceState actual,Ability ability,HeroRules rules) {
        if(ability==Ability.DISPLACE || ability==Ability.COPY_SPELL)return GinsengAbilities.availability(actual,ability,rules.ginseng(),true);
        return HeroAbilities.availability(caster(actual,ability),ability,freeRules(rules));
    }
    public static HeroEffect preview(Field field,PieceState actual,PieceState target,Position square,Ability ability,HeroRules rules) {
        HeroEffect effect=HeroAbilities.preview(field,caster(actual,ability),target,square,ability,freeRules(rules));
        if(!effect.result().isSuccess())return effect;
        var updates=new HashMap<>(effect.updates());PieceState view=updates.get(actual.id());
        if(view!=null)updates.put(actual.id(),new PieceState(actual.id(),actual.type(),actual.color(),view.position(),view.status(),actual.hero(),
                actual.cooldown(),actual.ultimateCharge(),actual.inventory(),ability==Ability.SWALLOW?view.storedPieceId():actual.storedPieceId(),view.blindTurns(),actual.startingColor()));
        return new HeroEffect(effect.result(),updates,effect.captures());
    }
}
