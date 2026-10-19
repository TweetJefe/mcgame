package game.persistence;
import game.core.*;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.heroes.moleminer.MoleminerTest;
import game.draft.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.ginseng.GinsengFixtures.*;
import static game.heroes.moleminer.MoleminerTest.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;
public class MoleminerSaveTest{
    private String xml(Game g)throws Exception{return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String s)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String s){assertThrows(java.io.IOException.class,()->load(s));}
    @Test public void persistentMineLoadsAndStillTriggersAfterItsOwnerDies()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Rook.class,BLACK,"c8");Game g=createMiner(b,Map.of());plant(g);move(g,"c2","c3");end(g);move(g,"c8","c3");end(g);String s=xml(g);assertTrue(s.contains("formatVersion=\"11\""));g=load(s);assertEquals(s,xml(g));move(g,"b1","a3");end(g);move(g,"c3","c2");assertTrue(g.getSnapshot().mines().cells().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void explosionFootprintFriendlyDeathAndRandomStateRoundtripExactly()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Rook.class,WHITE,"e4");Game g=AresTest.charge(createMiner(b,Map.of()),"c2",2);cast(g,"c2","e4",Ability.DETONATE);String s=xml(g);assertTrue(s.contains("DESTROYED"));assertEquals(s,xml(load(s)));invalid(s.replace("kind=\"BLAST_AREA\"","kind=\"STUN\""));invalid(s.replace("kind=\"BLAST_DEATH\"","kind=\"BLAST_CAPTURE\""));invalid(s.replace("status=\"DESTROYED\"","status=\"CAPTURED\""));
    }
    @Test public void forgedMineOriginSettingsAndOldFormatAreRejected()throws Exception{
        Game g=basic();plant(g);String s=xml(g);invalid(s.replace("formatVersion=\"11\"","formatVersion=\"10\""));invalid(s.replaceAll("(?s)<moleminerRules [^>]*/>",""));invalid(s.replace("mineCooldown=\"3\"","mineCooldown=\"1\""));invalid(s.replace("kind=\"PLANT_MINE\"","kind=\"INVISIBILITY\""));
    }
    @Test public void currentCatalogueUsesElevenAndPreviousCatalogueStillUsesTen()throws Exception{
        for(int catalog:new int[]{6,7}){Draft d=new Draft(new DraftRules(1,6,catalog));d.ban(WHITE,catalog==7?HeroType.MOLEMINER_PAWN:HeroType.CLOWNGANGER_KNIGHT);d.ban(BLACK,HeroType.IZBA_ROOK);d.finishPicks(WHITE);d.finishPicks(BLACK);Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);String s=xml(g);assertTrue(s.contains("formatVersion=\""+(catalog==7?11:10)+"\""));assertEquals(s,xml(load(s)));}
    }
}
