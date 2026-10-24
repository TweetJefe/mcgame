package game.draft;

import game.moves.Success;
import game.core.Game;
import game.heroes.*;
import game.state.*;
import model.FigureColor;
import model.Position;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static game.core.Positions.pos;
import static game.heroes.HeroType.*;
import static model.FigureColor.WHITE;
import static model.FigureColor.BLACK;
import static org.junit.Assert.*;

public class DraftGameTest {
    @Test public void ginsengUsesEitherBishopSlotAndCannotShareOneWithLumeshard() {
        for(String square:List.of("c1","f1","c8","f8")) {
            boolean white=square.endsWith("1");var side=white?WHITE:BLACK;
            Draft d=new Draft();assertEquals(10,d.getSnapshot().rules().catalogVersion());
            assertEquals(13,HeroCatalog.heroes().size());
            assertEquals(DraftActionResult.SUCCESS,d.ban(WHITE,PUDGE));
            assertEquals(DraftActionResult.SUCCESS,d.ban(BLACK,IZBA));
            if(!white)assertEquals(DraftActionResult.SUCCESS,d.finishPicks(WHITE));
            assertEquals(DraftActionResult.SUCCESS,d.pick(side,GINSENG,pos(square)));
            if(white)assertEquals(DraftActionResult.SUCCESS,d.finishPicks(BLACK));
            assertEquals(DraftActionResult.SQUARE_OCCUPIED,d.pick(side,LUMESHARD,pos(square)));
            String other=square.startsWith("c")?"f"+square.substring(1):"c"+square.substring(1);
            assertEquals(DraftActionResult.SUCCESS,d.pick(side,LUMESHARD,pos(other)));
            assertEquals(DraftActionResult.SUCCESS,d.finishPicks(side));
            Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);
            var piece=g.getPieceState(pos(square));assertEquals(GINSENG,piece.hero());
            assertEquals(SquareColor.at(pos(square)),piece.startingColor());
            assertEquals(0,piece.ultimateCharge());assertTrue(g.getSpells().borrowed().isEmpty());
            assertEquals(LUMESHARD,g.getPieceState(pos(other)).hero());
            assertEquals(d.getSnapshot(),Game.fromSnapshot(g.getSnapshot(),()->0L).getDraft());
        }
    }
    public static DraftSnapshot selected(HeroType white, String whiteSquare, HeroType black, String blackSquare) {
        Draft d=new Draft(new DraftRules(1,6,1));var remaining=HeroCatalog.heroes().stream().filter(h->h!=white && h!=black).toList();
        d.ban(WHITE,remaining.get(0));d.ban(BLACK,remaining.get(1));
        assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,white,pos(whiteSquare)));
        assertEquals(DraftActionResult.SUCCESS,d.pick(BLACK,black,pos(blackSquare)));return d.getSnapshot();
    }
    @Test public void onlyChosenHomePiecesBecomeHeroesAndInitialGameResourcesAreFresh() {
        AtomicLong time=new AtomicLong(99_000_000_000L);
        DraftSnapshot roster=selected(PUDGE,"h1",BOUNTY_CROW,"h7");
        Game g=Game.mobaFromDraft(roster,HeroRules.prototype(),600,time::get);var s=g.getSnapshot();
        assertEquals(32,s.pieces().size());assertEquals(2,s.pieces().values().stream().filter(p->p.hero()!=NONE).count());
        assertEquals(PUDGE,g.getPieceState(pos("h1")).hero());assertEquals(NONE,g.getPieceState(pos("a1")).hero());
        assertEquals(BOUNTY_CROW,g.getPieceState(pos("h7")).hero());assertEquals(NONE,g.getPieceState(pos("b7")).hero());
        assertEquals(CastlingRights.initial(),s.castlingRights());assertEquals(WHITE,s.active());
        assertFalse(s.turn().moveUsed());assertFalse(s.turn().abilityUsed());assertTrue(s.moves().isEmpty());assertTrue(s.events().isEmpty());
        assertEquals(0,s.economy().whiteHalves());assertEquals(0,s.economy().blackHalves());
        for(var p:s.pieces().values()) { assertEquals(0,p.cooldown());assertEquals(0,p.ultimateCharge());assertEquals(3,p.inventory().size()); }
        assertEquals(600_000,s.clock().getWhiteMillis());assertEquals(600_000,s.clock().getBlackMillis());
        time.addAndGet(1_500_000_000L);assertEquals(598_500,g.getClockSnapshot().getWhiteMillis());
        assertEquals(roster,g.getDraft());
    }
    @Test public void lumeshardOriginalColorFollowsChosenBishopForEitherSide() {
        for(String cell:List.of("c1","f1")) {
            Game g=Game.mobaFromDraft(selected(LUMESHARD,cell,BOLOTNIK,"b8"),HeroRules.prototype(),600,()->0L);
            assertEquals(SquareColor.at(pos(cell)),g.getPieceState(pos(cell)).startingColor());
            assertEquals(BOLOTNIK,g.getPieceState(pos("b8")).hero());
        }
        for(String cell:List.of("c8","f8")) {
            Game g=Game.mobaFromDraft(selected(BOUNTY_CROW,"a2",LUMESHARD,cell),HeroRules.prototype(),600,()->0L);
            assertEquals(SquareColor.at(pos(cell)),g.getPieceState(pos(cell)).startingColor());
        }
    }
    @Test public void unfinishedDraftCannotStartAndPassingCanYieldAnOrdinaryMobaBoard() {
        assertThrows(IllegalArgumentException.class,()->Game.mobaFromDraft(new Draft().getSnapshot(),HeroRules.prototype(),600));
        Draft d=new Draft(new DraftRules(1,6,1));d.ban(WHITE,PUDGE);d.ban(BLACK,LUMESHARD);d.finishPicks(WHITE);d.finishPicks(BLACK);
        Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);
        assertEquals(GameMode.MOBA,g.getMode());assertTrue(g.getSnapshot().pieces().values().stream().allMatch(p->p.hero()==NONE));
        assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos("e2"),pos("e4")));
        assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer()));assertEquals(BLACK,g.getSnapshot().active());
        assertEquals(d.getSnapshot(),g.getDraft());
    }
    @Test public void originalRosterSurvivesCapturesAndRestoration() {
        var roster=selected(BOUNTY_CROW,"e2",LUMESHARD,"f8");
        Game g=Game.mobaFromDraft(roster,HeroRules.prototype(),600,()->0L);
        play(g,"e2","e4");play(g,"d7","d5");play(g,"e4","d5");play(g,"d8","d5");
        assertEquals(roster,g.getDraft());var crow=g.getSnapshot().pieces().values().stream().filter(p->p.hero()==BOUNTY_CROW).findFirst().orElseThrow();
        assertEquals(PieceState.Status.CAPTURED,crow.status());
        assertEquals(roster,Game.fromSnapshot(g.getSnapshot(),()->0L).getDraft());
    }
    @Test public void validatorRejectsInventedHeroesAndChangedLumeshardStartingColor() {
        Game g=Game.mobaFromDraft(selected(PUDGE,"h1",LUMESHARD,"f8"),HeroRules.prototype(),600,()->0L);
        var s=g.getSnapshot();var pieces=new HashMap<>(s.pieces());var p=s.pieceAt(pos("a2"));
        pieces.put(p.id(),new PieceState(p.id(),p.type(),p.color(),p.position(),p.status(),BOUNTY_CROW,0,0,p.inventory(),null));
        assertThrows(IllegalArgumentException.class,()->Game.fromSnapshot(copy(s,pieces,s.draft()),()->0L));
        pieces.clear();pieces.putAll(s.pieces());var lume=s.pieceAt(pos("f8"));
        var other=SquareColor.at(pos("c8"));pieces.put(lume.id(),new PieceState(lume.id(),lume.type(),lume.color(),lume.position(),lume.status(),lume.hero(),0,0,lume.inventory(),null,0,other));
        assertThrows(IllegalArgumentException.class,()->Game.fromSnapshot(copy(s,pieces,s.draft()),()->0L));
    }
    public static GameSnapshot copy(GameSnapshot s, Map<Long,PieceState> pieces, DraftSnapshot draft) {
        return new GameSnapshot(s.version(),s.mode(),s.field(),pieces,s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),draft);
    }
    private static void play(Game g,String from,String to) {
        assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos(from),pos(to)));
        assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer()));
    }
}
