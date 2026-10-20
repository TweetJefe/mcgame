package game.scenarios;

import game.core.Game;

import java.util.function.LongSupplier;

public interface TrainingScenario {
    String getTitle();

    String getInstructions();

    boolean requiresCharge();

    Game createGame();

    Game createGame(LongSupplier time);

    default String getSettingsLabel() {
        return requiresCharge() ? "Хук без КД • Ульта за 2 взятия" : "Хук без КД • Ульта без заряда";
    }
}
