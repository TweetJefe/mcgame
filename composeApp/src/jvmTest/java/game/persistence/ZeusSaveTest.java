package game.persistence;

import game.heroes.ginseng.GinsengAbility;
import game.core.*;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.heroes.zeus.*;
import game.draft.*;
import game.state.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.ginseng.GinsengFixtures.*;
import static game.heroes.zeus.ZeusTest.*;
import static game.heroes.moleminer.MoleminerTest.cast;
import static model.FigureColor.*;
import static org.junit.Assert.*;
public class ZeusSaveTest {
    private String xml(Game g)throws Exception{return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String s)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String s){assertThrows(java.io.IOException.class,()->load(s));}
    @Test public void midChainChargesExpiryAndRandomStateSurviveLoading()throws Exception{
        Game g=basic();chain(g,"e4");cycle(g);chain(g,"e4");String s=xml(g);assertTrue(s.contains("formatVersion=\"12\""));g=load(s);assertEquals(s,xml(g));assertEquals(2,count(g,"e2","e4"));cycle(g);chain(g,"e4");assertNull(g.getPieceState(pos("e4")));AresTest.roundtrip(g);
    }
    @Test public void copiedLightningPoolsRoundtripIndependently()throws Exception{
        var b=setup();put(b,Bishop.class,WHITE,"c2");put(b,Knight.class,BLACK,"e4");Game g=AresTest.charge(ZeusTest.create(b,Map.of(pos("c2"),HeroType.GINSENG_BISHOP)),"c2",2);chain(g,"e4");cycle(g);cast(g,"c2","e2",GinsengAbility.COPY_SPELL);cycle(g);g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),ZeusAbility.CHAIN_LIGHTNING,pos("e4"),null,true));String s=xml(g);g=load(s);assertEquals(s,xml(g));assertEquals(2,g.getSnapshot().lightning().charges().size());assertEquals(1,count(g,"e2","e4"));assertEquals(1,count(g,"c2","e4"));
    }
    @Test public void forgedLightningSettingsMissingSectionAndDowngradeAreRejected()throws Exception{
        Game g=basic();chain(g,"e4");String s=xml(g);invalid(s.replace("formatVersion=\"12\"","formatVersion=\"11\""));invalid(s.replaceAll("(?s)<zeusRules [^>]*/>",""));invalid(s.replace("lightningCooldown=\"1\"","lightningCooldown=\"0\""));invalid(s.replace("kind=\"LIGHTNING_HIT\"","kind=\"LIGHTNING_RESET\""));
    }
    @Test public void forgedResetWithoutDeathAndForgedKillWithoutThirdChargeAreRejected()throws Exception{
        Game g=basic();chain(g,"e4");cycle(g);chain(g,"e4");cycle(g);chain(g,"e4");String s=xml(g);invalid(s.replaceFirst("kind=\"LIGHTNING_HIT\"","kind=\"LIGHTNING_DISCHARGE\""));invalid(s.replace("kind=\"LIGHTNING_CAPTURE\"","kind=\"COUNTER_CAPTURE\""));assertEquals(s,xml(load(s)));
    }
    @Test public void currentCatalogUsesTwelveAndPreviousCatalogRetainsEleven()throws Exception{
        for(int catalog:new int[]{7,8}){var d=new Draft(new DraftRules(1,6,catalog));d.ban(WHITE,HeroType.MOLEMINER_PAWN);d.ban(BLACK,HeroType.IZBA_ROOK);d.finishPicks(WHITE);d.finishPicks(BLACK);String s=xml(Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L));assertTrue(s.contains("formatVersion=\""+(catalog==8?12:11)+"\""));assertEquals(s,xml(load(s)));}
    }
    @Test public void snapshotCannotReplaceIndependentChargesWithACombinedPool(){
        Game g=basic();chain(g,"e4");var s=g.getSnapshot();var keys=s.lightning().charges().keySet();var forged=new LightningState(Map.of(keys.iterator().next(),new LightningState.Charge(2,5)),s.lightning().pulses());var altered=new GameSnapshot(s.version(),s.mode(),s.field(),s.pieces(),s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),s.summons(),s.spells(),s.faun(),s.clown(),s.mines(),forged);assertThrows(IllegalArgumentException.class,()->Game.fromSnapshot(altered,()->0L));
    }
}
