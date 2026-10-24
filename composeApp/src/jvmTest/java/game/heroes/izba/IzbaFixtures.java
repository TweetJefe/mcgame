package game.heroes.izba;

import game.core.Game;
import game.heroes.*;
import game.state.*;
import game.summoning.*;
import model.*;
import model.figures.*;
import java.util.*;
import static game.core.Positions.pos;
import static model.FigureColor.*;

public final class IzbaFixtures {
    private static final Field FACTORY=new Field(8);
    public static Map<Position,Figure> board(String whiteKing,String blackKing) {
        var b=new HashMap<Position,Figure>();
        for(int r=0;r<8;r++)for(int c=0;c<8;c++){Position p=new Position(r,c);b.put(p,FACTORY.createFigure(Empty.class,p,NONE));}
        put(b,King.class,WHITE,whiteKing);put(b,King.class,BLACK,blackKing);return b;
    }
    public static void put(Map<Position,Figure>b,Class<? extends Figure> type,FigureColor color,String square) {
        b.put(pos(square),FACTORY.createFigure(type,pos(square),color));
    }
    public static Game create(Map<Position,Figure>b,Map<Position,HeroType>heroes,FigureColor active,long seed) {
        Game g=Game.fromMobaPosition(new GamePosition(b,active,CastlingRights.none(),null,0),heroes,HeroRules.prototype(),()->0L);
        var s=g.getSnapshot();return Game.fromSnapshot(copy(s,s.pieces(),SummonState.initial(seed)),()->0L);
    }
    public static Game standard(long seed) {
        var b=board("a1","h8");put(b,Rook.class,WHITE,"e2");put(b,Knight.class,WHITE,"b1");put(b,Knight.class,BLACK,"g8");
        return charge(create(b,Map.of(pos("e2"),HeroType.IZBA),WHITE,seed),"e2");
    }
    public static Game charge(Game g,String...squares) {
        var s=g.getSnapshot();var states=new HashMap<>(s.pieces());
        for(String square:squares){var p=s.pieceAt(pos(square));states.put(p.id(),p.withAbilityState(p.cooldown(),2,p.storedPieceId()));}
        return Game.fromSnapshot(copy(s,states,s.summons()),()->0L);
    }
    public static GameSnapshot copy(GameSnapshot s,Map<Long,PieceState>pieces,SummonState summons) {
        return new GameSnapshot(s.version(),s.mode(),s.field(),pieces,s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),summons);
    }
    public static long seedFor(int choices,int index) {
        for(long seed=0;seed<100_000;seed++)if(SummonState.initial(seed).choose(choices).index()==index)return seed;
        throw new AssertionError("Missing test seed");
    }
}
