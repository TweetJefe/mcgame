package game.scenarios;

import game.core.Game;
import game.heroes.HeroType;
import game.heroes.pudge.PudgeRules;
import game.state.CastlingRights;
import game.state.GamePosition;

import model.*;
import model.figures.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

import static model.FigureColor.*;

/**
 * Fresh, untimed teaching positions. Restart creates a new Game, never rewinds a live one.
 */
public enum PudgeScenario implements TrainingScenario {
    DIAGONAL_HOOK("Хук по диагонали",
            "Белые: Пудж e2 → Хук → конь h5. Конь окажется на f3. Затем переместите Пуджа e2–d2 и закончите ход."),
    INDIRECT_CHECK("Открыть косвенный шах",
            "Белые: Пудж b2 → Хук → конь e2. Конь окажется на c2, а ладья e1 даст шах королю e8. Переместите Пуджа b2–b3 и закончите ход."),
    ESCAPE_CHECK("Спастись от шаха",
            "У белых шах и нет обычного легального хода. Пудж f1 → Хук → ладья h3. Она окажется на g2, закрыв другую ладью. Затем Пудж f1–e1 → Закончить ход."),
    COUNTER_CAPTURE("Проглотить и ответить на взятие",
            "Белые: Пудж e2 → Проглотить → конь d3, затем e2–f2 → Закончить ход. Чёрные: ладья f8 берёт Пуджа f2. Белый конь вернётся на f2 и возьмёт эту ладью."),
    ULTIMATE_CHARGE("Заряд ульты: два взятия",
            "Белые: Пудж e2 берёт пешку e3 → Закончить ход. Чёрные: король h8–g8 → Закончить ход. Белые: Пудж e3 берёт пешку f3. Заряд 2/2: Проглотить → союзный конь g4 (по диагонали). Заряд станет 0/2.");

    private final String title, instructions;

    PudgeScenario(String title, String instructions) {
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
        return this == ULTIMATE_CHARGE;
    }

    public Game createGame() {
        return createGame(System::nanoTime);
    }

    public Game createGame(LongSupplier time) {
        Field factory = new Field(8);
        Map<Position, Figure> board = new HashMap<>();
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Position square = new Position(row, col);
                board.put(square, factory.createFigure(Empty.class, square, NONE));
            }
        String pudge = "e2";
        put(factory, board, King.class, WHITE, "a1");
        put(factory, board, King.class, BLACK, "h8");
        switch (this) {
            case DIAGONAL_HOOK -> put(factory, board, Knight.class, BLACK, "h5");
            case INDIRECT_CHECK -> {
                pudge = "b2";
                put(factory, board, Empty.class, NONE, "a1");
                put(factory, board, King.class, WHITE, "h1");
                put(factory, board, Empty.class, NONE, "h8");
                put(factory, board, King.class, BLACK, "e8");
                put(factory, board, Rook.class, WHITE, "e1");
                put(factory, board, Knight.class, BLACK, "e2");
            }
            case ESCAPE_CHECK -> {
                pudge = "f1";
                put(factory, board, Empty.class, NONE, "a1");
                put(factory, board, Empty.class, NONE, "h8");
                put(factory, board, King.class, WHITE, "h1");
                put(factory, board, King.class, BLACK, "a8");
                put(factory, board, Rook.class, BLACK, "g3");
                put(factory, board, Rook.class, BLACK, "h3");
            }
            case COUNTER_CAPTURE -> {
                put(factory, board, Knight.class, WHITE, "d3");
                put(factory, board, Rook.class, BLACK, "f8");
            }
            case ULTIMATE_CHARGE -> {
                put(factory, board, Pawn.class, BLACK, "e3");
                put(factory, board, Pawn.class, BLACK, "f3");
                put(factory, board, Knight.class, WHITE, "g4");
            }
        }
        put(factory, board, Rook.class, WHITE, pudge);
        PudgeRules rules = requiresCharge() ? new PudgeRules(0, 2, true, true) : PudgeRules.sandbox();
        return Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                Map.of(square(pudge), HeroType.PUDGE), rules, time);
    }

    private static void put(Field factory, Map<Position, Figure> board, Class<? extends Figure> type,
                            FigureColor color, String name) {
        Position square = square(name);
        board.put(square, factory.createFigure(type, square, color));
    }

    private static Position square(String name) {
        return new Position(name.charAt(1) - '1', name.charAt(0) - 'a');
    }
}
