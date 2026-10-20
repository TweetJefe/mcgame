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

public enum BountyCrowScenario implements TrainingScenario {
    BOUNTY("Награда за цель",
            "Crow b2 → Награда → чёрный конь e4. Затем ладья e1 берёт e4: белым 3 + 1,5 = 4,5 золота. Метка исчезает при взятии. Завершите ход."),
    PASSIVE("Пассивка и метка вместе",
            "Crow d4 → Награда → пешка e5. Затем тот же Crow берёт e5: белым 1 + 1,5 + 0,5 = 3 золота. Пешки учитываются и в заряде ульты."),
    STEAL("Кража золота",
            "Crow f6 уже взял две пешки: заряд 2/2, у белых 3 золота. У чёрных 3 золота за коня. Выберите Crow → Украсть: станет 5 у белых и 1 у чёрных. Затем f6–f7 и завершите ход.");
    private final String title, instructions;

    BountyCrowScenario(String title, String instructions) {
        this.title = title;
        this.instructions = instructions;
    }

    public String getTitle() {
        return title;
    }

    public String getInstructions() {
        return instructions;
    }

    public boolean requiresCharge() {
        return true;
    }

    public String getSettingsLabel() {
        return "Награда: КД 3 • Метка: 2 следующих своих хода • Ульта за 2 взятия";
    }

    public Game createGame() {
        return createGame(System::nanoTime);
    }

    public Game createGame(LongSupplier time) {
        Field factory = new Field(8);
        var board = new HashMap<Position, Figure>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Position p = new Position(r, c);
                board.put(p, factory.createFigure(Empty.class, p, NONE));
            }
        put(factory, board, King.class, WHITE, "a1");
        put(factory, board, King.class, BLACK, "h8");
        String crow = this == BOUNTY ? "b2" : "d4";
        put(factory, board, Pawn.class, WHITE, crow);
        if (this == BOUNTY) {
            put(factory, board, Rook.class, WHITE, "e1");
            put(factory, board, Knight.class, BLACK, "e4");
        } else put(factory, board, Pawn.class, BLACK, "e5");
        if (this == STEAL) {
            put(factory, board, Pawn.class, BLACK, "f6");
            put(factory, board, Rook.class, BLACK, "h4");
            put(factory, board, Knight.class, WHITE, "h3");
        }
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                Map.of(p(crow), HeroType.BOUNTY_CROW_PAWN), HeroRules.prototype(), time);
        if (this == STEAL) {
            move(game, "d4", "e5");
            end(game);
            move(game, "h4", "h3");
            end(game);
            move(game, "e5", "f6");
            end(game);
            move(game, "h8", "g8");
            end(game);
        }
        return game;
    }

    private static void move(Game game, String from, String to) {
        require(game.moveFigure(game.getCurrentPlayer(), p(from), p(to)));
    }

    private static void end(Game game) {
        require(game.endTurn(game.getCurrentPlayer()));
    }

    private static void require(MoveResult result) {
        if (!result.isSuccess()) throw new IllegalStateException("Invalid Crow exercise: " + result);
    }

    private static Position p(String cell) {
        return new Position(cell.charAt(1) - '1', cell.charAt(0) - 'a');
    }

    private static void put(Field factory, Map<Position, Figure> board, Class<? extends Figure> type, FigureColor side, String cell) {
        Position p = p(cell);
        board.put(p, factory.createFigure(type, p, side));
    }
}
