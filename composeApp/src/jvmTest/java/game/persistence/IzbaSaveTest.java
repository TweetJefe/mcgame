package game.persistence;

import game.moves.Success;
import game.moves.TurnRejection;
import game.core.Game;
import game.heroes.*;
import game.heroes.izba.IzbaFixtures;
import game.moves.*;
import game.summoning.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static game.core.Positions.pos;
import static org.junit.Assert.*;

public class IzbaSaveTest {
    private Game spawned() { Game g=IzbaFixtures.standard(55);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("e2"),null,Ability.SUMMON_YAGA));return g; }
    private String xml(Game g)throws Exception {return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String s)throws Exception {return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String s) {assertThrows(java.io.IOException.class,()->GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)));}
    @Test public void formatSixStoresLifetimeLineageAndRandomnessExactly()throws Exception {
        Game g=spawned();String xml=xml(g);assertTrue(xml.contains("formatVersion=\"6\""));Game copy=load(xml);
        assertEquals(xml,xml(copy));assertEquals(g.getSummons(),copy.getSummons());
        assertTrue(copy.getTurnState().abilityUsed());assertEquals(TurnRejection.ABILITY_ALREADY_USED,copy.useAbility(copy.getCurrentPlayer(),pos("e2"),null,Ability.SUMMON_YAGA));
    }
    @Test public void rejectsMissingDuplicateForgedAndAlreadyOverdueOrigin()throws Exception {
        String s=xml(spawned());invalid(s.replaceAll("(?s)<origin [^>]*/>",""));
        invalid(s.replace("transformsAfterTurn=\"7\"","transformsAfterTurn=\"5\""));
        invalid(s.replace("transformed=\"false\"","transformed=\"true\""));
        invalid(s.replaceAll("sourceId=\"[0-9]+\"","sourceId=\"99999\""));
        var m=java.util.regex.Pattern.compile("<origin [^>]*/>").matcher(s);assertTrue(m.find());invalid(s.replace(m.group(),m.group()+m.group()));
        invalid(s.replaceAll("(?s)<summons .*?</summons>",""));invalid(s.replace("enabled=\"true\" randomState", "enabled=\"false\" randomState"));
    }
    @Test public void refusesDowngradeAndInconsistentHeroFormOrBirthEvent()throws Exception {
        String s=xml(spawned());invalid(s.replace("formatVersion=\"6\"","formatVersion=\"5\""));
        invalid(s.replace("BABA_YAGA_PAWN","NONE"));invalid(s.replaceAll("(?s)<event [^>]*kind=\"YAGA_BIRTH\"[^>]*/>",""));
        invalid(s.replace("kind=\"YAGA_BIRTH\"","kind=\"YAGA_MATURE\""));
    }
    @Test public void currentCatalogIsVersionedEvenWhenIzbaWasBanned()throws Exception {
        var d=new game.draft.Draft(new game.draft.DraftRules(1,6,2));d.ban(model.FigureColor.WHITE,HeroType.IZBA_ROOK);d.ban(model.FigureColor.BLACK,HeroType.PUDGE_ROOK);
        d.finishPicks(model.FigureColor.WHITE);d.finishPicks(model.FigureColor.BLACK);
        Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);String s=xml(g);
        assertTrue(s.contains("catalogVersion=\"2\""));assertTrue(s.contains("formatVersion=\"6\""));assertEquals(d.getSnapshot(),load(s).getDraft());
        invalid(s.replace("catalogVersion=\"2\"","catalogVersion=\"1\""));
    }
}
