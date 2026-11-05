package game.heroes.pudge;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;

import game.heroes.HeroRegistry;
import game.moves.Success;
import game.core.Game;
import game.core.Player;
import game.heroes.HeroType;
import game.scenarios.PudgeScenario;

import model.Figure;
import org.junit.Test;
import java.lang.reflect.Modifier;
import static game.core.Positions.pos;
import static org.junit.Assert.*;

public class PudgePreviewTest {
    @Test public void crossPackageHeroPreviewsAreImmutableAndCannotSpendLiveTurnBudgets() {
        Game game = PudgeScenario.DIAGONAL_HOOK.createGame(() -> 0L); var before = game.getSnapshot();
        var effect = game.getHeroRegistry().get(HeroType.PUDGE).preview(
                new HeroActionContext(before.field(), before.pieces(), before.turn().number(), before.mechanics()),
                new AbilityRequest(pos("e2"), PudgeAbility.HOOK, pos("h5")));
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(pos("f3"), effect.updates().get(before.pieceAt(pos("h5")).id()).position());
        assertEquals(before, game.getSnapshot());
        assertThrows(UnsupportedOperationException.class, () -> effect.updates().clear());
        assertThrows(UnsupportedOperationException.class, () -> effect.captures().clear());
        assertTrue(HeroRegistry.standard().get(HeroType.NONE).abilities().isEmpty());
    }
    @Test public void packageReorganizationDoesNotExposeMutablePlayerOperations() throws Exception {
        assertFalse(Modifier.isPublic(Player.class.getDeclaredMethod("recordCapture", Figure.class).getModifiers()));
        assertFalse(Modifier.isPublic(Player.class.getDeclaredMethod("recordMove", game.history.MadeMove.class).getModifiers()));
        assertFalse(Modifier.isPublic(Class.forName("game.core.ActionEngine").getModifiers()));
        assertFalse(Modifier.isPublic(Class.forName("game.core.ChessClock").getModifiers()));
    }
}
