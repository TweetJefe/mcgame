package game.heroes.ares;
import game.core.*;
import game.heroes.*;
import game.heroes.ginseng.GinsengFixtures;
import game.state.*;
import game.moves.*;
import game.persistence.GameSaveFile;
import game.draft.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.ginseng.GinsengFixtures.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class AresTest {
    public static Game create(Map<Position,Figure>b,Map<Position,HeroType> h){return Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),h,HeroRules.prototype(),()->0L);}
    public static Game basic(){return create(board(),Map.of(pos("c2"),HeroType.ARES_BISHOP));}
    public static Game charge(Game g,String square,int amount) {
        var s=g.getSnapshot();var states=new HashMap<>(s.pieces());var p=s.pieceAt(pos(square));states.put(p.id(),p.withAbilityState(p.cooldown(),amount,p.storedPieceId()));
        return Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),states,s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),s.summons()),()->0L);
    }
    public static Game collision(){var b=board();put(b,Knight.class,WHITE,"d2");put(b,Rook.class,BLACK,"f2");return create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP,pos("f2"),HeroType.PUDGE_ROOK));}
    private void move(Game g,String a,String b){assertEquals(MoveResult.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos(a),pos(b)));}
    private void end(Game g){assertEquals(MoveResult.SUCCESS,g.endTurn(g.getCurrentPlayer()));}
    private void cast(Game g,String a,String t,Ability spell){assertEquals(MoveResult.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos(a),t==null?null:pos(t),spell));}
    private void spear(Game g,String to){cast(g,"c2",to,Ability.SPEAR);}
    private void cycle(Game g){boolean w=g.getPieceState(pos("b1"))!=null;move(g,w?"b1":"a3",w?"a3":"b1");end(g);boolean b=g.getPieceState(pos("g8"))!=null;move(g,b?"g8":"f6",b?"f6":"g8");end(g);}
    public static void roundtrip(Game g)throws Exception{byte[] bytes=GameSaveFile.encode(g.getSnapshot());Game copy=Game.fromSnapshot(GameSaveFile.decode(bytes),()->0L);assertArrayEquals(bytes,GameSaveFile.encode(copy.getSnapshot()));}

    @Test public void allEightDirectionsCarryFriendlyOrEnemyPiecesToTheirFullRange()throws Exception {
        for(FigureColor color:List.of(WHITE,BLACK))for(int dr=-1;dr<=1;dr++)for(int dc=-1;dc<=1;dc++)if(dr!=0 || dc!=0){
            var b=board();empty(b,"c2");empty(b,"h8");put(b,King.class,BLACK,"h7");put(b,Bishop.class,WHITE,"d4");
            Position first=new Position(3+dr,3+dc);b.put(first,new Field(8).createFigure(Knight.class,first,color));
            Game g=create(b,Map.of(pos("d4"),HeroType.ARES_BISHOP));long id=g.getPieceState(first).id();int range=dr==0 || dc==0?3:2;Position to=new Position(3+dr*range,3+dc*range);
            cast(g,"d4",String.valueOf((char)('a'+to.getColumn()))+(to.getRow()+1),Ability.SPEAR);assertEquals(id,g.getPieceState(to).id());assertFalse(g.getPieceState(to).isStunned());roundtrip(g);
        }
    }
    @Test public void firstPieceStopsBeforeSecondAndBothAreStunnedButThirdIsUntouched()throws Exception {
        var b=board();put(b,Pawn.class,BLACK,"d2");put(b,Knight.class,BLACK,"f2");put(b,Rook.class,BLACK,"g2");Game g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP));
        long first=g.getPieceState(pos("d2")).id();spear(g,"f2");assertEquals(first,g.getPieceState(pos("e2")).id());assertTrue(g.getPieceState(pos("e2")).isStunned());assertTrue(g.getPieceState(pos("f2")).isStunned());assertFalse(g.getPieceState(pos("g2")).isStunned());assertEquals(2,g.getPieceState(pos("e2")).stunnedThroughTurn());roundtrip(g);
    }
    @Test public void adjacentFiguresStayOnTheirSquaresAndStillGetStunned()throws Exception {
        var b=board();put(b,Pawn.class,WHITE,"d2");put(b,Pawn.class,BLACK,"e2");Game g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP));long id=g.getPieceState(pos("d2")).id();spear(g,"f2");assertEquals(id,g.getPieceState(pos("d2")).id());assertTrue(g.getPieceState(pos("d2")).isStunned());assertTrue(g.getPieceState(pos("e2")).isStunned());roundtrip(g);
    }
    @Test public void kingStopsTheSpearWithoutMovingOrStunningItOrTheCarriedPiece()throws Exception {
        var b=board();empty(b,"h8");put(b,King.class,BLACK,"f2");put(b,Pawn.class,WHITE,"d2");Game g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP));
        long id=g.getPieceState(pos("d2")).id();spear(g,"f2");assertEquals(id,g.getPieceState(pos("e2")).id());assertEquals(King.class,g.getPieceState(pos("f2")).type());assertFalse(g.getPieceState(pos("f2")).isStunned());assertFalse(g.getPieceState(pos("e2")).isStunned());roundtrip(g);
    }
    @Test public void emptyShotIsAllowedButDoesNotReplaceTheOrdinaryMoveAndCooldownIsThreeOwnStarts()throws Exception {
        Game g=basic();spear(g,"f2");assertEquals(3,g.getPieceState(pos("c2")).cooldown());assertEquals(MoveResult.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));cycle(g);assertEquals(2,g.getPieceState(pos("c2")).cooldown());cycle(g);assertEquals(1,g.getPieceState(pos("c2")).cooldown());cycle(g);assertEquals(MoveResult.SUCCESS,g.getAbilityAvailability(pos("c2"),Ability.SPEAR));roundtrip(g);
    }
    @Test public void outOfRangeOffRayAndSelfCheckRejectionsAreAtomic() {
        Game g=basic();var before=g.getSnapshot();assertEquals(MoveResult.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("g2"),Ability.SPEAR));assertEquals(MoveResult.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("f4"),Ability.SPEAR));assertEquals(before,g.getSnapshot());
        var b=board();empty(b,"a1");put(b,King.class,WHITE,"d1");put(b,Rook.class,BLACK,"d8");put(b,Pawn.class,WHITE,"d2");g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP));before=g.getSnapshot();assertEquals(MoveResult.KING_IN_CHECK,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("f2"),Ability.SPEAR));assertEquals(before,g.getSnapshot());
    }
    @Test public void stunBlocksAlliedMovementEnemyMovementAndActiveSpellsUntilEnemyEnd()throws Exception {
        Game g=collision();spear(g,"f2");assertEquals(MoveResult.STUNNED,g.moveFigure(g.getCurrentPlayer(),pos("e2"),pos("f4")));move(g,"b1","a3");end(g);
        assertEquals(MoveResult.STUNNED,g.moveFigure(g.getCurrentPlayer(),pos("f2"),pos("f3")));assertEquals(MoveResult.STUNNED,g.getAbilityAvailability(pos("f2"),Ability.HOOK));roundtrip(g);
        move(g,"g8","f6");end(g);assertFalse(g.getPieceState(pos("e2")).isStunned());assertFalse(g.getPieceState(pos("f2")).isStunned());move(g,"e2","f4");roundtrip(g);
    }
    @Test public void stunnedAttackersStopGivingCheckAndRecoverWhenTheEnemyTurnEnds()throws Exception {
        var b=board();empty(b,"a1");put(b,King.class,WHITE,"f1");put(b,Knight.class,BLACK,"d2");put(b,Rook.class,BLACK,"f2");Game g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP));assertTrue(g.isInCheck());
        spear(g,"f2");assertFalse(g.isInCheck());move(g,"b1","a3");end(g);move(g,"g8","f6");end(g);assertTrue(g.isInCheck());roundtrip(g);
    }
    @Test public void aStunnedPieceKeepsItsStunWhenPudgeMovesItById()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"d2");put(b,Pawn.class,WHITE,"f2");put(b,Rook.class,BLACK,"e5");Game g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP,pos("e5"),HeroType.PUDGE_ROOK));
        spear(g,"f2");long id=g.getPieceState(pos("e2")).id();move(g,"b1","a3");end(g);cast(g,"e5","e2",Ability.HOOK);assertEquals(id,g.getPieceState(pos("e4")).id());assertTrue(g.getPieceState(pos("e4")).isStunned());roundtrip(g);
    }
    @Test public void spearEntersHostileMudInsteadOfCarryingTheVictimThroughIt()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"e4");put(b,Pawn.class,BLACK,"d2");Game g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP,pos("e4"),HeroType.BOLOTNIK_KNIGHT));
        cast(g,"e4","e2",Ability.MUD);cycle(g);long bolo=g.getPieceState(pos("e4")).id();spear(g,"f2");assertEquals(bolo,g.getPieceState(pos("e2")).id());assertNull(g.getPieceState(pos("f2")));assertEquals(2,g.getEconomy().whiteHalves());roundtrip(g);
    }
    @Test public void anUnchargedAresDiesNormallyButOnePawnCaptureArmsThePassive()throws Exception {
        var b=board();put(b,Pawn.class,BLACK,"d3");put(b,Rook.class,BLACK,"d8");Game g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP));move(g,"c2","d3");assertEquals(1,g.getPieceState(pos("d3")).ultimateCharge());end(g);move(g,"d8","d3");assertNull(g.getPieceState(pos("d3")));assertEquals(12,g.getEconomy().whiteHalves());assertEquals(6,g.getEconomy().blackHalves());roundtrip(g);
        b=board();put(b,Rook.class,BLACK,"c8");g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP));move(g,"b1","a3");end(g);move(g,"c8","c2");assertEquals(BLACK,g.getPieceState(pos("c2")).color());assertEquals(Rook.class,g.getPieceState(pos("c2")).type());roundtrip(g);
    }
    @Test public void chargedRevengeConsumesItsChargeAndPaysEachCaptureExactlyOnce()throws Exception {
        var b=board();put(b,Rook.class,BLACK,"c8");Game g=charge(create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP)),"c2",1);long id=g.getPieceState(pos("c2")).id();move(g,"b1","a3");end(g);move(g,"c8","c2");
        assertNull(g.getPieceState(pos("c2")));assertEquals(0,g.getSnapshot().pieces().get(id).ultimateCharge());assertEquals(10,g.getEconomy().whiteHalves());assertEquals(6,g.getEconomy().blackHalves());assertEquals(1,g.getSnapshot().events().stream().filter(e->e.kind()==game.history.GameEvent.Kind.ARES_REVENGE).count());roundtrip(g);
    }
    @Test public void aKingCanCaptureUnchargedAresButCannotCaptureChargedAres()throws Exception {
        for(int charge:new int[]{0,1}){
            var b=board();empty(b,"h8");put(b,King.class,BLACK,"d4");Game g=charge(create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP)),"c2",charge);
            move(g,"b1","d2");end(g);move(g,"d4","c3");end(g);move(g,"d2","b1");end(g);
            assertEquals(charge==1?MoveResult.KING_IN_CHECK:MoveResult.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos("c3"),pos("c2")));roundtrip(g);
        }
    }
    @Test public void loadedPudgeDiesToRevengeAndItsAllyReturnsWithoutEatingAresTwice()throws Exception {
        var b=board();put(b,Rook.class,BLACK,"c8");put(b,Knight.class,BLACK,"c7");Game g=charge(charge(create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP,pos("c8"),HeroType.PUDGE_ROOK)),"c2",1),"c8",2);
        move(g,"b1","a3");end(g);cast(g,"c8","c7",Ability.SWALLOW);move(g,"c8","c2");assertEquals(Knight.class,g.getPieceState(pos("c2")).type());assertEquals(BLACK,g.getPieceState(pos("c2")).color());assertEquals(10,g.getEconomy().whiteHalves());assertEquals(6,g.getEconomy().blackHalves());roundtrip(g);
    }
    @Test public void aPudgePassengerCanKillAresAndThenDieToItsFreshlyChargedRevenge()throws Exception {
        var b=board();put(b,Rook.class,WHITE,"e2");put(b,Knight.class,WHITE,"d2");put(b,Bishop.class,BLACK,"c4");Game g=charge(create(b,Map.of(pos("e2"),HeroType.PUDGE_ROOK,pos("c4"),HeroType.ARES_BISHOP)),"e2",2);
        cast(g,"e2","d2",Ability.SWALLOW);move(g,"b1","a3");end(g);move(g,"c4","e2");assertNull(g.getPieceState(pos("e2")));assertEquals(6,g.getEconomy().whiteHalves());assertEquals(16,g.getEconomy().blackHalves());roundtrip(g);
    }
    @Test public void revengeAgainstIzbaTriggersItsDeathSummon()throws Exception {
        var b=board();put(b,Rook.class,BLACK,"c8");Game g=charge(create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP,pos("c8"),HeroType.IZBA_ROOK)),"c2",1);move(g,"b1","a3");end(g);move(g,"c8","c2");assertEquals(1,g.getSummons().records().size());assertTrue(g.getSnapshot().pieces().values().stream().anyMatch(p->p.hero()==HeroType.BABA_YAGA_PAWN));roundtrip(g);
    }
    @Test public void aBogCaptureTriggersRevengeAndRemovesTheDeadBogOwnersTerrain()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"e2");put(b,Bishop.class,BLACK,"g5");Game g=charge(create(b,Map.of(pos("e2"),HeroType.BOLOTNIK_KNIGHT,pos("g5"),HeroType.ARES_BISHOP)),"g5",1);cast(g,"e2","f4",Ability.MUD);move(g,"b1","a3");end(g);move(g,"g5","f4");assertNull(g.getPieceState(pos("f4")));assertTrue(g.getTerrain().cells().isEmpty());assertEquals(6,g.getEconomy().whiteHalves());assertEquals(6,g.getEconomy().blackHalves());roundtrip(g);
    }
    @Test public void ginsengCopiesOnlyTheSpearWithoutObtainingRevenge()throws Exception {
        var b=board();put(b,Bishop.class,WHITE,"e2");put(b,Pawn.class,BLACK,"c3");put(b,Knight.class,BLACK,"c5");Game g=charge(GinsengFixtures.create(b,Map.of(pos("e2"),HeroType.ARES_BISHOP)),"c2",2);
        cast(g,"e2","h2",Ability.SPEAR);cycle(g);cast(g,"c2","e2",Ability.COPY_SPELL);cycle(g);assertEquals(Ability.SPEAR,g.getSpells().borrowed().get(g.getPieceState(pos("c2")).id()).ability());
        assertEquals(MoveResult.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.SPEAR,pos("c5"),null,true)));assertTrue(g.getPieceState(pos("c4")).isStunned());assertTrue(g.getPieceState(pos("c5")).isStunned());assertEquals(0,g.getPieceState(pos("c2")).cooldown());assertTrue(g.getSpells().borrowed().isEmpty());assertEquals(List.of(Ability.SPEAR),HeroAbilities.abilities(HeroType.ARES_BISHOP));roundtrip(g);
    }
    @Test public void nativeSpearAndTheCopiedVersionAreBlockedByFaunsCloud()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"e2");Game g=create(b,Map.of(pos("c2"),HeroType.ARES_BISHOP,pos("e2"),HeroType.FAUN_KNIGHT));cast(g,"e2","c2",Ability.CLOUD);cycle(g);assertEquals(MoveResult.SILENCED,g.getAbilityAvailability(pos("c2"),Ability.SPEAR));roundtrip(g);
    }
    @Test public void aresIsEighthDraftHeroAndCanReplaceEitherBishop()throws Exception {
        for(String square:List.of("c1","f1")){
            Draft d=new Draft();assertEquals(10,d.getSnapshot().rules().catalogVersion());assertEquals(13,HeroCatalog.heroes().size());d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.ARES_BISHOP,pos(square)));d.finishPicks(BLACK);d.finishPicks(WHITE);Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);assertEquals(HeroType.ARES_BISHOP,g.getPieceState(pos(square)).hero());roundtrip(g);
        }
        Game sandbox=Game.mobaHeroesSandbox(600);assertEquals(HeroType.ARES_BISHOP,sandbox.getPieceState(pos("d2")).hero());assertEquals(HeroType.ARES_BISHOP,sandbox.getPieceState(pos("d7")).hero());roundtrip(sandbox);
    }
    @Test public void babaYagaMaturingOnTheCastingSidesTurnKeepsStunUntilTheEnemyEnd() {
        Field factory=new Field(8);
        var izba=PieceState.initial(1,factory.createFigure(Rook.class,pos("e2"),WHITE),HeroType.IZBA_ROOK);
        var baba=PieceState.initial(2,factory.createFigure(Pawn.class,pos("a2"),WHITE),HeroType.BABA_YAGA_PAWN).withStun(8);
        var origin=new game.summoning.SummonRecord(2,1,1,7,pos("a2"),game.summoning.SummonRecord.Cause.ULTIMATE,false);
        var ledger=new game.summoning.SummonState(true,5,Map.of(2L,origin));
        var mature=game.summoning.SummonResolver.mature(Map.of(1L,izba,2L,baba),ledger,7,WHITE,game.terrain.TerrainState.empty());
        var result=mature.pieces().get(2L);assertEquals(Rook.class,result.type());assertEquals(HeroType.IZBA_ROOK,result.hero());assertTrue(result.isStunned());
        assertTrue(rules.moba.AttackRules.expireStuns(mature.pieces(),7).get(2L).isStunned());assertFalse(rules.moba.AttackRules.expireStuns(mature.pieces(),8).get(2L).isStunned());
    }
}
