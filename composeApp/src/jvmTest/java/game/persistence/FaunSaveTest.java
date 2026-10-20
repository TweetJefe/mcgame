package game.persistence;
import game.moves.Success;
import game.core.Game;
import game.heroes.*;
import game.heroes.faun.*;
import game.draft.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static game.core.Positions.pos;
import static model.FigureColor.*;
import static org.junit.Assert.*;
public class FaunSaveTest {
    private String xml(Game g)throws Exception{return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String xml)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(xml.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String xml){assertThrows(java.io.IOException.class,()->load(xml));}
    @Test public void invisibleMovingPieceAndItsRemainingLifetimeSurviveLoading()throws Exception {
        Game g=FaunTest.charged();assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("c2"),null,Ability.INVISIBILITY));
        assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos("c2"),pos("d4")));assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer()));
        String s=xml(g);assertTrue(s.contains("formatVersion=\"8\""));Game other=load(s);assertEquals(s,xml(other));assertEquals(g.getFaun(),other.getFaun());
        assertTrue(other.getFaun().hidden(other.getPieceState(pos("d4")),BLACK));
    }
    @Test public void cloudRulesJournalAndOldVersionDowngradesAreValidated()throws Exception {
        Game g=FaunTest.basic();assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("f4"),Ability.CLOUD));String s=xml(g);assertEquals(s,xml(load(s)));
        invalid(s.replace("formatVersion=\"8\"","formatVersion=\"7\""));invalid(s.replaceAll("(?s)<faunRules [^>]*/>",""));
        invalid(s.replace("cloudTurns=\"2\"","cloudTurns=\"0\""));invalid(s.replace("cloudCooldown=\"3\"","cloudCooldown=\"1\""));
        invalid(s.replace("kind=\"CLOUD\"","kind=\"INVISIBILITY\""));invalid(s.replace("to=\"f4\"","to=\"h8\""));
    }
    @Test public void newDraftUsesVersionEightEvenWhenFaunIsBannedAndOlderCatalogStaysSeven()throws Exception {
        for(int catalog:new int[]{3,4}) {
            var d=new Draft(new DraftRules(1,6,catalog));d.ban(WHITE,catalog==4?HeroType.FAUN_KNIGHT:HeroType.GINSENG_BISHOP);d.ban(BLACK,HeroType.IZBA_ROOK);d.finishPicks(WHITE);d.finishPicks(BLACK);
            Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);String s=xml(g);
            assertTrue(s.contains("formatVersion=\""+(catalog==4?8:7)+"\""));assertEquals(s,xml(load(s)));assertEquals(d.getSnapshot(),load(s).getDraft());
            if(catalog==4)invalid(s.replace("catalogVersion=\"4\"","catalogVersion=\"3\""));
        }
    }
}
