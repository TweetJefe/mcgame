package game.persistence;

import game.heroes.clownganger.ClowngangerAbility;
import game.moves.TurnRejection;
import game.core.*;
import game.heroes.*;
import game.draft.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static game.core.Positions.pos;
import static game.heroes.clownganger.ClowngangerTest.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;
public class ClowngangerSaveTest {
    private String xml(Game g)throws Exception{return new String(GameSaveFile.encode(g.getSnapshot()),StandardCharsets.UTF_8);}
    private Game load(String s)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(s.getBytes(StandardCharsets.UTF_8)),()->0L);}
    private void invalid(String s){assertThrows(java.io.IOException.class,()->load(s));}
    @Test public void midTurnSavePreservesTheRemainingMoveOfTheOtherBody()throws Exception{
        Game g=withIllusion();move(g,"c2","e3");String s=xml(g);assertTrue(s.contains("formatVersion=\"10\""));g=load(s);assertEquals(s,xml(g));move(g,"d2","f3");String both=xml(g);g=load(both);assertEquals(both,xml(g));assertEquals(TurnRejection.MOVE_ALREADY_USED,g.moveFigure(g.getCurrentPlayer(),pos("e3"),pos("c2")));end(g);assertEquals(xml(g),xml(load(xml(g))));
    }
    @Test public void boxRulesExpiryOriginAndFormatCannotBeForged()throws Exception{
        Game g=basic();cast(g,"c2","a1",ClowngangerAbility.BOX);String s=xml(g);assertEquals(s,xml(load(s)));invalid(s.replace("boxedThroughTurn=\"3\"","boxedThroughTurn=\"5\""));invalid(s.replace("boxedThroughTurn=\"3\"","boxedThroughTurn=\"0\""));invalid(s.replace("formatVersion=\"10\"","formatVersion=\"9\""));invalid(s.replaceAll("(?s)<clowngangerRules [^>]*/>",""));invalid(s.replace("boxTurns=\"1\"","boxTurns=\"2\""));invalid(s.replaceAll(" boxedThroughTurn=\"0\"",""));
    }
    @Test public void dummyWithoutBirthAndDuplicateIdentityAreRejected()throws Exception{
        String s=xml(withIllusion());invalid(s.replaceAll("kind=\"ILLUSION\"","kind=\"TELEPORT\""));invalid(s.replace("hero=\"ILLUSION\"","hero=\"NONE\""));invalid(s.replace("hero=\"ILLUSION\"","hero=\"CLOWNGANGER_KNIGHT\""));
    }
    @Test public void catalogueSixUsesTenEvenWhenClownIsBannedAndFiveStillUsesNine()throws Exception{
        for(int version:new int[]{5,6}){Draft d=new Draft(new DraftRules(1,6,version));d.ban(WHITE,version==6?HeroType.CLOWNGANGER:HeroType.ARES);d.ban(BLACK,HeroType.IZBA);d.finishPicks(WHITE);d.finishPicks(BLACK);Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);String s=xml(g);assertTrue(s.contains("formatVersion=\""+(version==6?10:9)+"\""));assertEquals(s,xml(load(s)));}
    }
}
