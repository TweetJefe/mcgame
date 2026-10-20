package game.persistence;

import game.heroes.ginseng.GinsengAbility;
import game.moves.Success;
import game.core.*;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.heroes.kikimora.*;
import game.draft.*;
import model.figures.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.kikimora.KikimoraTest.*;
import static game.heroes.ginseng.GinsengFixtures.put;
import static model.FigureColor.*;
import static org.junit.Assert.*;
public class KikimoraSaveTest{
    private String xml(Game g)throws Exception{return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String s)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String s){assertThrows(java.io.IOException.class,()->load(s));}
    @Test public void doubleStunKeepsDifferentAllyAndEnemyDeadlines()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Knight.class,WHITE,"e4");Game g=create(b,Map.of());yarn(g,"d3");String s=xml(g);assertTrue(s.contains("formatVersion=\"14\""));g=load(s);assertEquals(s,xml(g));assertEquals(4,g.getPieceState(pos("d3")).stunnedThroughTurn());assertEquals(5,g.getPieceState(pos("e4")).stunnedThroughTurn());cycle(g);cycle(g);assertFalse(g.getPieceState(pos("d3")).isStunned());assertTrue(g.getPieceState(pos("e4")).isStunned());AresTest.roundtrip(g);
    }
    @Test public void potFlagAndIdentitySurviveLoadingAndForcedMovement()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Bishop.class,WHITE,"f2");Game g=AresTest.charge(create(b,Map.of(pos("f2"),HeroType.GINSENG_BISHOP)),"c2",2);long id=g.getPieceState(pos("d3")).id();pot(g,"d3");String s=xml(g);g=load(s);assertEquals(s,xml(g));cycle(g);assertEquals(game.moves.Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("f2"),GinsengAbility.DISPLACE,pos("d3"),pos("e4"),false)));g=load(xml(g));assertEquals(id,g.getPieceState(pos("e4")).id());assertTrue(g.getPieceState(id).isPotted());AresTest.roundtrip(g);
    }
    @Test public void removingPotFlagOrItsHistoryOrRulesIsRejected()throws Exception{
        Game g=ready();pot(g,"d3");String s=xml(g);invalid(s.replace("potted=\"true\"","potted=\"false\""));invalid(s.replace("kind=\"CLAY_POT\"","kind=\"YARN\""));invalid(s.replaceAll("(?s)<kikimoraRules [^>]*/>",""));invalid(s.replace("formatVersion=\"14\"","formatVersion=\"13\""));
    }
    @Test public void forgedStunDurationAndDirectionAreRejected()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Knight.class,BLACK,"e4");Game g=create(b,Map.of());yarn(g,"d3");String s=xml(g);invalid(s.replace("stunnedThroughTurn=\"4\"","stunnedThroughTurn=\"2\""));invalid(s.replaceFirst("kind=\"YARN_STUN\"","kind=\"STUN\""));assertEquals(s,xml(load(s)));
    }
    @Test public void previousCatalogUsesThirteenAndCurrentCatalogUsesFourteen()throws Exception{
        for(int version:new int[]{9,10}){var d=new Draft(new DraftRules(1,6,version));d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);d.finishPicks(WHITE);d.finishPicks(BLACK);String s=xml(Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L));assertTrue(s.contains("formatVersion=\""+(version==10?14:13)+"\""));assertEquals(s,xml(load(s)));}
    }
    @Test public void copiedPotSurvivesLoadBeforeAndAfterUse()throws Exception{
        var b=setup();put(b,Bishop.class,WHITE,"e2");put(b,Knight.class,BLACK,"f3");put(b,Knight.class,BLACK,"d3");Game g=AresTest.charge(AresTest.charge(at(b,Map.of(pos("c2"),HeroType.GINSENG_BISHOP),"e2"),"e2",2),"c2",2);game.heroes.moleminer.MoleminerTest.cast(g,"e2","f3",KikimoraAbility.CLAY_POT);cycle(g);copy(g,"c2","e2");g=load(xml(g));cycle(g);borrowed(g,"c2",KikimoraAbility.CLAY_POT,"d3");g=load(xml(g));assertTrue(g.getPieceState(pos("d3")).isPotted());assertTrue(g.getPieceState(pos("f3")).isPotted());assertTrue(g.getSpells().borrowed().isEmpty());AresTest.roundtrip(g);
    }
}
