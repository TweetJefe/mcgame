package game.scenarios;

import game.core.Game;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.moves.MoveResult;
import game.state.CastlingRights;
import game.state.GamePosition;
import model.*;
import model.figures.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import static model.FigureColor.*;

public enum LumeshardScenario implements TrainingScenario {
    LASER("Лазер и ослепление",
            "Белые: Lumeshard d2 → Лазер → ладья g5 через пешку e3. Затем d2–c1 → Закончить ход. Чёрные: ладья g5 может пойти на g4, но не взять коня g2. После завершения хода чёрных ослепление исчезнет."),
    TELEPORT("Телепорт на исходный цвет",
            "Lumeshard f4 уже взял две пешки, заряд 2/2. Белые: Телепорт → a3 (чёрная клетка). Затем тот же слон a3–b4 → Закончить ход. Телепорт не заменяет обычное перемещение."),
    HOOK_COLOR("Исходный цвет после хука",
            "Белые: Пудж f2 → Хук → Lumeshard f4. Слон окажется на белой f3. Король a1–b1 → Закончить ход. Чёрные: король h8–g8 → Закончить ход. Теперь Lumeshard f3 может телепортироваться на чёрную a3, сохраняя исходный цвет.");

    private final String title, instructions;
    LumeshardScenario(String title, String instructions) { this.title = title; this.instructions = instructions; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
    public boolean requiresCharge() { return true; }
    public String getSettingsLabel() { return "Лазер: КД 3 • Ослепление: 1 ход • Ульта за 2 взятия"; }
    public Game createGame() { return createGame(System::nanoTime); }
    public Game createGame(LongSupplier time) {
        Field factory = new Field(8); var board = new HashMap<Position, Figure>();
        for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
            Position square = new Position(row, col); board.put(square, factory.createFigure(Empty.class, square, NONE));
        }
        put(factory, board, King.class, WHITE, "a1"); put(factory, board, King.class, BLACK, "h8");
        put(factory, board, Bishop.class, WHITE, "d2");
        var heroes = new HashMap<Position, HeroType>(); heroes.put(p("d2"), HeroType.LUMESHARD_BISHOP);
        if (this == LASER) {
            put(factory, board, Pawn.class, WHITE, "e3"); put(factory, board, Rook.class, BLACK, "g5");
            put(factory, board, Knight.class, WHITE, "g2");
        } else {
            put(factory, board, Pawn.class, BLACK, "e3"); put(factory, board, Pawn.class, BLACK, "f4");
            if (this == HOOK_COLOR) { put(factory, board, Rook.class, WHITE, "f2"); heroes.put(p("f2"), HeroType.PUDGE_ROOK); }
        }
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0), heroes, HeroRules.prototype(), time);
        if (this != LASER) {
            move(game, "d2", "e3"); end(game); move(game, "h8", "g8"); end(game);
            move(game, "e3", "f4"); end(game); move(game, "g8", "h8"); end(game);
        }
        return game;
    }
    private static void move(Game game, String from, String to) { require(game.moveFigure(game.getCurrentPlayer(), p(from), p(to))); }
    private static void end(Game game) { require(game.endTurn(game.getCurrentPlayer())); }
    private static void require(MoveResult result) { if (!result.isSuccess()) throw new IllegalStateException("Invalid training scenario: " + result); }
    private static Position p(String cell) { return new Position(cell.charAt(1) - '1', cell.charAt(0) - 'a'); }
    private static void put(Field factory, Map<Position, Figure> board, Class<? extends Figure> type, FigureColor side, String cell) {
        Position square = p(cell); board.put(square, factory.createFigure(type, square, side));
    }
}
