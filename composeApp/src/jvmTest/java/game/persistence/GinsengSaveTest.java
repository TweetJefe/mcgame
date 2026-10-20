package game.persistence;

import game.moves.Success;
import game.core.Game;
import game.draft.*;
import game.heroes.*;
import game.heroes.ginseng.*;
import game.scenarios.PudgeScenario;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static game.core.Positions.pos;
import static org.junit.Assert.*;

public class GinsengSaveTest {
    private String xml(Game g)throws Exception{return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String text)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(text.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String text){assertThrows(java.io.IOException.class,()->GameSaveFile.decode(text.getBytes(StandardCharsets.UTF_8)));}
    @Test public void savedCopyAndLatestSourceCastRestoreWithoutAnotherChargeOrExtraUse()throws Exception {
        Game g=GinsengFixtures.readyHook();String s=xml(g);assertTrue(s.contains("formatVersion=\"7\""));Game restored=load(s);
        assertEquals(s,xml(restored));assertEquals(g.getSpells(),restored.getSpells());
        assertEquals(Success.SUCCESS,restored.useAbility(restored.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.HOOK,pos("a4"),null,true)));
        String used=xml(restored);assertEquals(used,xml(load(used)));assertTrue(load(used).getSpells().borrowed().isEmpty());
    }
    @Test public void rejectsMissingWrongLatestAndPrematureCopiedSpellMetadata()throws Exception {
        String s=xml(GinsengFixtures.readyHook());
        invalid(s.replace("copiedAbility=\"HOOK\"",""));invalid(s.replace("copiedAbility=\"HOOK\"","copiedAbility=\"SWALLOW\""));
        invalid(s.replace("copiedAbility=\"HOOK\"","copiedAbility=\"UNKNOWN\""));
        invalid(s.replace("borrowed=\"false\"","borrowed=\"true\""));invalid(s.replaceAll("(?s)<ginsengRules [^>]*/>",""));
        invalid(s.replace("formatVersion=\"7\"","formatVersion=\"6\""));
    }
    @Test public void changingBorrowedCastToNativeCannotCreateAnUnlimitedGinsengHook()throws Exception {
        Game g=GinsengFixtures.readyHook();assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.HOOK,pos("a4"),null,true)));
        String s=xml(g);invalid(s.replace("borrowed=\"true\"","borrowed=\"false\""));
        invalid(s.replaceAll("(?s)<event [^>]*kind=\"COPY_SPELL\"[^>]*/>",""));
    }
    @Test public void previousFormatWithoutGinsengRetainsItsVersionAndRebuildsLastUsedMemory()throws Exception {
        Game g=PudgeScenario.values()[0].createGame(()->0L);
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("h5"),Ability.HOOK));String s=xml(g);
        assertFalse(s.contains("formatVersion=\"7\""));assertFalse(s.contains("borrowed="));
        Game restored=load(s);assertEquals(s,xml(restored));assertEquals(Ability.HOOK,restored.getSpells().lastUsed().get(g.getPieceState(pos("e2")).id()));
    }
    @Test public void latestCatalogCanBeSavedEvenWhenGinsengWasBanned()throws Exception {
        var d=new Draft(new DraftRules(1,6,3));d.ban(model.FigureColor.WHITE,HeroType.GINSENG_BISHOP);d.ban(model.FigureColor.BLACK,HeroType.IZBA_ROOK);
        d.finishPicks(model.FigureColor.WHITE);d.finishPicks(model.FigureColor.BLACK);
        Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);String s=xml(g);
        assertTrue(s.contains("catalogVersion=\"3\""));assertTrue(s.contains("formatVersion=\"7\""));assertEquals(d.getSnapshot(),load(s).getDraft());
        invalid(s.replace("catalogVersion=\"3\"","catalogVersion=\"2\""));
    }
}
