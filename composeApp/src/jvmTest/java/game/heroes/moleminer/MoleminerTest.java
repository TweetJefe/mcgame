package game.heroes.moleminer;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.TurnRejection;
import game.core.*;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.state.*;
import game.moves.*;
import game.draft.*;
import game.heroes.ginseng.GinsengFixtures;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.ginseng.GinsengFixtures.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;
public class MoleminerTest{
    public static Game seed(Game game,long seed){var s=game.getSnapshot();return Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),s.pieces(),s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),new game.summoning.SummonState(true,seed,s.summons().records())),()->0L);}
    public static Game createMiner(Map<Position,Figure>b,Map<Position,HeroType> other){var h=new HashMap<>(other);h.put(pos("c2"),HeroType.MOLEMINER_PAWN);return seed(Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),h,HeroRules.prototype(),()->0L),0);}
    public static Game basic(){var b=board();put(b,Pawn.class,WHITE,"c2");return createMiner(b,Map.of());}
    public static Game ready(){return AresTest.charge(basic(),"c2",2);}
    public static void move(Game g,String a,String b){assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos(a),pos(b)));}
    public static void end(Game g){assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer()));}
    public static void cast(Game g,String a,String target,Ability ability){assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos(a),target==null?null:pos(target),ability));}
    public static void plant(Game g){cast(g,"c2",null,Ability.PLANT_MINE);}
    private void cycle(Game g){boolean w=g.getPieceState(pos("b1"))!=null;move(g,w?"b1":"a3",w?"a3":"b1");end(g);boolean b=g.getPieceState(pos("g8"))!=null;move(g,b?"g8":"f6",b?"f6":"g8");end(g);}

    @Test public void plantingIsOptionalDoesNotReplaceMovementAndCooldownIsThreeOwnStarts()throws Exception{
        Game g=basic();plant(g);assertTrue(g.getSnapshot().mines().cells().containsKey(pos("c2")));assertEquals(3,g.getPieceState(pos("c2")).cooldown());assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));
        cycle(g);assertEquals(2,g.getPieceState(pos("c2")).cooldown());cycle(g);assertEquals(1,g.getPieceState(pos("c2")).cooldown());cycle(g);assertEquals(Success.SUCCESS,g.getAbilityAvailability(pos("c2"),Ability.PLANT_MINE));var before=g.getSnapshot();assertEquals(AbilityRejection.TERRAIN_OCCUPIED,g.useAbility(g.getCurrentPlayer(),pos("c2"),null,Ability.PLANT_MINE));assertEquals(before,g.getSnapshot());AresTest.roundtrip(g);
    }
    @Test public void enemyEntryDetonatesOnceAndAwardsTheOwnerGoldAndUltimateCharge()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Knight.class,BLACK,"b4");Game g=createMiner(b,Map.of());long source=g.getPieceState(pos("c2")).id();plant(g);move(g,"c2","c3");end(g);move(g,"b4","c2");assertNull(g.getPieceState(pos("c2")));assertTrue(g.getSnapshot().mines().cells().isEmpty());assertEquals(1,g.getPieceState(source).ultimateCharge());assertEquals(6,g.getEconomy().whiteHalves());assertEquals(0,g.getEconomy().blackHalves());assertEquals(1,g.getSnapshot().whiteCaptures().size());AresTest.roundtrip(g);
    }
    @Test public void friendlyEntryAndCrossingDoNotConsumeTheMine()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Knight.class,WHITE,"b4");Game g=createMiner(b,Map.of());plant(g);move(g,"c2","c3");end(g);move(g,"g8","f6");end(g);move(g,"b4","c2");assertTrue(g.getSnapshot().mines().cells().containsKey(pos("c2")));assertEquals(0,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void mineSurvivesItsOwnerAndCanPayAndChargeTheDeadHeroLater()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Rook.class,BLACK,"c8");Game g=createMiner(b,Map.of());long id=g.getPieceState(pos("c2")).id();plant(g);move(g,"c2","c3");end(g);move(g,"c8","c3");end(g);assertEquals(PieceState.Status.CAPTURED,g.getPieceState(id).status());assertTrue(g.getSnapshot().mines().cells().containsKey(pos("c2")));AresTest.roundtrip(g);
        move(g,"b1","a3");end(g);move(g,"c3","c2");assertNull(g.getPieceState(pos("c2")));assertEquals(1,g.getPieceState(id).ultimateCharge());assertEquals(10,g.getEconomy().whiteHalves());assertEquals(4,g.getEconomy().blackHalves());AresTest.roundtrip(g);
    }
    @Test public void multipleMinesOnDifferentCellsArePermanent()throws Exception{
        Game g=basic();plant(g);move(g,"c2","c3");end(g);move(g,"g8","f6");end(g);cycle(g);cycle(g);cast(g,"c3",null,Ability.PLANT_MINE);assertEquals(Set.of(pos("c2"),pos("c3")),g.getSnapshot().mines().cells().keySet());cycle(g);cycle(g);cycle(g);assertEquals(2,g.getSnapshot().mines().cells().size());AresTest.roundtrip(g);
    }
    @Test public void blackKingCannotEnterAnEnemyMineEvenWhenItCouldCaptureTheCoveringPiece()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");empty(b,"h8");put(b,King.class,BLACK,"d1");Game g=createMiner(b,Map.of());plant(g);move(g,"c2","c3");end(g);var before=g.getSnapshot();assertEquals(StatusRejection.KING_IMMUNE,g.moveFigure(g.getCurrentPlayer(),pos("d1"),pos("c2")));assertEquals(before,g.getSnapshot());AresTest.roundtrip(g);
    }
    @Test public void hookAndDisplacementTriggerMines()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Pawn.class,BLACK,"b3");put(b,Knight.class,BLACK,"c4");put(b,Rook.class,WHITE,"c1");Game g=createMiner(b,Map.of(pos("c1"),HeroType.PUDGE_ROOK));plant(g);move(g,"c2","b3");end(g);move(g,"g8","f6");end(g);cast(g,"c1","c4",Ability.HOOK);assertNull(g.getPieceState(pos("c2")));assertEquals(8,g.getEconomy().whiteHalves());assertEquals(0,g.getPieceState(pos("c1")).ultimateCharge());AresTest.roundtrip(g);
        b=board();put(b,Pawn.class,WHITE,"c2");put(b,Bishop.class,WHITE,"d3");put(b,Knight.class,BLACK,"d2");g=createMiner(b,Map.of(pos("d3"),HeroType.GINSENG_BISHOP));plant(g);move(g,"c2","c3");end(g);move(g,"g8","f6");end(g);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("d3"),Ability.DISPLACE,pos("d2"),pos("c2"),false)));assertNull(g.getPieceState(pos("c2")));assertEquals(6,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void ultimateHasRadiusThreeAndRejectsKingAndProtectedTargetsAtomically(){
        Game g=ready();var before=g.getSnapshot();assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("g6"),Ability.DETONATE));assertEquals(StatusRejection.KING_IMMUNE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("a1"),Ability.DETONATE));assertEquals(before,g.getSnapshot());
    }
    @Test public void jumpOntoEnemyKillsItAndTheCasterWithoutReplacingOrdinaryMovement()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Rook.class,BLACK,"e4");Game g=AresTest.charge(createMiner(b,Map.of()),"c2",2);long id=g.getPieceState(pos("c2")).id();cast(g,"c2","e4",Ability.DETONATE);assertNull(g.getPieceState(pos("c2")));assertNull(g.getPieceState(pos("e4")));assertEquals(PieceState.Status.DESTROYED,g.getPieceState(id).status());assertEquals(10,g.getEconomy().whiteHalves());assertEquals(1,g.getPieceState(id).ultimateCharge());assertEquals(0,g.getEconomy().blackHalves());assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));move(g,"b1","a3");end(g);AresTest.roundtrip(g);
    }
    @Test public void alliedLandingAndSuicideDoNotGiveGoldOrEnemyCapturePoints()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Rook.class,WHITE,"e4");Game g=AresTest.charge(createMiner(b,Map.of()),"c2",2);long ally=g.getPieceState(pos("e4")).id();cast(g,"c2","e4",Ability.DETONATE);assertEquals(PieceState.Status.DESTROYED,g.getPieceState(ally).status());assertEquals(0,g.getEconomy().whiteHalves());assertEquals(0,g.getEconomy().blackHalves());assertTrue(g.getSnapshot().whiteCaptures().isEmpty());assertTrue(g.getSnapshot().blackCaptures().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void randomBlastIsAlwaysTwoByTwoIncludesLandingAndStaysInsideTheBoard()throws Exception{
        for(String cell:List.of("a1","a4","h4","h8","e4")){var options=MoleminerAbilities.corners(pos(cell));assertFalse(options.isEmpty());for(var corner:options){var blast=new MineState.Blast(1,1,pos(cell),corner);assertTrue(blast.contains(pos(cell)));assertTrue(blast.end().getRow()<8 && blast.end().getColumn()<8);}}
        var seen=new HashSet<Position>();for(long seed=0;seed<20;seed++){Game g=seed(ready(),seed);var before=g.getSnapshot();g.getAbilityTargets(pos("c2"),Ability.DETONATE);assertEquals(before,g.getSnapshot());cast(g,"c2","e4",Ability.DETONATE);seen.add(g.getSnapshot().mines().blasts().getFirst().corner());AresTest.roundtrip(g);}assertEquals(4,seen.size());
    }
    @Test public void blastKillsExactlyTheFiguresInItsChosenSquare()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");for(int r=2;r<=4;r++)for(int c=3;c<=5;c++){Position at=new Position(r,c);b.put(at,new Field(8).createFigure(Pawn.class,at,BLACK));}Game g=AresTest.charge(createMiner(b,Map.of()),"c2",2);var before=g.getSnapshot();cast(g,"c2","e4",Ability.DETONATE);var blast=g.getSnapshot().mines().blasts().getFirst();int kills=0;for(var p:before.pieces().values())if(p.color()==BLACK && p.type()==Pawn.class){assertEquals(blast.contains(p.position())?PieceState.Status.CAPTURED:PieceState.Status.BOARD,g.getPieceState(p.id()).status());if(blast.contains(p.position()))kills++;}assertEquals(4,kills);assertEquals(8,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void passiveIzbaSpawnsAfterBlastAndChargedAresDoesNotKillCasterTwice()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Rook.class,BLACK,"e4");put(b,Bishop.class,BLACK,"f4");Game g=AresTest.charge(AresTest.charge(createMiner(b,Map.of(pos("e4"),HeroType.IZBA_ROOK,pos("f4"),HeroType.ARES_BISHOP)),"c2",2),"f4",1);cast(g,"c2","e4",Ability.DETONATE);assertEquals(1,g.getSummons().records().size());assertEquals(16,g.getEconomy().whiteHalves());assertEquals(0,g.getEconomy().blackHalves());assertEquals(0,g.getPieceState(pos("f4"))==null?0:g.getPieceState(pos("f4")).ultimateCharge());AresTest.roundtrip(g);
    }
    @Test public void mineKillingAresRevengeRemovesTheOwnerAtItsActualRemoteSquare()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Bishop.class,BLACK,"e4");Game g=AresTest.charge(createMiner(b,Map.of(pos("e4"),HeroType.ARES_BISHOP)),"e4",1);plant(g);move(g,"c2","c3");end(g);move(g,"e4","c2");assertNull(g.getPieceState(pos("c3")));assertNull(g.getPieceState(pos("c2")));assertEquals(6,g.getEconomy().whiteHalves());assertEquals(4,g.getEconomy().blackHalves());AresTest.roundtrip(g);
    }
    @Test public void ginsengCopiesPlantingAndOwnsItsMines()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"e2");Game g=AresTest.charge(seed(GinsengFixtures.create(b,Map.of(pos("e2"),HeroType.MOLEMINER_PAWN)),0),"c2",2);cast(g,"e2",null,Ability.PLANT_MINE);cycle(g);cast(g,"c2","e2",Ability.COPY_SPELL);cycle(g);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.PLANT_MINE,null,null,true)));assertEquals(g.getPieceState(pos("c2")).id(),g.getSnapshot().mines().cells().get(pos("c2")).ownerId());assertEquals(0,g.getPieceState(pos("c2")).cooldown());assertTrue(g.getSpells().borrowed().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void normalPromotionKeepsMoleminerAbilities()throws Exception{
        var b=board();empty(b,"c2");put(b,Pawn.class,WHITE,"c7");var h=Map.of(pos("c7"),HeroType.MOLEMINER_PAWN);Game g=Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),h,HeroRules.prototype(),()->0L);assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos("c7"),pos("c8"),Promotion.QUEEN));cast(g,"c8",null,Ability.PLANT_MINE);assertEquals(HeroType.MOLEMINER_PAWN,g.getPieceState(pos("c8")).hero());assertEquals(Queen.class,g.getPieceState(pos("c8")).type());AresTest.roundtrip(g);
    }
    @Test public void tenthDraftHeroUsesAnyPawnHomeAndSandboxUsesAFile()throws Exception{
        for(String cell:List.of("a2","h2")){var d=new Draft();assertEquals(10,d.getSnapshot().rules().catalogVersion());assertEquals(13,HeroCatalog.heroes().size());d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.MOLEMINER_PAWN,pos(cell)));d.finishPicks(BLACK);d.finishPicks(WHITE);Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);assertEquals(HeroType.MOLEMINER_PAWN,g.getPieceState(pos(cell)).hero());AresTest.roundtrip(g);}assertEquals(HeroType.MOLEMINER_PAWN,Game.mobaHeroesSandbox(600).getPieceState(pos("a2")).hero());
    }

    @Test public void twoOwnPawnCapturesChargeTheUltimateAndItSpendsBoth()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Pawn.class,BLACK,"d3");put(b,Pawn.class,BLACK,"e4");Game g=createMiner(b,Map.of());move(g,"c2","d3");assertEquals(1,g.getPieceState(pos("d3")).ultimateCharge());end(g);move(g,"g8","f6");end(g);move(g,"d3","e4");assertEquals(2,g.getPieceState(pos("e4")).ultimateCharge());long id=g.getPieceState(pos("e4")).id();cast(g,"e4","h4",Ability.DETONATE);assertEquals(0,g.getPieceState(id).ultimateCharge());assertEquals(4,g.getEconomy().whiteHalves());end(g);AresTest.roundtrip(g);
    }
    @Test public void kingAndBoxedAllyInsideTheAreaSurviveTheExplosion()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");empty(b,"h8");put(b,King.class,BLACK,"f4");put(b,Knight.class,WHITE,"c4");put(b,Pawn.class,WHITE,"f5");Game g=AresTest.charge(createMiner(b,Map.of(pos("c4"),HeroType.CLOWNGANGER_KNIGHT)),"c2",2);
        cast(g,"c4","f5",Ability.BOX);move(g,"b1","a3");end(g);move(g,"g8","f6");end(g);var before=g.getSnapshot();assertEquals(StatusRejection.BOXED,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("f5"),Ability.DETONATE));assertEquals(before,g.getSnapshot());cast(g,"c2","e4",Ability.DETONATE);assertEquals(King.class,g.getPieceState(pos("f4")).type());assertEquals(Pawn.class,g.getPieceState(pos("f5")).type());assertTrue(g.getPieceState(pos("f5")).isBoxed());AresTest.roundtrip(g);
    }
    @Test public void remotePudgeRetaliationKillsMineOwnerButPassengerReturnsToTheMineSquare()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Pawn.class,BLACK,"b3");put(b,Rook.class,BLACK,"c8");put(b,Knight.class,BLACK,"b7");Game g=AresTest.charge(createMiner(b,Map.of(pos("c8"),HeroType.PUDGE_ROOK)),"c8",2);plant(g);move(g,"c2","b3");end(g);cast(g,"c8","b7",Ability.SWALLOW);move(g,"c8","c2");assertNull(g.getPieceState(pos("b3")));assertEquals(Knight.class,g.getPieceState(pos("c2")).type());assertEquals(BLACK,g.getPieceState(pos("c2")).color());assertEquals(12,g.getEconomy().whiteHalves());assertEquals(4,g.getEconomy().blackHalves());AresTest.roundtrip(g);
    }
    @Test public void illusionTriggersMineWithoutRewardAndReceivesOneExtension()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Pawn.class,BLACK,"b3");put(b,Rook.class,WHITE,"c1");put(b,Knight.class,BLACK,"d3");Game g=AresTest.charge(createMiner(b,Map.of(pos("c1"),HeroType.PUDGE_ROOK,pos("d3"),HeroType.CLOWNGANGER_KNIGHT)),"d3",1);plant(g);move(g,"c2","b3");end(g);cast(g,"d3","c3",Ability.ILLUSION);long dummy=g.getPieceState(pos("c3")).id();assertEquals(HeroType.ILLUSION,g.getPieceState(dummy).hero());move(g,"g8","f6");end(g);cast(g,"c1","c3",Ability.HOOK);assertEquals(dummy,g.getPieceState(pos("c2")).id());assertTrue(g.getSnapshot().mines().cells().isEmpty());assertTrue(g.getSnapshot().clown().illusions().get(dummy).extended());assertEquals(2,g.getEconomy().whiteHalves());assertEquals(1,g.getPieceState(pos("b3")).ultimateCharge());AresTest.roundtrip(g);
    }
    @Test public void mineAndMudOnOneCellResolveOnlyOneCapture()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Knight.class,WHITE,"e2");put(b,Knight.class,BLACK,"b4");Game g=createMiner(b,Map.of(pos("e2"),HeroType.BOLOTNIK_KNIGHT));plant(g);move(g,"c2","c3");end(g);move(g,"g8","f6");end(g);cast(g,"e2","c2",Ability.MUD);move(g,"b1","a3");end(g);move(g,"b4","c2");assertNull(g.getPieceState(pos("c2")));assertEquals(6,g.getEconomy().whiteHalves());assertEquals(1,g.getPieceState(pos("c3")).ultimateCharge());assertEquals(0,g.getPieceState(pos("e2")).ultimateCharge());AresTest.roundtrip(g);
    }
    @Test public void explosionCannotUncoverOwnKingAndFailedPreviewDoesNotConsumeRandomness(){
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Rook.class,BLACK,"a8");put(b,Rook.class,WHITE,"a2");Game g=AresTest.charge(createMiner(b,Map.of()),"c2",2);var before=g.getSnapshot();assertEquals(MovementRejection.KING_IN_CHECK,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("a2"),Ability.DETONATE));assertEquals(before,g.getSnapshot());
    }
    @Test public void cloudBlocksMoleminerSpellsButDoesNotDisarmExistingMines()throws Exception{
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Knight.class,BLACK,"e5");put(b,Knight.class,BLACK,"b4");Game g=createMiner(b,Map.of(pos("e5"),HeroType.FAUN_KNIGHT));plant(g);move(g,"c2","c3");end(g);cast(g,"e5","c3",Ability.CLOUD);move(g,"b4","c2");end(g);assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("c3"),Ability.PLANT_MINE));assertEquals(6,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void ultimateLandingTriggersEnemyTrapsAndStillDetonatesExactlyOnce()throws Exception{
        for(boolean mine:List.of(true,false)){
            var b=board();put(b,Pawn.class,WHITE,"c2");put(b,mine?Pawn.class:Knight.class,BLACK,"e4");
            Game g=AresTest.charge(createMiner(b,Map.of(pos("e4"),mine?HeroType.MOLEMINER_PAWN:HeroType.BOLOTNIK_KNIGHT)),"c2",2);long id=g.getPieceState(pos("c2")).id();move(g,"b1","a3");end(g);cast(g,"e4",mine?null:"e4",mine?Ability.PLANT_MINE:Ability.MUD);move(g,"e4",mine?"e3":"f6");end(g);cast(g,"c2","e4",Ability.DETONATE);
            assertEquals(PieceState.Status.CAPTURED,g.getPieceState(id).status());assertEquals(1,g.getSnapshot().mines().blasts().size());assertEquals(4,g.getEconomy().blackHalves());assertEquals(mine?0:6,g.getEconomy().whiteHalves());assertTrue(g.getSnapshot().terrain().cells().isEmpty());assertTrue(g.getSnapshot().mines().cells().isEmpty());AresTest.roundtrip(g);
        }
    }
    @Test public void blastRemovingOriginalAndMovedIllusionKeepsReplayableEffectHistory()throws Exception{
        long chosen=-1;for(long value=0;value<100;value++){var first=game.summoning.SummonState.initial(value).choose(2);var second=game.summoning.SummonState.initial(first.nextState()).choose(4);if(first.index()==1 && second.index()==2){chosen=value;break;}}assertTrue(chosen>=0);
        var b=board();put(b,Pawn.class,WHITE,"c2");put(b,Knight.class,BLACK,"e4");Game g=seed(AresTest.charge(AresTest.charge(createMiner(b,Map.of(pos("e4"),HeroType.CLOWNGANGER_KNIGHT)),"c2",2),"e4",1),chosen);move(g,"b1","a3");end(g);cast(g,"e4","f4",Ability.ILLUSION);long dummy=g.getPieceState(pos("f4")).id();move(g,"f4","d5");end(g);cast(g,"c2","e4",Ability.DETONATE);assertEquals(PieceState.Status.DISSOLVED,g.getPieceState(dummy).status());assertTrue(g.getSnapshot().clown().illusions().get(dummy).extended());AresTest.roundtrip(g);
    }
}
