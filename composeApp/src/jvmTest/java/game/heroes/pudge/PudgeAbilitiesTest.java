package game.heroes.pudge;

import game.moves.Success;
import game.core.Game;
import game.core.Player;
import game.heroes.Ability;
import game.heroes.HeroAbilities;
import game.heroes.HeroType;
import game.scenarios.PudgeScenario;

import model.Figure;
import org.junit.Test;
import java.lang.reflect.Modifier;
import static game.core.Positions.pos;
import static org.junit.Assert.*;

public class PudgeAbilitiesTest {
    @Test public void crossPackageHeroPreviewsAreImmutableAndCannotSpendLiveTurnBudgets() {
        Game game = PudgeScenario.DIAGONAL_HOOK.createGame(() -> 0L); var before = game.getSnapshot();
        var effect = HeroAbilities.preview(before.field(), before.pieceAt(pos("e2")), before.pieceAt(pos("h5")), Ability.HOOK, before.pudgeRules());
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(pos("f3"), effect.updates().get(before.pieceAt(pos("h5")).id()).position());
        assertEquals(before, game.getSnapshot());
        assertThrows(UnsupportedOperationException.class, () -> effect.updates().clear());
        assertThrows(UnsupportedOperationException.class, () -> effect.captures().clear());
        assertTrue(HeroAbilities.abilities(HeroType.NONE).isEmpty());
    }
    @Test public void packageReorganizationDoesNotExposeMutablePlayerOperations() throws Exception {
        assertFalse(Modifier.isPublic(Player.class.getDeclaredMethod("recordCapture", Figure.class).getModifiers()));
        assertFalse(Modifier.isPublic(Player.class.getDeclaredMethod("recordMove", game.history.MadeMove.class).getModifiers()));
        assertFalse(Modifier.isPublic(Class.forName("game.core.ActionEngine").getModifiers()));
        assertFalse(Modifier.isPublic(Class.forName("game.core.ChessClock").getModifiers()));
    }
}
