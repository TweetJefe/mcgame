package game.persistence;

import game.heroes.bountycrow.BountyCrowAbility;
import game.heroes.bolotnik.BolotnikAbility;

import game.moves.Success;
import game.moves.TurnRejection;
import game.core.Game;
import game.scenarios.BolotnikScenario;
import game.scenarios.BountyCrowScenario;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static game.core.Positions.pos;
import static org.junit.Assert.*;

public class BolotnikSaveTest {
    private String xml(Game g)throws Exception { return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8); }
    private Game load(String s)throws Exception { return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L); }
    private Game hidden() {
        Game g=BolotnikScenario.AMBUSH.createGame(()->0L);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("f5"),null,BolotnikAbility.AMBUSH));return g;
    }
    private void invalid(String s) { assertThrows(java.io.IOException.class,()->GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8))); }
    @Test public void allExercisesRoundtripAndRestartExactly()throws Exception {
        for(var scenario:BolotnikScenario.values()) {
            Game g=scenario.createGame(()->0L);String s=xml(g);assertTrue(s.contains("formatVersion=\"4\""));
            assertEquals(s,xml(load(s)));assertEquals(s,xml(scenario.createGame(()->0L)));
        }
    }
    @Test public void hiddenHeroAndFriendlyOccupantRestoreAndTrapStillReleases()throws Exception {
        Game g=load(xml(hidden()));long id=g.getTerrain().cells().values().iterator().next().ownerId();
        assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos("a1"),pos("b1")));assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer()));
        assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos("e8"),pos("e5")));
        assertEquals(id,g.getPieceState(pos("e5")).id());assertTrue(g.getTerrain().cells().isEmpty());assertEquals(xml(g),xml(load(xml(g))));
    }
    @Test public void timedMudAndCooldownPersistWithoutGivingAnExtraSkill()throws Exception {
        Game g=BolotnikScenario.MUD.createGame(()->0L);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("d4"),pos("f4"),BolotnikAbility.MUD));
        g=load(xml(g));assertEquals(4,g.getPieceState(pos("d4")).cooldown());assertTrue(g.getTurnState().abilityUsed());
        assertEquals(TurnRejection.ABILITY_ALREADY_USED,g.useAbility(g.getCurrentPlayer(),pos("d4"),null,BolotnikAbility.AMBUSH));
        assertEquals(5,g.getTerrain().cells().get(pos("f4")).expiresAfterTurn());
    }
    @Test public void rejectsOrphanHiddenHeroIncompleteFootprintWrongOwnerAndDowngrade()throws Exception {
        String s=xml(hidden());
        invalid(s.replaceAll("(?s)<mud [^>]*/>",""));
        invalid(s.replaceFirst("(?s)<mud [^>]*/>",""));
        invalid(s.replaceAll("ownerId=\"[0-9]+\"","ownerId=\"999999\""));
        invalid(s.replace("formatVersion=\"4\"","formatVersion=\"3\""));
        invalid(s.replace("HIDDEN_BOG","BOARD"));
    }
    @Test public void rejectsNegativeMissingOrImplausibleMudExpiryAndDuplicateCell()throws Exception {
        Game g=BolotnikScenario.MUD.createGame(()->0L);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("d4"),pos("f4"),BolotnikAbility.MUD));String s=xml(g);
        invalid(s.replace("expiresAfterTurn=\"5\"","expiresAfterTurn=\"-1\""));
        invalid(s.replace("expiresAfterTurn=\"5\"","expiresAfterTurn=\"500\""));
        invalid(s.replace("expiresAfterTurn=\"5\"",""));
        var matcher=java.util.regex.Pattern.compile("<mud [^>]*/>").matcher(s);assertTrue(matcher.find());String cell=matcher.group();invalid(s.replace(cell,cell+cell));
    }
    @Test public void previousFormatThreeRemainsWritableAndPlayableWithoutTerrain()throws Exception {
        String s=xml(BountyCrowScenario.STEAL.createGame(()->0L));assertTrue(s.contains("formatVersion=\"3\""));
        Game g=load(s);assertEquals(s,xml(g));assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("f6"),null,BountyCrowAbility.STEAL));
        assertTrue(g.getTerrain().cells().isEmpty());assertEquals(10,g.getEconomy().whiteHalves());
    }
}
