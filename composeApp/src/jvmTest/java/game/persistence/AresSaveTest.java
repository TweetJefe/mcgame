package game.persistence;
import game.moves.StatusRejection;
import game.moves.Success;
import game.core.Game;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.draft.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static game.core.Positions.pos;
import static model.FigureColor.*;
import static org.junit.Assert.*;
public class AresSaveTest {
    private String xml(Game g)throws Exception{return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String s)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String s){assertThrows(java.io.IOException.class,()->load(s));}
    @Test public void stunAndSpearCooldownSurviveLoadingAndExpireAtTheCorrectBoundary()throws Exception {
        Game g=AresTest.collision();assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("f2"),Ability.SPEAR));String s=xml(g);assertTrue(s.contains("formatVersion=\"9\""));Game other=load(s);assertEquals(s,xml(other));assertTrue(other.getPieceState(pos("e2")).isStunned());
        assertEquals(Success.SUCCESS,other.moveFigure(other.getCurrentPlayer(),pos("b1"),pos("a3")));assertEquals(Success.SUCCESS,other.endTurn(other.getCurrentPlayer()));
        assertEquals(StatusRejection.STUNNED,other.getAbilityAvailability(pos("f2"),Ability.HOOK));assertEquals(Success.SUCCESS,other.moveFigure(other.getCurrentPlayer(),pos("g8"),pos("f6")));assertEquals(Success.SUCCESS,other.endTurn(other.getCurrentPlayer()));assertFalse(other.getPieceState(pos("e2")).isStunned());AresTest.roundtrip(other);
    }
    @Test public void tamperedStunRulesEventsAndVersionAreRejected()throws Exception {
        Game g=AresTest.collision();g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("f2"),Ability.SPEAR);String s=xml(g);
        invalid(s.replace("formatVersion=\"9\"","formatVersion=\"8\""));invalid(s.replaceAll("(?s)<aresRules [^>]*/>",""));
        invalid(s.replace("stunnedThroughTurn=\"2\"","stunnedThroughTurn=\"9\""));invalid(s.replace("stunnedThroughTurn=\"2\"","stunnedThroughTurn=\"0\""));
        invalid(s.replaceAll("(?s)<event [^>]*kind=\"STUN\"[^>]*/>",""));invalid(s.replace("spearCooldown=\"3\"","spearCooldown=\"1\""));
    }
    @Test public void theNewCatalogUsesXmlNineEvenWhenAresIsBannedAndPreviousCatalogRemainsEight()throws Exception {
        for(int catalog:new int[]{4,5}) {
            var d=new Draft(new DraftRules(1,6,catalog));d.ban(WHITE,catalog==5?HeroType.ARES_BISHOP:HeroType.FAUN_KNIGHT);d.ban(BLACK,HeroType.IZBA_ROOK);d.finishPicks(WHITE);d.finishPicks(BLACK);
            Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);String s=xml(g);assertTrue(s.contains("formatVersion=\""+(catalog==5?9:8)+"\""));assertEquals(s,xml(load(s)));assertEquals(d.getSnapshot(),load(s).getDraft());
            if(catalog==5)invalid(s.replace("catalogVersion=\"5\"","catalogVersion=\"4\""));
        }
    }
}
