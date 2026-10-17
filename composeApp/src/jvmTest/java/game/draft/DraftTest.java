package game.draft;

import game.heroes.HeroType;
import model.Position;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static game.draft.DraftActionResult.*;
import static game.heroes.HeroType.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class DraftTest {
    private Draft picks() {
        Draft d=new Draft(new DraftRules(1,6,1));
        assertEquals(SUCCESS,d.ban(WHITE,BOUNTY_CROW_PAWN));
        assertEquals(SUCCESS,d.ban(BLACK,BOLOTNIK_KNIGHT));return d;
    }
    @Test public void orderedBansAndUniquePicksCompleteWhenPoolIsExhausted() {
        Draft d=picks();assertEquals(DraftPhase.PICKS,d.getSnapshot().phase());assertEquals(WHITE,d.getSnapshot().active());
        assertEquals(SUCCESS,d.pick(WHITE,PUDGE_ROOK,pos("h1")));
        assertEquals(HERO_PICKED,d.pick(BLACK,PUDGE_ROOK,pos("a8")));
        assertEquals(SUCCESS,d.pick(BLACK,LUMESHARD_BISHOP,pos("f8")));
        assertTrue(d.getSnapshot().isComplete());assertEquals(model.FigureColor.NONE,d.getSnapshot().active());
        assertEquals(Map.of(pos("h1"),PUDGE_ROOK,pos("f8"),LUMESHARD_BISHOP),d.getSnapshot().assignments());
        assertEquals(DRAFT_COMPLETE,d.finishPicks(WHITE));assertEquals(DRAFT_COMPLETE,d.ban(BLACK,PUDGE_ROOK));
        assertEquals(d.getSnapshot(),Draft.fromSnapshot(d.getSnapshot()).getSnapshot());
    }
    @Test public void invalidCommandsAreAtomicAndCannotSkipTheTurn() {
        Draft d=new Draft(new DraftRules(1,6,1));var initial=d.getSnapshot();
        assertEquals(WRONG_SIDE,d.ban(BLACK,PUDGE_ROOK));assertEquals(WRONG_PHASE,d.finishPicks(WHITE));
        assertEquals(WRONG_PHASE,d.pick(WHITE,PUDGE_ROOK,pos("h1")));
        assertEquals(INVALID_HERO,d.ban(WHITE,null));assertEquals(INVALID_HERO,d.ban(WHITE,HeroType.NONE));
        assertEquals(initial,d.getSnapshot());assertEquals(SUCCESS,d.ban(WHITE,BOUNTY_CROW_PAWN));
        var afterBan=d.getSnapshot();assertEquals(HERO_BANNED,d.ban(BLACK,BOUNTY_CROW_PAWN));assertEquals(afterBan,d.getSnapshot());
        assertEquals(SUCCESS,d.ban(BLACK,BOLOTNIK_KNIGHT));var beforePick=d.getSnapshot();
        assertEquals(HERO_BANNED,d.pick(WHITE,BOUNTY_CROW_PAWN,pos("b2")));
        for(Position p:Arrays.asList(null,pos("a8"),pos("b1"),pos("a2"),new Position(-1,0),new Position(0,8)))
            assertEquals(INVALID_SQUARE,d.pick(WHITE,PUDGE_ROOK,p));
        assertEquals(WRONG_SIDE,d.pick(BLACK,PUDGE_ROOK,pos("a8")));assertEquals(beforePick,d.getSnapshot());
    }
    @Test public void passingEndsOnlyThatSidesPicksAndOtherSideCanPickAgain() {
        Draft d=picks();assertEquals(SUCCESS,d.finishPicks(WHITE));
        assertTrue(d.getAvailableHeroes(WHITE).isEmpty());assertTrue(d.getPlacementTargets(WHITE,PUDGE_ROOK).isEmpty());
        assertEquals(SUCCESS,d.pick(BLACK,PUDGE_ROOK,pos("h8")));assertEquals(BLACK,d.getSnapshot().active());
        assertEquals(SUCCESS,d.pick(BLACK,LUMESHARD_BISHOP,pos("c8")));assertTrue(d.getSnapshot().isComplete());
        assertTrue(d.getSnapshot().picksFor(WHITE).isEmpty());assertEquals(2,d.getSnapshot().picksFor(BLACK).size());
    }
    @Test public void bothSidesCanPassAndPlayOrdinaryPieces() {
        Draft d=picks();d.finishPicks(WHITE);d.finishPicks(BLACK);assertTrue(d.getSnapshot().isComplete());
        assertTrue(d.getSnapshot().assignments().isEmpty());
    }
    @Test public void noBansAndOnePickLimitDoNotRequireExhaustingTheCatalog() {
        Draft d=new Draft(new DraftRules(0,1));assertEquals(DraftPhase.PICKS,d.getSnapshot().phase());
        assertEquals(SUCCESS,d.pick(WHITE,BOUNTY_CROW_PAWN,pos("h2")));
        assertEquals(SUCCESS,d.pick(BLACK,BOLOTNIK_KNIGHT,pos("b8")));assertTrue(d.getSnapshot().isComplete());
        assertEquals(2,d.getSnapshot().assignments().size());
    }
    @Test public void everyHeroCanReplaceAnyCompatibleHomePieceOnly() {
        assertEquals(List.of(pos("a1"),pos("h1")),HeroCatalog.startingSquares(PUDGE_ROOK,WHITE));
        assertEquals(List.of(pos("c8"),pos("f8")),HeroCatalog.startingSquares(LUMESHARD_BISHOP,BLACK));
        assertEquals(List.of(pos("b1"),pos("g1")),HeroCatalog.startingSquares(BOLOTNIK_KNIGHT,WHITE));
        assertEquals(8,HeroCatalog.startingSquares(BOUNTY_CROW_PAWN,BLACK).size());
        assertTrue(HeroCatalog.startingSquares(null,WHITE).isEmpty());assertTrue(HeroCatalog.startingSquares(PUDGE_ROOK,model.FigureColor.NONE).isEmpty());
    }
    @Test public void historyAndRosterAreDeeplyImmutable() {
        Draft d=picks();var before=d.getSnapshot();d.pick(WHITE,PUDGE_ROOK,pos("a1"));
        assertFalse(before.isPicked(PUDGE_ROOK));assertEquals(2,before.events().size());
        var s=d.getSnapshot();assertThrows(UnsupportedOperationException.class,()->s.bans().clear());
        assertThrows(UnsupportedOperationException.class,()->s.picks().clear());
        assertThrows(UnsupportedOperationException.class,()->s.picksFor(WHITE).clear());
        assertThrows(UnsupportedOperationException.class,()->s.events().clear());
        assertThrows(UnsupportedOperationException.class,()->s.finished().add(WHITE));
        assertThrows(UnsupportedOperationException.class,()->s.assignments().clear());
    }
    @Test public void restoreRejectsInventedStateOutOfOrderOrIllegalEvents() {
        Draft d=picks();var s=d.getSnapshot();
        assertThrows(IllegalArgumentException.class,()->Draft.fromSnapshot(new DraftSnapshot(s.version()+1,s.rules(),s.phase(),s.active(),s.bans(),s.picks(),s.finished(),s.events())));
        assertThrows(IllegalArgumentException.class,()->Draft.replay(s.rules(),List.of(new DraftEvent(2,DraftEvent.Kind.BAN,WHITE,PUDGE_ROOK,null))));
        assertThrows(IllegalArgumentException.class,()->Draft.replay(s.rules(),List.of(new DraftEvent(1,DraftEvent.Kind.BAN,BLACK,PUDGE_ROOK,null))));
        var illegal=new ArrayList<>(s.events());illegal.add(new DraftEvent(3,DraftEvent.Kind.PICK,WHITE,PUDGE_ROOK,pos("a8")));
        assertThrows(IllegalArgumentException.class,()->Draft.replay(s.rules(),illegal));
    }
    @Test public void rulesCannotBanTheWholeCatalogOrRequireInvalidRosterSize() {
        assertThrows(IllegalArgumentException.class,()->new DraftRules(3,6));
        assertThrows(IllegalArgumentException.class,()->new DraftRules(-1,6));
        assertThrows(IllegalArgumentException.class,()->new DraftRules(0,0));
        assertThrows(IllegalArgumentException.class,()->new DraftRules(0,15));
    }
}
