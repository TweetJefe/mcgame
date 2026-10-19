package game.persistence;
import game.core.*;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.heroes.chronomlin.*;
import game.draft.*;
import model.figures.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static game.core.Positions.pos;
import static game.heroes.chronomlin.ChronomlinTest.*;
import static game.heroes.moleminer.MoleminerTest.move;
import static game.heroes.moleminer.MoleminerTest.end;
import static model.FigureColor.*;
import static org.junit.Assert.*;
public class ChronomlinSaveTest {
    private String xml(Game g)throws Exception{return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String s)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String s){assertThrows(java.io.IOException.class,()->load(s));}
    @Test public void grappleLandingCooldownAndIdentityRoundtripExactly()throws Exception{
        Game g=ready();long id=g.getPieceState(pos("c2")).id();grapple(g,"c2","c6");String s=xml(g);assertTrue(s.contains("formatVersion=\"13\""));g=load(s);assertEquals(s,xml(g));assertEquals(id,g.getPieceState(pos("c5")).id());assertEquals(3,g.getPieceState(id).cooldown());cycle(g);evolve(g,"c5");assertEquals(Bishop.class,g.getPieceState(id).type());AresTest.roundtrip(g);
    }
    @Test public void evolutionChainResumesAfterLoadingWithoutResettingFormOrHero()throws Exception{
        Game g=ready();evolve(g,"c2");g=load(xml(g));cycle(g);g=AresTest.charge(g,"c2",1);evolve(g,"c2");g=load(xml(g));assertEquals(Knight.class,g.getPieceState(pos("c2")).type());cycle(g);g=AresTest.charge(g,"c2",1);evolve(g,"c2");g=load(xml(g));assertEquals(Rook.class,g.getPieceState(pos("c2")).type());move(g,"c2","c3");end(g);AresTest.roundtrip(g);
    }
    @Test public void evolvedGinsengAndItsNewAbilityCopyRoundtrip()throws Exception{
        Game g=ginsengReady();long id=g.getPieceState(pos("e2")).id();borrowed(g,id,Ability.EVOLVE,null);String s=xml(g);g=load(s);assertEquals(s,xml(g));assertEquals(Knight.class,g.getPieceState(id).type());cycle(g);g=AresTest.charge(g,"e2",2);g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("c2"),Ability.COPY_SPELL);g=load(xml(g));cycle(g);borrowed(g,id,Ability.EVOLVE,null);assertEquals(Rook.class,g.getPieceState(id).type());AresTest.roundtrip(g);
    }
    @Test public void forgedFormMissingRulesAndDowngradeAreRejected()throws Exception{
        Game g=ready();evolve(g,"c2");String s=xml(g);invalid(s.replace("formatVersion=\"13\"","formatVersion=\"12\""));invalid(s.replaceAll("(?s)<chronomlinRules [^>]*/>",""));invalid(s.replace("type=\"Bishop\"","type=\"Rook\""));invalid(s.replace("kind=\"EVOLVE\"","kind=\"STEAL\""));
    }
    @Test public void currentCatalogNeedsThirteenWhilePreviousCatalogStillUsesTwelve()throws Exception{
        for(int version:new int[]{8,9}){var d=new Draft(new DraftRules(1,6,version));d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);d.finishPicks(WHITE);d.finishPicks(BLACK);String s=xml(Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L));assertTrue(s.contains("formatVersion=\""+(version==9?13:12)+"\""));assertEquals(s,xml(load(s)));}
    }
    @Test public void promotionAfterAlreadyEvolvingIsRejectedWhenForgedInHistory()throws Exception{
        Game g=ready();evolve(g,"c2");move(g,"c2","d3");String s=xml(g);invalid(s.replace("kind=\"NORMAL\"","kind=\"PROMOTION\" promotion=\"QUEEN\""));assertEquals(s,xml(load(s)));
    }
}
