package game.heroes.clownganger;
import game.state.*;
import game.heroes.HeroType;
import game.history.GameEvent;
import java.util.Objects;
public final class ClownView {
    private ClownView(){}
    public static PieceState displayed(GameSnapshot s,PieceState p){
        if(p==null || p.color()==s.active() || p.hero()!=HeroType.ILLUSION)return p;
        var source=s.pieces().get(s.clown().illusions().get(p.id()).sourceId());
        return new PieceState(p.id(),p.type(),p.color(),p.position(),p.status(),source.hero(),source.cooldown(),source.ultimateCharge(),p.inventory(),source.storedPieceId(),p.blindTurns(),source.startingColor(),p.stunnedThroughTurn(),p.boxedThroughTurn(),p.potted());
    }
    public static boolean concealed(GameSnapshot s,GameEvent e){
        // Hide creation and both identities' action coordinates from the opposing local view.
        return s.clown().illusions().values().stream().anyMatch(i->{var source=s.pieces().get(i.sourceId());return source.color()!=s.active() && e.turn()>=i.bornTurn() && (Objects.equals(e.actorId(),i.id()) || Objects.equals(e.actorId(),i.sourceId()) || Objects.equals(e.targetId(),i.id()));});
    }
}
