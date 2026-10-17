package game.scenarios;

import game.core.Game;
import game.heroes.*;
import game.moves.MoveResult;
import game.state.*;
import model.*;
import model.figures.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import static model.FigureColor.*;

public enum BolotnikScenario implements TrainingScenario {
    MUD("Грязь под союзником",
            "Bolotnik d4 → Грязь → своя пешка f4. Король a1–b1, завершить ход. Чёрная ладья f8 берёт пешку f4: Bolotnik появляется на f4 и съедает ладью."),
    AMBUSH("Ожидание в болоте",
            "Bolotnik f5 готов к ульте. Засада: болото e5–g5, герой исчезнет. Король a1–b1, завершить ход. Чёрная ладья e8 берёт пешку e5: Bolotnik выходит на e5, болото исчезает."),
    LEFTMOST("Ульта: первая цель слева",
            "Bolotnik f5 готов к ульте. На e5 и g5 стоят враги. Засада немедленно съедает пешку e5, ближайшую к A. Герой выходит на e5, g5 остаётся, болото исчезает. Затем нужен обычный ход."),
    KNIGHT("Прыжок через грязь",
            "Bolotnik d4 → Грязь → b2. Король a1–a2, завершить ход. Чёрный конь b1 не может прыгнуть на c3 через b2, но может пойти на d2.");
    private final String title, instructions;
    BolotnikScenario(String title, String instructions) { this.title=title; this.instructions=instructions; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
    public boolean requiresCharge() { return true; }
    public String getSettingsLabel() { return "Грязь: радиус 4, КД 4 • 2 следующих своих хода • Ульта за 2 взятия"; }
    public Game createGame() { return createGame(System::nanoTime); }
    public Game createGame(LongSupplier time) {
        Field factory=new Field(8); var board=new HashMap<Position,Figure>();
        for(int r=0;r<8;r++) for(int c=0;c<8;c++) { Position p=new Position(r,c);board.put(p,factory.createFigure(Empty.class,p,NONE)); }
        put(factory,board,King.class,WHITE,"a1");put(factory,board,King.class,BLACK,"h8");
        boolean charged=this==AMBUSH || this==LEFTMOST;
        String hero=charged ? "b3" : "d4";put(factory,board,Knight.class,WHITE,hero);
        if(charged) {
            put(factory,board,Pawn.class,BLACK,"d4");put(factory,board,Pawn.class,BLACK,"f5");
            if(this==LEFTMOST) { put(factory,board,Pawn.class,BLACK,"e5");put(factory,board,Pawn.class,BLACK,"g5"); }
            else { put(factory,board,Pawn.class,WHITE,"e5");put(factory,board,Rook.class,BLACK,"e8"); }
        } else if(this==MUD) { put(factory,board,Pawn.class,WHITE,"f4");put(factory,board,Rook.class,BLACK,"f8"); }
        else put(factory,board,Knight.class,BLACK,"b1");
        Game game=Game.fromMobaPosition(new GamePosition(board,WHITE,CastlingRights.none(),null,0),Map.of(p(hero),HeroType.BOLOTNIK_KNIGHT),HeroRules.prototype(),time);
        if(charged) { move(game,"b3","d4");end(game);move(game,"h8","g8");end(game);move(game,"d4","f5");end(game);move(game,"g8","h8");end(game); }
        return game;
    }
    private static void move(Game game,String from,String to) { require(game.moveFigure(game.getCurrentPlayer(),p(from),p(to))); }
    private static void end(Game game) { require(game.endTurn(game.getCurrentPlayer())); }
    private static void require(MoveResult result) { if(!result.isSuccess()) throw new IllegalStateException("Invalid Bolotnik exercise: "+result); }
    private static Position p(String s) { return new Position(s.charAt(1)-'1',s.charAt(0)-'a'); }
    private static void put(Field factory,Map<Position,Figure> board,Class<? extends Figure> type,FigureColor color,String s) { Position p=p(s);board.put(p,factory.createFigure(type,p,color)); }
}
