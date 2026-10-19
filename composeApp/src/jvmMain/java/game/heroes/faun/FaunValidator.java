package game.heroes.faun;
import game.heroes.*;
import game.state.*;
public final class FaunValidator {
    private FaunValidator(){}
    public static void validate(GameSnapshot s) {
        if(s.faun()==null || !s.faun().equals(FaunState.fromHistory(s.events(),s.heroRules().faun())))throw new IllegalArgumentException("Faun effects differ from history");
        if(s.mode()==GameMode.CLASSIC && (!s.faun().clouds().isEmpty() || !s.faun().invisible().isEmpty()))throw new IllegalArgumentException("Faun effects in Classic");
        for(Cloud c:s.faun().clouds()) {
            var p=s.pieces().get(c.sourceId());
            if(p==null || (p.hero()!=HeroType.FAUN_KNIGHT && p.hero()!=HeroType.GINSENG_BISHOP) || c.castTurn()>s.turn().number() || c.expiresAfterTurn()<s.turn().number())throw new IllegalArgumentException("Invalid cloud source or lifetime");
        }
        for(var e:s.faun().invisible().entrySet()) {
            var p=s.pieces().get(e.getKey());
            if(p==null || p.isRemoved() || (p.hero()!=HeroType.FAUN_KNIGHT && p.hero()!=HeroType.GINSENG_BISHOP) || e.getValue().castTurn()>s.turn().number() || e.getValue().expiresAfterTurn()<s.turn().number())throw new IllegalArgumentException("Invalid invisible piece");
        }
    }
}
