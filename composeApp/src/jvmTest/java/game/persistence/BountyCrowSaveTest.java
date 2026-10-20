package game.persistence;

import game.moves.Success;
import game.core.Game;
import game.heroes.Ability;
import game.scenarios.BountyCrowScenario;
import game.scenarios.LumeshardScenario;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static game.core.Positions.pos;
import static org.junit.Assert.*;

public class BountyCrowSaveTest {
    private String xml(Game game) throws Exception { return new String(GameSaveFile.encode(game.getSnapshot()), StandardCharsets.UTF_8); }
    private Game load(String xml) throws Exception { return Game.fromSnapshot(GameSaveFile.decode(xml.getBytes(StandardCharsets.UTF_8)), () -> 0L); }
    private Game marked() {
        Game game = BountyCrowScenario.PASSIVE.createGame(() -> 0L);
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("d4"), pos("e5"), Ability.BOUNTY)); return game;
    }
    private void invalid(String xml) {
        try { GameSaveFile.decode(xml.getBytes(StandardCharsets.UTF_8)); fail("Expected invalid save"); }
        catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("Некорректное")); }
    }
    @Test public void allTrainingPositionsRoundtripExactlyIncludingPreparedGoldLedger() throws Exception {
        for (var scenario : BountyCrowScenario.values()) {
            Game game = scenario.createGame(() -> 0L); String data = xml(game);
            assertTrue(data.contains("formatVersion=\"3\"")); Game copy = load(data);
            assertEquals(game.getSnapshot().economy(), copy.getSnapshot().economy()); assertEquals(data, xml(copy));
            assertEquals(data, xml(scenario.createGame(() -> 0L)));
        }
    }
    @Test public void loadingAnActiveMarkPaysBothBonusesOnContinuation() throws Exception {
        Game game = load(xml(marked()));
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("d4"), pos("e5")));
        assertEquals(6, game.getEconomy().whiteHalves()); assertTrue(game.getEconomy().marks().isEmpty());
        assertEquals(xml(game), xml(load(xml(game))));
    }
    @Test public void theftAfterLoadingTransfersInsteadOfMintingGold() throws Exception {
        Game game = load(xml(BountyCrowScenario.STEAL.createGame(() -> 0L)));
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("f6"), null, Ability.STEAL));
        assertEquals(10, game.getEconomy().whiteHalves()); assertEquals(2, game.getEconomy().blackHalves());
        assertEquals(xml(game), xml(load(xml(game))));
    }
    @Test public void rejectsChangedWalletPriceBonusMissingLedgerAndDowngrade() throws Exception {
        Game game = marked(); assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("d4"), pos("e5")));
        String data = xml(game);
        invalid(data.replace("whiteHalves=\"6\"", "whiteHalves=\"600\""));
        invalid(data.replace("baseHalves=\"2\"", "baseHalves=\"4\""));
        invalid(data.replace("passiveHalves=\"1\"", "passiveHalves=\"0\""));
        invalid(data.replace("bountyHalves=\"3\"", "bountyHalves=\"2\""));
        invalid(data.replaceAll("(?s)<change [^>]*/>", ""));
        invalid(data.replace("formatVersion=\"3\"", "formatVersion=\"2\""));
    }
    @Test public void rejectsMissingExpiryWrongSourceAndDuplicateMark() throws Exception {
        String data = xml(marked());
        invalid(data.replaceAll(" expiresAfterTurn=\"[^\"]*\"", ""));
        invalid(data.replace("expiresAfterTurn=\"5\"", "expiresAfterTurn=\"500\""));
        String mark = java.util.regex.Pattern.compile("<mark [^>]*/>").matcher(data).results().findFirst().orElseThrow().group();
        invalid(data.replace(mark, mark + mark));
    }
    @Test public void previousFormatTwoStillLoadsAndRetainsItsLegacyEconomyMode() throws Exception {
        Game game = LumeshardScenario.TELEPORT.createGame(() -> 0L);
        String old = xml(game).replace("formatVersion=\"3\"", "formatVersion=\"2\"")
                .replaceAll("(?s)\\s*<crowRules [^>]*/>", "")
                .replaceAll("(?s)\\s*<goldRules [^>]*/>", "")
                .replaceAll("(?s)\\s*<economy .*?</economy>", "");
        Game copy = load(old); assertFalse(copy.getEconomy().enabled());
        assertTrue(xml(copy).contains("formatVersion=\"2\""));
        assertEquals(Success.SUCCESS, copy.useAbility(copy.getCurrentPlayer(), pos("f4"), pos("a3"), Ability.TELEPORT));
    }
}
