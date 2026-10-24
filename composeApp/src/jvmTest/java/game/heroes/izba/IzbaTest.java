package game.heroes.izba;

import game.heroes.pudge.PudgeAbility;
import game.heroes.bolotnik.BolotnikAbility;

import game.core.Game;
import game.draft.*;
import game.heroes.*;
import game.moves.*;
import game.persistence.GameSaveFile;
import game.state.*;
import game.summoning.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.izba.IzbaFixtures.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class IzbaTest {
    private void move(Game g,String a,String b){assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos(a),pos(b)));}
    private void end(Game g){assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer()));}
    private void waitTurn(Game g,String a,String b){move(g,a,b);end(g);}
    private void summon(Game g,String square){assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos(square),null,IzbaAbility.SUMMON_YAGA));}
    private PieceState baby(Game g){return g.getSnapshot().pieces().values().stream().filter(p->p.hero()==HeroType.BABA_YAGA).findFirst().orElseThrow();}
    private Game reload(Game g)throws Exception{return Game.fromSnapshot(GameSaveFile.decode(GameSaveFile.encode(g.getSnapshot())),()->0L);}
    @Test public void onlyIzbaIsDraftableAndTwoRookHomesAreAvailable() {
        assertEquals(5,HeroCatalog.heroes(2).size());assertTrue(HeroCatalog.heroes().contains(HeroType.IZBA));assertFalse(HeroCatalog.heroes().contains(HeroType.BABA_YAGA));
        assertEquals(List.of(pos("a1"),pos("h1")),HeroCatalog.startingSquares(HeroType.IZBA,WHITE));
        Draft d=new Draft();d.ban(WHITE,HeroType.PUDGE);d.ban(BLACK,HeroType.LUMESHARD);
        var before=d.getSnapshot();assertEquals(DraftActionResult.INVALID_HERO,d.pick(WHITE,HeroType.BABA_YAGA,pos("b2")));assertEquals(before,d.getSnapshot());
        assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.IZBA,pos("h1")));d.finishPicks(BLACK);d.finishPicks(WHITE);
        Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);
        assertEquals(HeroType.IZBA,g.getPieceState(pos("h1")).hero());assertTrue(g.getSummons().enabled());
    }
    @Test public void ultimateUsesOneAbilityButDoesNotReplaceMandatoryMoveAndPreviewsDoNotReroll()throws Exception {
        Game g=standard(81);var before=g.getSnapshot();
        for(int i=0;i<3;i++){g.getLegalMoves(pos("e2"));g.getAbilityTargets(pos("e2"),IzbaAbility.SUMMON_YAGA);g.getAbilityAvailability(pos("e2"),IzbaAbility.SUMMON_YAGA);}
        assertEquals(before,g.getSnapshot());assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("e3"),IzbaAbility.SUMMON_YAGA));assertEquals(before,g.getSnapshot());
        Game twin=reload(g);summon(g,"e2");summon(twin,"e2");assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(twin.getSnapshot()));
        assertNotEquals(before.summons().randomState(),g.getSummons().randomState());assertEquals(0,g.getPieceState(pos("e2")).ultimateCharge());
        assertTrue(g.getTurnState().abilityUsed());assertFalse(g.getTurnState().moveUsed());
        assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));
        assertEquals(TurnRejection.ABILITY_ALREADY_USED,g.useAbility(g.getCurrentPlayer(),pos("e2"),null,IzbaAbility.SUMMON_YAGA));
        PieceState p=baby(g);assertEquals(Pawn.class,p.type());assertEquals(WHITE,p.color());assertTrue(p.position().getRow()<2);assertEquals(3,p.inventory().size());
        assertEquals(0,g.getEconomy().whiteHalves());assertEquals(0,g.getEconomy().blackHalves());
    }
    @Test public void chargeComesFromOwnCapturesIncludingPawnsAndNoActiveSkillExists() {
        var b=board("a1","h8");put(b,Rook.class,WHITE,"e2");put(b,Pawn.class,BLACK,"e4");put(b,Pawn.class,BLACK,"e5");put(b,Knight.class,BLACK,"g8");
        Game g=create(b,Map.of(pos("e2"),HeroType.IZBA),WHITE,0);
        assertEquals(AbilityRejection.ULTIMATE_NOT_READY,g.getAbilityAvailability(pos("e2"),IzbaAbility.SUMMON_YAGA));assertEquals(List.of(IzbaAbility.SUMMON_YAGA),HeroAbilities.abilities(HeroType.IZBA));
        move(g,"e2","e4");assertEquals(1,g.getPieceState(pos("e4")).ultimateCharge());end(g);waitTurn(g,"g8","f6");
        move(g,"e4","e5");assertEquals(2,g.getPieceState(pos("e5")).ultimateCharge());summon(g,"e5");assertEquals(0,g.getPieceState(pos("e5")).ultimateCharge());
    }
    @Test public void passiveSpawnsSameSideInEightNeighborRadiusAfterEnemyCapture()throws Exception {
        var b=board("a1","h8");put(b,Rook.class,WHITE,"d4");put(b,Rook.class,BLACK,"d8");put(b,Knight.class,WHITE,"b1");put(b,Knight.class,BLACK,"g8");
        Game g=create(b,Map.of(pos("d4"),HeroType.IZBA),BLACK,5);move(g,"d8","d4");PieceState p=baby(g);
        assertEquals(WHITE,p.color());assertEquals(1,Math.max(Math.abs(p.position().getRow()-3),Math.abs(p.position().getColumn()-3)));
        assertEquals(10,g.getEconomy().blackHalves());assertEquals(0,g.getEconomy().whiteHalves());assertEquals(Rook.class,g.getPieceState(pos("d4")).type());
        assertEquals(6,g.getSummons().records().get(p.id()).transformsAfterTurn());g=reload(g);
        end(g);waitTurn(g,"b1","c3");waitTurn(g,"g8","f6");waitTurn(g,"c3","b1");waitTurn(g,"f6","g8");
        assertEquals(HeroType.BABA_YAGA,g.getSnapshot().pieces().get(p.id()).hero());waitTurn(g,"b1","c3");
        assertEquals(HeroType.IZBA,g.getSnapshot().pieces().get(p.id()).hero());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void crowdedDeathHasNoSpawnAndDoesNotConsumeRandomness()throws Exception {
        var b=board("a1","h8");put(b,Rook.class,WHITE,"d4");put(b,Knight.class,BLACK,"f5");
        for(String p:List.of("c3","d3","e3","c4","e4","c5","d5","e5"))put(b,Pawn.class,WHITE,p);
        Game g=create(b,Map.of(pos("d4"),HeroType.IZBA),BLACK,10);long seed=g.getSummons().randomState();move(g,"f5","d4");
        assertTrue(g.getSummons().records().isEmpty());assertEquals(seed,g.getSummons().randomState());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void maturesOnlyAfterThirdFollowingOwnerTurnAndKeepsIdAndLocation()throws Exception {
        Game g=standard(3);summon(g,"e2");PieceState p=baby(g);long id=p.id();Position cell=p.position();
        assertEquals(7,g.getSummons().records().get(id).transformsAfterTurn());
        waitTurn(g,"b1","c3");waitTurn(g,"g8","f6");waitTurn(g,"c3","b1");waitTurn(g,"f6","g8");
        waitTurn(g,"b1","c3");assertEquals(HeroType.BABA_YAGA,g.getSnapshot().pieces().get(id).hero());
        g=reload(g);waitTurn(g,"g8","f6");waitTurn(g,"c3","b1");PieceState house=g.getSnapshot().pieces().get(id);
        assertEquals(HeroType.IZBA,house.hero());assertEquals(Rook.class,house.type());assertEquals(cell,house.position());assertEquals(0,house.ultimateCharge());
        assertEquals(2,g.getSnapshot().pieces().values().stream().filter(x->x.hero()==HeroType.IZBA).count());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void ultimateAlwaysUsesAnEmptyHomeCellBeforeConsideringReplacement() {
        for(int i=0;i<32;i++) {
            var b=board("a1","h8");put(b,Rook.class,WHITE,"e2");
            for(int r=0;r<2;r++)for(int c=0;c<8;c++){String cell=""+(char)('a'+c)+(r+1);if(!cell.equals("a1") && !cell.equals("e2") && !cell.equals("h2"))put(b,Pawn.class,WHITE,cell);}
            Game g=charge(create(b,Map.of(pos("e2"),HeroType.IZBA),WHITE,i),"e2");summon(g,"e2");
            assertEquals(pos("h2"),baby(g).position());assertTrue(g.getSnapshot().pieces().values().stream().noneMatch(p->p.status()==PieceState.Status.REPLACED));
        }
    }
    @Test public void fullHomeRanksMayReplaceEnemyWithoutGoldChargeOrDeathReaction()throws Exception {
        var b=board("e1","h8");
        for(int r=0;r<2;r++)for(int c=0;c<8;c++){String p=""+(char)('a'+c)+(r+1);if(!p.equals("e1"))put(b,Pawn.class,WHITE,p);}
        put(b,Rook.class,WHITE,"h1");put(b,Rook.class,BLACK,"a1");
        Game g=charge(create(b,Map.of(pos("h1"),HeroType.IZBA,pos("a1"),HeroType.IZBA),WHITE,seedFor(15,0)),"h1");
        long removed=g.getPieceState(pos("a1")).id();summon(g,"h1");
        assertEquals(HeroType.BABA_YAGA,g.getPieceState(pos("a1")).hero());assertEquals(WHITE,g.getPieceState(pos("a1")).color());
        assertEquals(PieceState.Status.REPLACED,g.getSnapshot().pieces().get(removed).status());assertEquals(1,g.getSummons().records().size());
        assertEquals(0,g.getEconomy().whiteHalves());assertTrue(g.getWhitePlayer().getAttackedFigures().isEmpty());assertEquals(0,g.getPieceState(pos("h1")).ultimateCharge());
        assertEquals(King.class,g.getPieceState(pos("e1")).type());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void blackUltimateUsesRanksSevenAndEightAndCanProduceMoreThanEightPawns()throws Exception {
        var b=board("a1","h8");put(b,Rook.class,BLACK,"e7");
        for(String cell:List.of("a7","b7","c7","d7","f7","g7","h7","a6"))put(b,Pawn.class,BLACK,cell);
        Game g=charge(create(b,Map.of(pos("e7"),HeroType.IZBA),BLACK,0),"e7");summon(g,"e7");
        assertTrue(baby(g).position().getRow()>=6);assertEquals(9,g.getSnapshot().pieces().values().stream().filter(p->p.type()==Pawn.class).count());
        assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void aHouseKilledByPudgeCounterCaptureAlsoSummons()throws Exception {
        var b=board("a1","h8");put(b,Rook.class,WHITE,"d2");put(b,Rook.class,BLACK,"d4");put(b,Knight.class,BLACK,"e3");put(b,Knight.class,BLACK,"g8");
        Game g=charge(create(b,Map.of(pos("d2"),HeroType.IZBA,pos("d4"),HeroType.PUDGE),BLACK,0),"d4");
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("d4"),pos("e3"),PudgeAbility.SWALLOW));waitTurn(g,"g8","f6");
        move(g,"d2","d4");assertEquals(HeroType.BABA_YAGA,baby(g).hero());assertEquals(Knight.class,g.getPieceState(pos("d4")).type());
        assertEquals(10,g.getEconomy().whiteHalves());assertEquals(10,g.getEconomy().blackHalves());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void babiesCanMatureInsidePudgeAndReturnAsRooks()throws Exception {
        var b=board("a1","h8");put(b,Rook.class,WHITE,"c2");put(b,Rook.class,WHITE,"e2");put(b,Knight.class,WHITE,"b1");put(b,Knight.class,BLACK,"g8");
        Game g=charge(create(b,Map.of(pos("c2"),HeroType.IZBA,pos("e2"),HeroType.PUDGE),WHITE,seedFor(12,2)),"c2","e2");summon(g,"c2");
        PieceState p=baby(g);assertEquals(pos("e1"),p.position());waitTurn(g,"b1","c3");waitTurn(g,"g8","f6");
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("e1"),PudgeAbility.SWALLOW));waitTurn(g,"c3","b1");waitTurn(g,"f6","g8");
        waitTurn(g,"b1","c3");waitTurn(g,"g8","f6");waitTurn(g,"c3","b1");
        assertEquals(HeroType.IZBA,g.getSnapshot().pieces().get(p.id()).hero());assertEquals(PieceState.Status.SWALLOWED,g.getSnapshot().pieces().get(p.id()).status());
        assertEquals(Long.valueOf(p.id()),g.getPieceState(pos("e2")).storedPieceId());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void draftRosterAndSaveSupportAnExtraSeventeenthFriendlyPiece()throws Exception {
        Draft d=new Draft();d.ban(WHITE,HeroType.PUDGE);d.ban(BLACK,HeroType.LUMESHARD);
        d.pick(WHITE,HeroType.IZBA,pos("h1"));d.finishPicks(BLACK);d.finishPicks(WHITE);
        Game g=charge(Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L),"h1");
        move(g,"f2","f4");summon(g,"h1");assertEquals(pos("f2"),baby(g).position());
        assertEquals(33,g.getSnapshot().pieces().size());assertEquals(17,g.getSnapshot().pieces().values().stream().filter(p->p.color()==WHITE).count());
        assertEquals(d.getSnapshot(),reload(g).getDraft());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void replacedPudgeAndPassengerAreRemovedWithoutCaptureOrRelease()throws Exception {
        var b=board("e1","h8");
        for(int r=0;r<2;r++)for(int c=0;c<8;c++){String cell=""+(char)('a'+c)+(r+1);if(!cell.equals("e1"))put(b,Pawn.class,WHITE,cell);}
        put(b,Rook.class,WHITE,"h1");put(b,Rook.class,WHITE,"b3");put(b,Rook.class,BLACK,"a1");put(b,Pawn.class,BLACK,"b2");put(b,Knight.class,BLACK,"g8");
        Game g=charge(create(b,Map.of(pos("h1"),HeroType.IZBA,pos("a1"),HeroType.PUDGE),BLACK,seedFor(15,0)),"h1","a1");
        long pudge=g.getPieceState(pos("a1")).id(),passenger=g.getPieceState(pos("b2")).id();
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("a1"),pos("b2"),PudgeAbility.SWALLOW));waitTurn(g,"g8","f6");move(g,"b3","b2");summon(g,"h1");
        assertEquals(PieceState.Status.REPLACED,g.getSnapshot().pieces().get(pudge).status());assertEquals(PieceState.Status.REPLACED,g.getSnapshot().pieces().get(passenger).status());
        assertEquals(0,g.getEconomy().whiteHalves());assertEquals(0,g.getEconomy().blackHalves());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void summonedPawnImmediatelyTriggersEnemyMudAndAwardsNormalHeroPawnGold()throws Exception {
        var b=board("h1","h8");put(b,Rook.class,WHITE,"e2");put(b,Knight.class,WHITE,"b1");put(b,Knight.class,BLACK,"g4");put(b,Knight.class,BLACK,"g8");
        // Home squares are sorted by row/file; c2 is the 9th of the 13 empty cells.
        var empty=new ArrayList<Position>();for(int r=0;r<2;r++)for(int c=0;c<8;c++){Position p=new Position(r,c);if(b.get(p) instanceof Empty)empty.add(p);}
        Game g=charge(create(b,Map.of(pos("e2"),HeroType.IZBA,pos("g4"),HeroType.BOLOTNIK),BLACK,seedFor(empty.size(),empty.indexOf(pos("c2")))),"e2");
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("g4"),pos("c2"),BolotnikAbility.MUD));waitTurn(g,"g8","f6");summon(g,"e2");
        assertEquals(HeroType.BOLOTNIK,g.getPieceState(pos("c2")).hero());assertEquals(PieceState.Status.CAPTURED,baby(g).status());
        var born=g.getEvents().stream().filter(e->e.kind()==game.history.GameEvent.Kind.YAGA_BIRTH).findFirst().orElseThrow();
        var killed=g.getEvents().stream().filter(e->e.kind()==game.history.GameEvent.Kind.BOG_CAPTURE).findFirst().orElseThrow();
        assertTrue(born.sequence()<killed.sequence());
        assertEquals(4,g.getEconomy().blackHalves());assertTrue(g.getTerrain().cells().isEmpty());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void deathOnEnemyMudAlsoProducesThePassiveSummon()throws Exception {
        var b=board("h1","h8");put(b,Rook.class,WHITE,"e2");put(b,Knight.class,BLACK,"g4");put(b,Knight.class,BLACK,"g8");
        Game g=create(b,Map.of(pos("e2"),HeroType.IZBA,pos("g4"),HeroType.BOLOTNIK),BLACK,0);
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("g4"),pos("e3"),BolotnikAbility.MUD));waitTurn(g,"g8","f6");move(g,"e2","e3");
        assertEquals(HeroType.BOLOTNIK,g.getPieceState(pos("e3")).hero());assertEquals(WHITE,baby(g).color());
        assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void summonCanReplaceItsOwnCasterWithoutTriggeringThePassive()throws Exception {
        var b=board("e1","h8");
        for(int r=0;r<2;r++)for(int c=0;c<8;c++){String cell=""+(char)('a'+c)+(r+1);if(!cell.equals("e1"))put(b,Pawn.class,WHITE,cell);}
        put(b,Rook.class,WHITE,"h1");Game g=charge(create(b,Map.of(pos("h1"),HeroType.IZBA),WHITE,seedFor(15,6)),"h1");
        // e1 is excluded, so h1 has index 6 among the 15 replacement candidates.
        long id=g.getPieceState(pos("h1")).id();summon(g,"h1");assertEquals(PieceState.Status.REPLACED,g.getSnapshot().pieces().get(id).status());
        assertEquals(1,g.getSummons().records().size());assertEquals(HeroType.BABA_YAGA,g.getPieceState(pos("h1")).hero());
        assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void summonedPawnCanCaptureNormallyWithoutChargingAnUltimateOrRestartingItsLifetime()throws Exception {
        var b=board("a1","h8");put(b,Rook.class,WHITE,"e2");put(b,Knight.class,WHITE,"b1");put(b,Pawn.class,BLACK,"d2");
        Game g=charge(create(b,Map.of(pos("e2"),HeroType.IZBA),WHITE,seedFor(12,0)),"e2");summon(g,"e2");PieceState p=baby(g);
        assertEquals(pos("c1"),p.position());move(g,"c1","d2");assertEquals(p.id(),g.getPieceState(pos("d2")).id());
        assertEquals(0,g.getPieceState(pos("d2")).ultimateCharge());assertEquals(2,g.getEconomy().whiteHalves());assertEquals(7,g.getSummons().records().get(p.id()).transformsAfterTurn());
        assertTrue(HeroAbilities.abilities(HeroType.BABA_YAGA).isEmpty());assertArrayEquals(GameSaveFile.encode(g.getSnapshot()),GameSaveFile.encode(reload(g).getSnapshot()));
    }
    @Test public void rejectedCaptureCannotRerollADangerousPassiveSpawn()throws Exception {
        var b=board("a1","h8");put(b,Rook.class,WHITE,"h6");put(b,Rook.class,BLACK,"h4");
        Game g=create(b,Map.of(pos("h6"),HeroType.IZBA),BLACK,seedFor(5,3));var before=g.getSnapshot();
        assertEquals(MovementRejection.KING_IN_CHECK,g.moveFigure(g.getCurrentPlayer(),pos("h4"),pos("h6")));assertEquals(before,g.getSnapshot());
        g=reload(g);long seed=g.getSummons().randomState();assertEquals(MovementRejection.KING_IN_CHECK,g.moveFigure(g.getCurrentPlayer(),pos("h4"),pos("h6")));
        assertEquals(seed,g.getSummons().randomState());assertTrue(g.getSummons().records().isEmpty());assertTrue(g.getMoves().isEmpty());
    }
}
