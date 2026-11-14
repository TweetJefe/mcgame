package game.heroes.ares;
import game.core.Game;
import game.heroes.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AresEnemyTargetsTest {
 private Position p(int row){return new Position(row,3);}
 private Game setup(){var g=Game.sandbox();assertTrue(g.placeEditorPiece(Bishop.class,HeroType.ARES,FigureColor.WHITE,p(1)).isSuccess());return g;}
 private void put(Game g,FigureColor side,int row){assertTrue(g.placeEditorPiece(Pawn.class,HeroType.NONE,side,p(row)).isSuccess());}
 private void cast(Game g){assertTrue(g.useAbility(g.getCurrentPlayer(),new AbilityRequest(p(1),AresAbility.SPEAR,p(4))).isSuccess());}
 @Test public void allyBlocksWithoutMovingOrStunning(){var g=setup();put(g,FigureColor.WHITE,2);put(g,FigureColor.BLACK,4);cast(g);assertNotNull(g.getPieceState(p(2)));assertFalse(g.getPieceState(p(2)).isStunned());assertFalse(g.getPieceState(p(4)).isStunned());}
 @Test public void enemyIsPinnedAgainstAllyWithoutStunningAlly(){var g=setup();put(g,FigureColor.BLACK,2);put(g,FigureColor.WHITE,4);cast(g);assertEquals(FigureColor.BLACK,g.getPieceState(p(3)).color());assertTrue(g.getPieceState(p(3)).isStunned());assertFalse(g.getPieceState(p(4)).isStunned());}
 @Test public void bothEnemiesAreStunnedIncludingOrdinaryPawns(){var g=setup();put(g,FigureColor.BLACK,2);put(g,FigureColor.BLACK,4);cast(g);assertTrue(g.getPieceState(p(3)).isStunned());assertTrue(g.getPieceState(p(4)).isStunned());}
}
