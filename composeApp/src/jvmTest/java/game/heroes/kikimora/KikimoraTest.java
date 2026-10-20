package game.heroes.kikimora;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.TurnRejection;
import game.core.*;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.heroes.moleminer.MoleminerTest;
import game.moves.*;
import game.state.*;
import game.draft.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.ginseng.GinsengFixtures.*;
import static game.heroes.moleminer.MoleminerTest.move;
import static game.heroes.moleminer.MoleminerTest.end;
import static game.heroes.moleminer.MoleminerTest.cast;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class KikimoraTest{
    public static Map<Position,Figure> setup(){var b=board();empty(b,"g8");return b;}
    public static Game create(Map<Position,Figure>b,Map<Position,HeroType>other){return at(b,other,"c2");}
    public static Game at(Map<Position,Figure>b,Map<Position,HeroType>other,String source){var h=new HashMap<>(other);h.put(pos(source),HeroType.KIKIMORA_BISHOP);return MoleminerTest.seed(Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),h,HeroRules.prototype(),()->0L),0);}
    public static Game basic(){var b=setup();put(b,Knight.class,BLACK,"d3");return create(b,Map.of());}
    public static Game ready(){return AresTest.charge(basic(),"c2",2);}
    public static void yarn(Game g,String target){cast(g,"c2",target,Ability.YARN);}
    public static void pot(Game g,String target){cast(g,"c2",target,Ability.CLAY_POT);}
    public static void cycle(Game g){boolean w=g.getPieceState(pos("b1"))!=null;move(g,w?"b1":"a3",w?"a3":"b1");end(g);boolean b=g.getPieceState(pos("h8"))!=null;move(g,b?"h8":"g8",b?"g8":"h8");end(g);}
    public static void copy(Game g,String actor,String source){cast(g,actor,source,Ability.COPY_SPELL);}
    public static void borrowed(Game g,String actor,Ability ability,String target){assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos(actor),ability,pos(target),null,true)));}
    @Test public void singleTargetStunLastsThroughOneEnemyTurnAndDoesNotReplaceMovement()throws Exception{
        Game g=basic();yarn(g,"d3");assertEquals(2,g.getPieceState(pos("d3")).stunnedThroughTurn());assertEquals(3,g.getPieceState(pos("c2")).cooldown());assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));move(g,"b1","a3");end(g);var before=g.getSnapshot();assertEquals(StatusRejection.STUNNED,g.moveFigure(g.getCurrentPlayer(),pos("d3"),pos("f4")));assertEquals(before,g.getSnapshot());move(g,"h8","g8");end(g);assertFalse(g.getPieceState(pos("d3")).isStunned());AresTest.roundtrip(g);
    }
    @Test public void secondEnemyOneOrTwoCellsBehindMakesBothStunsTwoTurns()throws Exception{
        for(String second:List.of("e4","f5")){var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Knight.class,BLACK,second);Game g=create(b,Map.of());yarn(g,"d3");assertEquals(4,g.getPieceState(pos("d3")).stunnedThroughTurn());assertEquals(4,g.getPieceState(pos(second)).stunnedThroughTurn());cycle(g);assertTrue(g.getPieceState(pos(second)).isStunned());cycle(g);assertFalse(g.getPieceState(pos(second)).isStunned());AresTest.roundtrip(g);}
    }
    @Test public void friendlySecondFigureUsesItsOwnTwoFollowingTurnsExcludingCastTurn()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Bishop.class,WHITE,"e4");Game g=create(b,Map.of(pos("e4"),HeroType.GINSENG_BISHOP));yarn(g,"d3");assertEquals(4,g.getPieceState(pos("d3")).stunnedThroughTurn());assertEquals(5,g.getPieceState(pos("e4")).stunnedThroughTurn());assertEquals(StatusRejection.STUNNED,g.moveFigure(g.getCurrentPlayer(),pos("e4"),pos("f5")));cycle(g);assertEquals(StatusRejection.STUNNED,g.getAbilityAvailability(pos("e4"),Ability.DISPLACE));cycle(g);assertFalse(g.getPieceState(pos("d3")).isStunned());assertTrue(g.getPieceState(pos("e4")).isStunned());cycle(g);assertFalse(g.getPieceState(pos("e4")).isStunned());AresTest.roundtrip(g);
    }
    @Test public void secondFigureFollowsTheExactFlightIncludingANonDiagonalSlope()throws Exception{
        var b=setup();for(String s:List.of("e3","g4","e5"))put(b,Knight.class,BLACK,s);Game g=create(b,Map.of());yarn(g,"e3");assertTrue(g.getPieceState(pos("g4")).isStunned());assertFalse(g.getPieceState(pos("e5")).isStunned());assertEquals(4,g.getPieceState(pos("e3")).stunnedThroughTurn());AresTest.roundtrip(g);
    }
    @Test public void offRayTooFarOrBehindTheCasterDoesNotCountAsSecondFigure()throws Exception{
        for(String second:List.of("e3","g6","b1")){var b=setup();put(b,Knight.class,BLACK,"d3");if(!second.equals("b1"))put(b,Knight.class,BLACK,second);Game g=create(b,Map.of());yarn(g,"d3");assertEquals(2,g.getPieceState(pos("d3")).stunnedThroughTurn());assertFalse(g.getPieceState(pos(second)).isStunned());AresTest.roundtrip(g);}
    }
    @Test public void kingsAndProtectedFiguresStopTheSecondSearch()throws Exception{
        for(boolean king:List.of(true,false)){var b=setup();put(b,Knight.class,BLACK,"d3");if(king){empty(b,"h8");put(b,King.class,BLACK,"e4");}else put(b,Knight.class,BLACK,"e4");Game g=create(b,Map.of());if(!king){g=AresTest.charge(g,"c2",2);pot(g,"d3");cycle(g);var before=g.getSnapshot();assertEquals(StatusRejection.POTTED,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("d3"),Ability.YARN));assertEquals(before,g.getSnapshot());}else{yarn(g,"d3");assertEquals(2,g.getPieceState(pos("d3")).stunnedThroughTurn());assertFalse(g.getPieceState(pos("e4")).isStunned());}AresTest.roundtrip(g);}
    }
    @Test public void primaryTargetMustBeEnemyNonkingWithinRadiusTwo(){
        var b=setup();put(b,Knight.class,WHITE,"d3");put(b,Knight.class,BLACK,"f5");Game g=create(b,Map.of());var before=g.getSnapshot();assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("d3"),Ability.YARN));assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("f5"),Ability.YARN));assertEquals(StatusRejection.KING_IMMUNE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("h8"),Ability.YARN));assertEquals(before,g.getSnapshot());
    }
    @Test public void cooldownIsThreeOwnStarts()throws Exception{Game g=basic();yarn(g,"d3");cycle(g);assertEquals(2,g.getPieceState(pos("c2")).cooldown());cycle(g);assertEquals(1,g.getPieceState(pos("c2")).cooldown());cycle(g);assertEquals(Success.SUCCESS,g.getAbilityAvailability(pos("c2"),Ability.YARN));AresTest.roundtrip(g);}
    @Test public void stunHistoriesOfDifferentCastersRoundtripAfterReapplication()throws Exception{
        var b=setup();put(b,Bishop.class,WHITE,"e2");put(b,Knight.class,BLACK,"d3");put(b,Knight.class,BLACK,"e4");Game g=AresTest.charge(at(b,Map.of(pos("c2"),HeroType.GINSENG_BISHOP),"e2"),"c2",2);cast(g,"e2","d3",Ability.YARN);cycle(g);copy(g,"c2","e2");cycle(g);borrowed(g,"c2",Ability.YARN,"d3");AresTest.roundtrip(g);
    }
    @Test public void potCostsTwoCapturesPreservesIdentityAndGivesNoGold()throws Exception{
        Game g=basic();var before=g.getSnapshot();assertEquals(AbilityRejection.ULTIMATE_NOT_READY,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("d3"),Ability.CLAY_POT));assertEquals(before,g.getSnapshot());g=AresTest.charge(g,"c2",2);long id=g.getPieceState(pos("d3")).id();pot(g,"d3");assertEquals(id,g.getPieceState(pos("d3")).id());assertTrue(g.getPieceState(id).isPotted());assertEquals(0,g.getPieceState(pos("c2")).ultimateCharge());assertEquals(0,g.getEconomy().whiteHalves());assertTrue(g.getSnapshot().whiteCaptures().isEmpty());assertFalse(g.getTurnState().moveUsed());AresTest.roundtrip(g);
    }
    @Test public void alliedOrEnemyPotCannotMoveAttackOrBeCaptured()throws Exception{
        for(FigureColor color:List.of(WHITE,BLACK)){var b=setup();put(b,Knight.class,color,"d3");put(b,Rook.class,WHITE,"d1");Game g=AresTest.charge(create(b,Map.of()),"c2",2);pot(g,"d3");var before=g.getSnapshot();assertEquals(color==WHITE?MovementRejection.OWN_PIECE_AT_TARGET:StatusRejection.POTTED,g.moveFigure(g.getCurrentPlayer(),pos("d1"),pos("d3")));assertEquals(MovementRejection.ILLEGAL_MOVEMENT,g.moveFigure(g.getCurrentPlayer(),pos("d1"),pos("d5")));if(color==WHITE)assertEquals(StatusRejection.POTTED,g.moveFigure(g.getCurrentPlayer(),pos("d3"),pos("f4")));assertEquals(before,g.getSnapshot());cycle(g);cycle(g);cycle(g);assertTrue(g.getPieceState(pos("d3")).isPotted());AresTest.roundtrip(g);}
    }
    @Test public void potStopsGivingCheckWithoutBeingACapture()throws Exception{
        var b=setup();empty(b,"c2");put(b,Bishop.class,WHITE,"b2");put(b,Rook.class,BLACK,"a2");put(b,Pawn.class,BLACK,"f6");Game g=AresTest.charge(at(b,Map.of(),"b2"),"b2",2);cast(g,"b2","a2",Ability.CLAY_POT);assertTrue(g.getPieceState(pos("a2")).isPotted());assertEquals(StatusRejection.POTTED,g.moveFigure(g.getCurrentPlayer(),pos("a1"),pos("a2")));move(g,"b1","c3");end(g);AresTest.roundtrip(g);
    }
    @Test public void potRangeIsAllEightNeighborsAndKingsAndSelfAreExcluded(){
        var b=setup();put(b,Knight.class,BLACK,"c4");Game g=AresTest.charge(create(b,Map.of()),"c2",2);var before=g.getSnapshot();assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("c4"),Ability.CLAY_POT));assertEquals(StatusRejection.KING_IMMUNE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("a1"),Ability.CLAY_POT));assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("c2"),Ability.CLAY_POT));assertEquals(before,g.getSnapshot());
    }
    @Test public void ginsengCanShiftAPotAndItKeepsItsFlagAfterMoving()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Bishop.class,WHITE,"f2");Game g=AresTest.charge(create(b,Map.of(pos("f2"),HeroType.GINSENG_BISHOP)),"c2",2);long id=g.getPieceState(pos("d3")).id();pot(g,"d3");cycle(g);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("f2"),Ability.DISPLACE,pos("d3"),pos("e4"),false)));assertNull(g.getPieceState(pos("d3")));assertEquals(id,g.getPieceState(pos("e4")).id());assertTrue(g.getPieceState(id).isPotted());AresTest.roundtrip(g);
    }
    @Test public void pudgeHookCanMoveAPot()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Rook.class,WHITE,"a3");Game g=AresTest.charge(create(b,Map.of(pos("a3"),HeroType.PUDGE_ROOK)),"c2",2);pot(g,"d3");move(g,"a1","a2");end(g);move(g,"h8","g8");end(g);cast(g,"a3","d3",Ability.HOOK);assertTrue(g.getPieceState(pos("b3")).isPotted());assertNull(g.getPieceState(pos("d3")));AresTest.roundtrip(g);
    }
    @Test public void aresSpearCanCarryAPotWithoutStunningOrDestroyingIt()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"c3");put(b,Bishop.class,WHITE,"b3");Game g=AresTest.charge(create(b,Map.of(pos("b3"),HeroType.ARES_BISHOP)),"c2",2);pot(g,"c3");move(g,"b1","a3");end(g);move(g,"h8","g7");end(g);cast(g,"b3","e3",Ability.SPEAR);assertTrue(g.getPieceState(pos("e3")).isPotted());assertFalse(g.getPieceState(pos("e3")).isStunned());AresTest.roundtrip(g);
    }
    @Test public void movingAPotOntoEnemyMineOrMudDoesNotTriggerTheTrap()throws Exception{
        for(boolean mine:List.of(true,false)){var b=setup();put(b,Knight.class,WHITE,"d3");put(b,Bishop.class,WHITE,"f2");if(mine)put(b,Pawn.class,BLACK,"e4");else put(b,Knight.class,BLACK,"f6");var h=new HashMap<Position,HeroType>();h.put(pos("f2"),HeroType.GINSENG_BISHOP);h.put(pos(mine?"e4":"f6"),mine?HeroType.MOLEMINER_PAWN:HeroType.BOLOTNIK_KNIGHT);Game g=AresTest.charge(create(b,h),"c2",2);pot(g,"d3");move(g,"b1","a3");end(g);if(mine){cast(g,"e4",null,Ability.PLANT_MINE);move(g,"e4","e3");}else{cast(g,"f6","e4",Ability.MUD);move(g,"h8","g8");}end(g);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("f2"),Ability.DISPLACE,pos("d3"),pos("e4"),false)));assertTrue(g.getPieceState(pos("e4")).isPotted());assertTrue(mine?g.getSnapshot().mines().cells().containsKey(pos("e4")):g.getTerrain().cells().containsKey(pos("e4")));assertEquals(0,g.getEconomy().blackHalves());AresTest.roundtrip(g);}
    }
    @Test public void lightningCannotTargetAPotAndExistingChargesAreRemovedWithoutAKill()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Rook.class,WHITE,"e2");Game g=AresTest.charge(AresTest.charge(create(b,Map.of(pos("e2"),HeroType.ZEUS_ROOK)),"c2",2),"e2",2);cast(g,"e2","d3",Ability.CHAIN_LIGHTNING);assertFalse(g.getSnapshot().lightning().charges().isEmpty());cycle(g);pot(g,"d3");assertTrue(g.getSnapshot().lightning().charges().isEmpty());cycle(g);var before=g.getSnapshot();assertEquals(StatusRejection.POTTED,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("d3"),Ability.CHAIN_LIGHTNING));assertEquals(AbilityRejection.NO_CHARGED_TARGETS,g.getAbilityAvailability(pos("e2"),Ability.THUNDERSTORM));assertEquals(before,g.getSnapshot());assertEquals(0,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void explosionLeavesPotsIntactAndCannotLandDirectlyOnThem()throws Exception{
        boolean covered=false;
        for(long seed=0;seed<8;seed++){var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Pawn.class,WHITE,"e2");Game g=MoleminerTest.seed(AresTest.charge(AresTest.charge(create(b,Map.of(pos("e2"),HeroType.MOLEMINER_PAWN)),"c2",2),"e2",2),seed);pot(g,"d3");cycle(g);var before=g.getSnapshot();assertEquals(StatusRejection.POTTED,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("d3"),Ability.DETONATE));assertEquals(before,g.getSnapshot());cast(g,"e2","d4",Ability.DETONATE);covered|=g.getSnapshot().mines().blasts().getLast().contains(pos("d3"));assertTrue(g.getPieceState(pos("d3")).isPotted());assertEquals(0,g.getEconomy().whiteHalves());AresTest.roundtrip(g);}assertTrue(covered);
    }
    @Test public void potKeepsASwallowedAllyWithoutTriggeringPudgeReaction()throws Exception{
        var b=setup();put(b,Rook.class,WHITE,"d3");put(b,Knight.class,WHITE,"d4");Game g=AresTest.charge(AresTest.charge(create(b,Map.of(pos("d3"),HeroType.PUDGE_ROOK)),"c2",2),"d3",2);long ally=g.getPieceState(pos("d4")).id();cast(g,"d3","d4",Ability.SWALLOW);cycle(g);pot(g,"d3");assertEquals(Long.valueOf(ally),g.getPieceState(pos("d3")).storedPieceId());assertEquals(PieceState.Status.SWALLOWED,g.getPieceState(ally).status());cycle(g);assertTrue(g.getPieceState(pos("d3")).isPotted());assertEquals(StatusRejection.POTTED,g.getAbilityAvailability(pos("d3"),Ability.HOOK));AresTest.roundtrip(g);
    }
    @Test public void aPottedIllusionOutlivesItsOriginalAndOrdinaryExpiry()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");Game g=AresTest.charge(AresTest.charge(create(b,Map.of(pos("d3"),HeroType.CLOWNGANGER_KNIGHT)),"c2",2),"d3",1);move(g,"b1","a3");end(g);cast(g,"d3","c3",Ability.ILLUSION);move(g,"h8","g8");end(g);assertEquals(HeroType.ILLUSION,g.getPieceState(pos("c3")).hero());long id=g.getPieceState(pos("c3")).id();pot(g,"c3");move(g,"c2","d3");end(g);move(g,"g8","h8");end(g);cycle(g);cycle(g);assertTrue(g.getPieceState(id).isPotted());assertEquals(PieceState.Status.BOARD,g.getPieceState(id).status());AresTest.roundtrip(g);
    }
    @Test public void aPottedBabaYagaDoesNotMature()throws Exception{
        Game chosen=null;long id=0;
        for(long seed=0;seed<40 && chosen==null;seed++){var b=setup();put(b,Rook.class,WHITE,"e2");Game g=MoleminerTest.seed(AresTest.charge(AresTest.charge(create(b,Map.of(pos("e2"),HeroType.IZBA_ROOK)),"c2",2),"e2",2),seed);cast(g,"e2",null,Ability.SUMMON_YAGA);var baby=g.getSnapshot().pieces().values().stream().filter(p->p.hero()==HeroType.BABA_YAGA_PAWN).findFirst().orElseThrow();if(Math.max(Math.abs(baby.position().getRow()-1),Math.abs(baby.position().getColumn()-2))==1){chosen=g;id=baby.id();}}
        assertNotNull(chosen);Game g=chosen;cycle(g);pot(g,cell(g.getPieceState(id).position()));cycle(g);cycle(g);cycle(g);assertTrue(g.getPieceState(id).isPotted());assertEquals(HeroType.BABA_YAGA_PAWN,g.getPieceState(id).hero());assertFalse(g.getSummons().records().get(id).transformed());AresTest.roundtrip(g);
    }
    private static String cell(Position p){return ""+(char)('a'+p.getColumn())+(p.getRow()+1);}
    @Test public void aPottedMineOwnerKeepsOldMinesButCannotBeKilledByAresRevenge()throws Exception{
        var b=setup();empty(b,"c2");put(b,Bishop.class,WHITE,"c3");put(b,Pawn.class,WHITE,"d3");put(b,Bishop.class,BLACK,"b5");put(b,Pawn.class,BLACK,"f6");Game g=AresTest.charge(AresTest.charge(at(b,Map.of(pos("d3"),HeroType.MOLEMINER_PAWN,pos("b5"),HeroType.ARES_BISHOP),"c3"),"c3",2),"b5",1);long owner=g.getPieceState(pos("d3")).id();cast(g,"d3",null,Ability.PLANT_MINE);move(g,"d3","d4");end(g);move(g,"h8","g8");end(g);cast(g,"c3","d4",Ability.CLAY_POT);move(g,"b1","a3");end(g);move(g,"b5","d3");assertTrue(g.getPieceState(owner).isPotted());assertEquals(0,g.getPieceState(owner).ultimateCharge());assertEquals(6,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void pottedBolotnikDoesNotEmergeFromOldMud()throws Exception{
        var b=setup();put(b,Knight.class,WHITE,"d3");put(b,Knight.class,BLACK,"h5");Game g=AresTest.charge(create(b,Map.of(pos("d3"),HeroType.BOLOTNIK_KNIGHT)),"c2",2);cast(g,"d3","f4",Ability.MUD);cycle(g);pot(g,"d3");move(g,"a3","b1");end(g);move(g,"h5","f4");assertTrue(g.getPieceState(pos("d3")).isPotted());assertEquals(BLACK,g.getPieceState(pos("f4")).color());assertEquals(0,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void ginsengCopiesYarnAndUsesTheNewCasterDirection()throws Exception{
        var b=setup();put(b,Bishop.class,WHITE,"e2");for(String s:List.of("d3","c4","e4"))put(b,Knight.class,BLACK,s);Game g=AresTest.charge(at(b,Map.of(pos("c2"),HeroType.GINSENG_BISHOP),"e2"),"c2",2);cast(g,"e2","d3",Ability.YARN);cycle(g);copy(g,"c2","e2");cycle(g);borrowed(g,"c2",Ability.YARN,"d3");assertTrue(g.getPieceState(pos("e4")).isStunned());assertEquals(0,g.getPieceState(pos("c2")).cooldown());assertTrue(g.getSpells().borrowed().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void ginsengCopiesPotWithoutAdditionalCost()throws Exception{
        var b=setup();put(b,Bishop.class,WHITE,"e2");put(b,Knight.class,BLACK,"f3");put(b,Knight.class,BLACK,"d3");Game g=AresTest.charge(AresTest.charge(at(b,Map.of(pos("c2"),HeroType.GINSENG_BISHOP),"e2"),"e2",2),"c2",2);cast(g,"e2","f3",Ability.CLAY_POT);cycle(g);copy(g,"c2","e2");cycle(g);borrowed(g,"c2",Ability.CLAY_POT,"d3");assertTrue(g.getPieceState(pos("f3")).isPotted());assertTrue(g.getPieceState(pos("d3")).isPotted());assertEquals(0,g.getPieceState(pos("c2")).ultimateCharge());assertTrue(g.getSpells().borrowed().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void potCanBeAnAnchorForChronomlin()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Pawn.class,WHITE,"h3");Game g=AresTest.charge(create(b,Map.of(pos("h3"),HeroType.CHRONOMLIN_PAWN)),"c2",2);pot(g,"d3");cycle(g);cast(g,"h3","d3",Ability.GRAPPLE);assertEquals(HeroType.CHRONOMLIN_PAWN,g.getPieceState(pos("e3")).hero());assertTrue(g.getPieceState(pos("d3")).isPotted());AresTest.roundtrip(g);
    }
    @Test public void invisiblePrimaryIsRejectedButAutomaticSecondTargetCanBeHidden()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d3");put(b,Knight.class,BLACK,"e4");Game g=AresTest.charge(create(b,Map.of(pos("e4"),HeroType.FAUN_KNIGHT)),"e4",2);move(g,"b1","a3");end(g);cast(g,"e4",null,Ability.INVISIBILITY);move(g,"h8","g8");end(g);var before=g.getSnapshot();assertEquals(StatusRejection.TARGET_INVISIBLE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("e4"),Ability.YARN));assertEquals(before,g.getSnapshot());yarn(g,"d3");assertTrue(g.getPieceState(pos("e4")).isStunned());AresTest.roundtrip(g);
    }
    @Test public void pottedRookCannotParticipateInCastling()throws Exception{
        var b=new HashMap<>(new Field(8).getGameField());empty(b,"f1");empty(b,"g1");put(b,Bishop.class,WHITE,"g2");Game g=Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.initial(),null,0),Map.of(pos("g2"),HeroType.KIKIMORA_BISHOP),HeroRules.prototype(),()->0L);g=AresTest.charge(g,"g2",2);cast(g,"g2","h1",Ability.CLAY_POT);var before=g.getSnapshot();assertFalse(g.getSnapshot().castlingRights().whiteKingSide());assertFalse(g.moveFigure(g.getCurrentPlayer(),pos("e1"),pos("g1")).isSuccess());assertEquals(before,g.getSnapshot());AresTest.roundtrip(g);
    }
    @Test public void potCannotBeUsedAsSourceForGinsengCopyOrSwallowedByPudge()throws Exception{
        var b=setup();put(b,Rook.class,WHITE,"d3");put(b,Knight.class,WHITE,"d4");put(b,Bishop.class,WHITE,"f2");put(b,Rook.class,WHITE,"e3");Game g=AresTest.charge(AresTest.charge(AresTest.charge(AresTest.charge(create(b,Map.of(pos("d3"),HeroType.PUDGE_ROOK,pos("e3"),HeroType.PUDGE_ROOK,pos("f2"),HeroType.GINSENG_BISHOP)),"c2",2),"d3",2),"e3",2),"f2",2);cast(g,"d3","d4",Ability.SWALLOW);cycle(g);pot(g,"d3");cycle(g);var before=g.getSnapshot();assertEquals(StatusRejection.POTTED,g.useAbility(g.getCurrentPlayer(),pos("f2"),pos("d3"),Ability.COPY_SPELL));assertEquals(StatusRejection.POTTED,g.useAbility(g.getCurrentPlayer(),pos("e3"),pos("d3"),Ability.SWALLOW));assertEquals(before,g.getSnapshot());AresTest.roundtrip(g);
    }
    @Test public void aCloudBlocksBothKikimoraSpells()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"e5");Game g=AresTest.charge(create(b,Map.of(pos("e5"),HeroType.FAUN_KNIGHT)),"c2",2);move(g,"b1","a3");end(g);cast(g,"e5","c2",Ability.CLOUD);move(g,"h8","g8");end(g);assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("c2"),Ability.YARN));assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("c2"),Ability.CLAY_POT));AresTest.roundtrip(g);
    }
    @Test public void convertingADoubleMovedPawnRemovesEnPassantImmediately()throws Exception{
        var b=setup();empty(b,"c2");empty(b,"h8");put(b,King.class,BLACK,"h7");put(b,Bishop.class,WHITE,"d4");put(b,Pawn.class,WHITE,"d5");put(b,Pawn.class,BLACK,"e7");Game g=Game.fromMobaPosition(new GamePosition(b,BLACK,CastlingRights.none(),null,0),Map.of(pos("d4"),HeroType.KIKIMORA_BISHOP),HeroRules.prototype(),()->0L);g=AresTest.charge(g,"d4",2);move(g,"e7","e5");end(g);assertEquals(pos("e6"),g.getSnapshot().enPassantTarget());cast(g,"d4","e5",Ability.CLAY_POT);assertNull(g.getSnapshot().enPassantTarget());var before=g.getSnapshot();assertFalse(g.moveFigure(g.getCurrentPlayer(),pos("d5"),pos("e6")).isSuccess());assertEquals(before,g.getSnapshot());assertTrue(g.getPieceState(pos("e5")).isPotted());AresTest.roundtrip(g);
    }
    @Test public void draftAndSandboxContainThirteenthHero()throws Exception{
        var d=new Draft();assertEquals(10,d.getSnapshot().rules().catalogVersion());assertEquals(13,HeroCatalog.heroes().size());d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.KIKIMORA_BISHOP,pos("f1")));assertNotEquals(DraftActionResult.SUCCESS,d.pick(BLACK,HeroType.KIKIMORA_BISHOP,pos("c8")));d.finishPicks(BLACK);d.finishPicks(WHITE);Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);assertEquals(HeroType.KIKIMORA_BISHOP,g.getPieceState(pos("f1")).hero());AresTest.roundtrip(g);g=Game.mobaHeroesSandbox(600);assertEquals(HeroType.KIKIMORA_BISHOP,g.getPieceState(pos("g2")).hero());assertEquals(1,g.getSnapshot().pieces().values().stream().filter(p->p.hero()==HeroType.KIKIMORA_BISHOP).count());AresTest.roundtrip(g);
    }
}
