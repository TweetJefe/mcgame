package game.persistence;

import game.core.Game;
import game.draft.*;
import game.heroes.*;
import game.moves.MoveResult;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static game.core.Positions.pos;
import static game.heroes.HeroType.*;
import static org.junit.Assert.*;

public class DraftSaveTest {
    private Game game() {return Game.mobaFromDraft(DraftGameTest.selected(PUDGE_ROOK,"h1",LUMESHARD_BISHOP,"f8"),HeroRules.prototype(),600,()->0L);}
    private String xml(Game g)throws Exception {return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String s)throws Exception {return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String s) {assertThrows(java.io.IOException.class,()->GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)));}
    @Test public void completedDraftRoundtripsExactlyAndMidTurnCanContinue()throws Exception {
        Game g=game();var draft=g.getDraft();assertEquals(MoveResult.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos("h2"),pos("h4")));
        String s=xml(g);assertTrue(s.contains("formatVersion=\"5\""));Game restored=load(s);
        assertEquals(s,xml(restored));assertEquals(draft,restored.getDraft());assertTrue(restored.getTurnState().moveUsed());
        assertEquals(MoveResult.SUCCESS,restored.endTurn(restored.getCurrentPlayer()));
        assertEquals(MoveResult.SUCCESS,restored.moveFigure(restored.getCurrentPlayer(),pos("e7"),pos("e5")));
        assertEquals(draft,load(xml(restored)).getDraft());
    }
    @Test public void rejectsMissingUnfinishedDuplicateUnknownAndOutOfOrderDraft()throws Exception {
        String s=xml(game());invalid(s.replaceAll("(?s)<draft .*?</draft>",""));
        invalid(s.replaceFirst("<action [^>]*kind=\"PICK\"[^>]*/>",""));
        invalid(s.replace("sequence=\"2\"","sequence=\"1\""));
        invalid(s.replace("kind=\"BAN\"","kind=\"UNKNOWN\""));
        invalid(s.replace("square=\"h1\"","square=\"a8\""));
        invalid(s.replace("formatVersion=\"5\"","formatVersion=\"4\""));
        var m=java.util.regex.Pattern.compile("(?s)<draft .*?</draft>").matcher(s);assertTrue(m.find());invalid(s.replace(m.group(),m.group()+m.group()));
    }
    @Test public void rejectsHistoryWithDifferentHeroRosterOrOriginalBishopColor()throws Exception {
        String s=xml(game());
        // Only the draft action changes: the live board still has Pudge on h1 and Lumeshard's original f8 color.
        invalid(s.replaceAll("(<action [^>]*hero=\")PUDGE_ROOK", "$1BOLOTNIK_KNIGHT"));
        invalid(s.replaceAll("(<action [^>]*square=\")f8", "$1c8"));
    }
    @Test public void draftedBolotnikAndCrowUseFormatFiveWithoutRequiringLumeshard()throws Exception {
        Game g=Game.mobaFromDraft(DraftGameTest.selected(BOUNTY_CROW_PAWN,"h2",BOLOTNIK_KNIGHT,"b8"),HeroRules.prototype(),600,()->0L);
        String s=xml(g);assertTrue(s.contains("formatVersion=\"5\""));assertEquals(s,xml(load(s)));
    }
}
