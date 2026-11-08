package game.draft;

import game.core.Game;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.persistence.GameSaveFile;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static game.core.Positions.pos;
import static game.draft.MatchDraft.Result.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class MatchDraftTest {
    private static MatchDraft create(AtomicLong time) { return new MatchDraft(HeroRules.defaults(),time::get); }
    private static void seconds(AtomicLong time,long seconds) { time.addAndGet(Duration.ofSeconds(seconds).toNanos()); }
    private static void act(MatchDraft d,HeroType hero) {
        var step=d.getSnapshot().expected();
        assertEquals(SUCCESS,step.kind()==MatchDraft.Kind.BAN ? d.ban(step.side(),hero) : d.pick(step.side(),hero));
    }
    private static void fill(MatchDraft d) {
        while(d.getSnapshot().phase()==MatchDraft.Phase.SELECTION) act(d,d.availableHeroes().getFirst());
    }
    @Test public void exactSequenceAndFiveRequiredHeroesPerSide() {
        var time=new AtomicLong(); var d=create(time);
        assertEquals(List.of("WB","BB","BP","WP","WP","BP","BB","WB","WP","BP","BP","WP","WB","BB","WP","BP"),
                MatchDraft.order().stream().map(s->(s.side()==WHITE?"W":"B")+(s.kind()==MatchDraft.Kind.BAN?"B":"P")).toList());
        fill(d); var s=d.getSnapshot(); assertEquals(MatchDraft.Phase.PREPARATION,s.phase());
        assertEquals(6,s.bans().size()); assertEquals(16,s.actions().size());
        for(var side:List.of(WHITE,BLACK)) for(var role:DraftRole.values())
            assertEquals(role.required(),s.rosters().get(side).stream().filter(h->DraftRole.of(s.catalog().definition(h))==role).count());
        assertTrue(s.assignments().isEmpty());
    }
    @Test public void cannotBanLastTwoNeededKnights() {
        var d=create(new AtomicLong()); act(d,HeroType.FAUN); act(d,HeroType.BOLOTNIK);
        act(d,HeroType.BOUNTY_CROW); act(d,HeroType.MOLEMINER); act(d,HeroType.LUMESHARD); act(d,HeroType.GINSENG);
        var before=d.getSnapshot();
        assertEquals(ROSTER_WOULD_BE_IMPOSSIBLE,d.ban(BLACK,HeroType.CLOWNGANGER));
        assertEquals(before,d.getSnapshot()); assertFalse(d.availableHeroes().contains(HeroType.PHOENIX));
    }
    @Test public void cannotPickSecondKnight() {
        var d=create(new AtomicLong()); act(d,HeroType.SHIVA); act(d,HeroType.ZEUS);
        act(d,HeroType.FAUN); act(d,HeroType.CLOWNGANGER);
        assertEquals(ROSTER_WOULD_BE_IMPOSSIBLE,d.pick(WHITE,HeroType.PHOENIX));
    }
    @Test public void cannotPickThirdPawn() {
        var d=create(new AtomicLong()); act(d,HeroType.SHIVA); act(d,HeroType.ZEUS);
        act(d,HeroType.FAUN); act(d,HeroType.BOUNTY_CROW); act(d,HeroType.MOLEMINER); act(d,HeroType.GINSENG);
        act(d,HeroType.ARES); act(d,HeroType.BOLOTNIK);
        assertEquals(ROSTER_WOULD_BE_IMPOSSIBLE,d.pick(WHITE,HeroType.CHRONOMLIN));
    }
    @Test public void lastKnightIsProtectedWhenOnlyOneSideStillNeedsIt() {
        var d=create(new AtomicLong()); act(d,HeroType.FAUN); act(d,HeroType.MOLEMINER);
        act(d,HeroType.BOUNTY_CROW); act(d,HeroType.CLOWNGANGER); act(d,HeroType.LUMESHARD); act(d,HeroType.GINSENG);
        act(d,HeroType.BOLOTNIK);
        assertEquals(ROSTER_WOULD_BE_IMPOSSIBLE,d.ban(WHITE,HeroType.PHOENIX));
    }
    @Test public void rookReserveIsProtectedToo() {
        var d=create(new AtomicLong()); act(d,HeroType.SHIVA); act(d,HeroType.ZEUS);
        act(d,HeroType.BOUNTY_CROW); act(d,HeroType.MOLEMINER); act(d,HeroType.LUMESHARD); act(d,HeroType.GINSENG);
        assertEquals(ROSTER_WOULD_BE_IMPOSSIBLE,d.ban(BLACK,HeroType.PUDGE));
    }
    @Test public void reservePersistsAndBasicTimeResetsForConsecutiveActions() {
        var t=new AtomicLong(); var d=create(t); seconds(t,30); act(d,HeroType.SHIVA);
        assertEquals(Duration.ofSeconds(50).toNanos(),d.getSnapshot().reserveNanos().get(WHITE).longValue());
        act(d,HeroType.ZEUS); act(d,HeroType.FAUN); act(d,HeroType.BOUNTY_CROW);
        assertEquals(WHITE,d.getSnapshot().expected().side());
        assertEquals(Duration.ofSeconds(20).toNanos(),d.getSnapshot().actionNanos());
        seconds(t,25); act(d,HeroType.LUMESHARD);
        assertEquals(Duration.ofSeconds(45).toNanos(),d.getSnapshot().reserveNanos().get(WHITE).longValue());
    }
    @Test public void snapshotDoesNotDoubleSpendReserve() {
        var t=new AtomicLong(); var d=create(t); seconds(t,30);
        var first=d.getSnapshot(); assertEquals(first,d.getSnapshot());
        assertEquals(Duration.ofSeconds(50).toNanos(),first.reserveNanos().get(WHITE).longValue());
        assertEquals(0,d.tick().actions().size());
    }
    @Test public void expiredCommandCannotAccidentallyMakeTheNextConsecutivePick() {
        var t=new AtomicLong(); var d=create(t); act(d,HeroType.SHIVA); act(d,HeroType.ZEUS); act(d,HeroType.FAUN);
        seconds(t,80);
        assertEquals(ACTION_EXPIRED,d.pick(WHITE,HeroType.BOUNTY_CROW));
        assertEquals(4,d.getSnapshot().actions().size()); assertEquals(4,d.getSnapshot().step());
        assertEquals(WHITE,d.getSnapshot().expected().side());
    }
    @Test public void timeoutAtExactBoundaryAutomaticallyActsAndConsumesReserve() {
        var t=new AtomicLong(); var d=create(t); seconds(t,79); assertEquals(0,d.tick().actions().size());
        seconds(t,1); var s=d.tick(); assertEquals(1,s.actions().size()); assertTrue(s.actions().getFirst().automatic());
        assertEquals(0,s.reserveNanos().get(WHITE).longValue()); assertEquals(BLACK,s.expected().side());
        assertEquals(Duration.ofSeconds(20).toNanos(),s.actionNanos());
    }
    @Test public void delayedTickCompletesWholeDraftAndPreparationWithoutRestartingClocks() {
        var t=new AtomicLong(); var d=create(t); seconds(t,1000); var s=d.tick();
        assertEquals(MatchDraft.Phase.COMPLETE,s.phase()); assertEquals(10,s.assignments().size());
        assertEquals(16,s.actions().size()); assertTrue(s.actions().stream().allMatch(MatchDraft.Action::automatic));
        assertEquals(s,d.tick());
    }
    @Test public void preparationHasItsOwnFifteenSecondsAndEnforcesPlacement() {
        var t=new AtomicLong(); var d=create(t); fill(d);
        var hero=d.getSnapshot().rosters().get(WHITE).getFirst(); var cells=d.placementTargets(WHITE,hero);
        assertFalse(cells.isEmpty()); assertEquals(INVALID_SQUARE,d.place(WHITE,hero,pos("d4")));
        assertEquals(NOT_SELECTED,d.place(BLACK,hero,cells.getFirst()));
        assertEquals(SUCCESS,d.place(WHITE,hero,cells.getFirst()));
        seconds(t,14); assertEquals(MatchDraft.Phase.PREPARATION,d.tick().phase());
        assertThrows(IllegalStateException.class,d::completedDraft);
        seconds(t,1); assertEquals(MatchDraft.Phase.COMPLETE,d.tick().phase());
        assertEquals(hero,d.getSnapshot().assignments().get(cells.getFirst()));
    }
    @Test public void positionsAreAssignedOnlyAfterSelectionAndCanBeChanged() {
        var d=create(new AtomicLong()); assertTrue(d.placementTargets(WHITE,HeroType.PUDGE).isEmpty()); fill(d);
        var hero=d.getSnapshot().rosters().get(WHITE).getFirst(); var cells=d.placementTargets(WHITE,hero);
        assertEquals(SUCCESS,d.place(WHITE,hero,cells.getFirst()));
        assertEquals(SUCCESS,d.clearPlacement(WHITE,hero)); assertTrue(d.getSnapshot().assignments().isEmpty());
        assertEquals(SUCCESS,d.place(WHITE,hero,cells.getLast()));
    }
    @Test public void completedDraftStartsGameAndRoundTripsExistingSaveFormat() throws Exception {
        var t=new AtomicLong(); var d=create(t); fill(d); seconds(t,15); d.tick();
        var draft=d.completedDraft(); assertTrue(draft.isComplete());
        var game=Game.mobaFromDraft(d,600,()->0L);
        var copy=Game.fromSnapshot(GameSaveFile.decode(GameSaveFile.encode(game.getSnapshot())),()->0L);
        assertEquals(draft,copy.getDraft()); assertEquals(d.getSnapshot().assignments(),copy.getDraft().assignments());
    }
    @Test public void invalidCommandsDoNotChangeSelectionAndSnapshotsAreImmutable() {
        var d=create(new AtomicLong()); var before=d.getSnapshot();
        assertEquals(WRONG_SIDE,d.ban(BLACK,HeroType.PUDGE)); assertEquals(WRONG_PHASE,d.pick(WHITE,HeroType.PUDGE));
        assertEquals(UNAVAILABLE_HERO,d.ban(WHITE,HeroType.BABA_YAGA)); assertEquals(before,d.getSnapshot());
        assertThrows(UnsupportedOperationException.class,()->before.rosters().get(WHITE).add(HeroType.PUDGE));
    }
}
