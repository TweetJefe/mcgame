package game.heroes.ginseng;

import game.core.Game;
import game.heroes.*;
import game.state.*;
import model.*;
import model.figures.*;
import java.util.*;
import static game.core.Positions.pos;
import static model.FigureColor.*;

public final class GinsengFixtures {
    private static final Field FACTORY=new Field(8);
    public static Map<Position,Figure> board() {
        var b=new HashMap<Position,Figure>();
        for(int r=0;r<8;r++)for(int c=0;c<8;c++){Position p=new Position(r,c);b.put(p,FACTORY.createFigure(Empty.class,p,NONE));}
        put(b,King.class,WHITE,"a1");put(b,King.class,BLACK,"h8");put(b,Bishop.class,WHITE,"c2");
        put(b,Knight.class,WHITE,"b1");put(b,Knight.class,BLACK,"g8");return b;
    }
    public static void put(Map<Position,Figure>b,Class<? extends Figure> type,FigureColor color,String square) {
        b.put(pos(square),FACTORY.createFigure(type,pos(square),color));
    }
    public static void empty(Map<Position,Figure>b,String square){put(b,Empty.class,NONE,square);}
    public static Game create(Map<Position,Figure>b,Map<Position,HeroType> other) {
        var heroes=new HashMap<>(other);heroes.put(pos("c2"),HeroType.GINSENG_BISHOP);
        return Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),heroes,HeroRules.prototype(),()->0L);
    }
    public static Game charged(Game g,String...squares) {
        var s=g.getSnapshot();var states=new HashMap<>(s.pieces());
        for(String cell:squares){var p=s.pieceAt(pos(cell));states.put(p.id(),p.withAbilityState(p.cooldown(),2,p.storedPieceId()));}
        return Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),states,s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),s.summons()),()->0L);
    }
    public static Game hook() {
        var b=board();put(b,Rook.class,WHITE,"e2");put(b,Pawn.class,BLACK,"e5");put(b,Pawn.class,BLACK,"a4");put(b,Knight.class,WHITE,"f2");
        return charged(create(b,Map.of(pos("e2"),HeroType.PUDGE_ROOK)),"c2","e2");
    }
    public static Game readyHook() {
        Game g=hook();ok(g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("e5"),Ability.HOOK));
        play(g,"b1","a3");play(g,"g8","f6");ok(g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("e2"),Ability.COPY_SPELL));
        play(g,"a3","b1");play(g,"f6","g8");return g;
    }
    private static void play(Game g,String a,String b){ok(g.moveFigure(g.getCurrentPlayer(),pos(a),pos(b)));ok(g.endTurn(g.getCurrentPlayer()));}
    private static void ok(game.moves.MoveResult result){if(!result.isSuccess())throw new AssertionError(result);}
}
