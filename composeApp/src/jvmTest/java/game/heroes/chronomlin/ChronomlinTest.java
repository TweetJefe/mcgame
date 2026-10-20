package game.heroes.chronomlin;

import game.heroes.bolotnik.BolotnikAbility;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.faun.FaunAbility;
import game.heroes.clownganger.ClowngangerAbility;
import game.heroes.moleminer.MoleminerAbility;

import game.core.*;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.heroes.moleminer.MoleminerTest;
import game.heroes.clownganger.ClownView;
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

public class ChronomlinTest {
    public static Map<Position,Figure> setup(){var b=board();empty(b,"g8");put(b,Pawn.class,WHITE,"c2");return b;}
    public static Game create(Map<Position,Figure>b,Map<Position,HeroType>other){return at(b,other,"c2");}
    public static Game at(Map<Position,Figure>b,Map<Position,HeroType>other,String source){var h=new HashMap<>(other);h.put(pos(source),HeroType.CHRONOMLIN_PAWN);return MoleminerTest.seed(Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),h,HeroRules.prototype(),()->0L),0);}
    public static Game basic(){var b=setup();put(b,Knight.class,BLACK,"c6");return create(b,Map.of());}
    public static Game ready(){return AresTest.charge(basic(),"c2",1);}
    public static void cycle(Game g){boolean w=g.getPieceState(pos("b1"))!=null;move(g,w?"b1":"a3",w?"a3":"b1");end(g);boolean b=g.getPieceState(pos("h8"))!=null;move(g,b?"h8":"g8",b?"g8":"h8");end(g);}
    public static void grapple(Game g,String from,String target){cast(g,from,target,ChronomlinAbility.GRAPPLE);}
    public static void evolve(Game g,String from){cast(g,from,null,ChronomlinAbility.EVOLVE);}
    public static void borrowed(Game g,long actor,Abilities ability,String target){assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(g.getPieceState(actor).position(),ability,target==null?null:pos(target),null,true)));}
    public static Game ginsengReady(){
        var b=setup();put(b,Bishop.class,WHITE,"e2");Game g=AresTest.charge(AresTest.charge(create(b,Map.of(pos("e2"),HeroType.GINSENG_BISHOP)),"c2",1),"e2",2);evolve(g,"c2");cycle(g);cast(g,"e2","c2",GinsengAbility.COPY_SPELL);cycle(g);return g;
    }
    @Test public void allEightDirectionsWorkForAlliedAndEnemyAnchorsWithoutCapture()throws Exception{
        for(var color:List.of(WHITE,BLACK))for(int dr=-1;dr<=1;dr++)for(int dc=-1;dc<=1;dc++)if(dr!=0 || dc!=0){
            var b=setup();empty(b,"a1");empty(b,"c2");put(b,King.class,WHITE,"h1");put(b,Pawn.class,WHITE,"d4");Position target=new Position(3+3*dr,3+3*dc),landing=new Position(3+2*dr,3+2*dc);b.put(target,new Field(8).createFigure(Knight.class,target,color));Game g=at(b,Map.of(),"d4");long id=g.getPieceState(pos("d4")).id(),anchor=g.getPieceState(target).id();cast(g,"d4",cell(target),ChronomlinAbility.GRAPPLE);assertEquals(id,g.getPieceState(landing).id());assertEquals(anchor,g.getPieceState(target).id());assertEquals(0,g.getEconomy().whiteHalves());assertTrue(g.getSnapshot().whiteCaptures().isEmpty());AresTest.roundtrip(g);
        }
    }
    private static String cell(Position p){return ""+(char)('a'+p.getColumn())+(p.getRow()+1);}
    @Test public void grappleHasNoDistanceCapAndKeepsMandatoryMovementUnused()throws Exception{
        var b=setup();empty(b,"c2");put(b,Pawn.class,WHITE,"a2");put(b,Knight.class,BLACK,"a8");Game g=at(b,Map.of(),"a2");long id=g.getPieceState(pos("a2")).id();grapple(g,"a2","a8");assertEquals(id,g.getPieceState(pos("a7")).id());assertEquals(3,g.getPieceState(id).cooldown());assertFalse(g.getTurnState().moveUsed());assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));assertEquals(MovementRejection.ILLEGAL_MOVEMENT,g.moveFigure(g.getCurrentPlayer(),pos("a7"),pos("b8")));move(g,"b1","a3");end(g);AresTest.roundtrip(g);
    }
    @Test public void firstFigureBlocksAnyAnchorBehindItAndAdjacentAnchorDoesNotSpendResources(){
        var b=setup();put(b,Knight.class,BLACK,"c4");put(b,Knight.class,WHITE,"c7");Game g=create(b,Map.of());var before=g.getSnapshot();assertEquals(AbilityRejection.ABILITY_BLOCKED,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("c7"),ChronomlinAbility.GRAPPLE));assertEquals(before,g.getSnapshot());grapple(g,"c2","c4");assertNotNull(g.getPieceState(pos("c3")));cycle(g);cycle(g);cycle(g);before=g.getSnapshot();assertEquals(AbilityRejection.NO_GRAPPLE_MOVEMENT,g.useAbility(g.getCurrentPlayer(),pos("c3"),pos("c4"),ChronomlinAbility.GRAPPLE));assertEquals(before,g.getSnapshot());
    }
    @Test public void bothKingsCanBeAnchorsAndNeitherKingMoves()throws Exception{
        var b=setup();empty(b,"h8");put(b,King.class,BLACK,"c8");Game g=create(b,Map.of());long king=g.getPieceState(pos("c8")).id();grapple(g,"c2","c8");assertEquals(king,g.getPieceState(pos("c8")).id());assertEquals(Pawn.class,g.getPieceState(pos("c7")).type());AresTest.roundtrip(g);
        b=setup();empty(b,"a1");put(b,King.class,WHITE,"a4");g=create(b,Map.of());grapple(g,"c2","a4");assertEquals(Pawn.class,g.getPieceState(pos("b3")).type());assertEquals(King.class,g.getPieceState(pos("a4")).type());AresTest.roundtrip(g);
    }
    @Test public void emptyNonRayAndOwnSquareTargetsAreRejectedAtomically(){
        var b=setup();put(b,Knight.class,BLACK,"d5");Game g=create(b,Map.of());var before=g.getSnapshot();assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("c5"),ChronomlinAbility.GRAPPLE));assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("d5"),ChronomlinAbility.GRAPPLE));assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("c2"),ChronomlinAbility.GRAPPLE));assertEquals(before,g.getSnapshot());
    }
    @Test public void cooldownIsThreeOwnTurnStarts()throws Exception{
        Game g=basic();grapple(g,"c2","c6");cycle(g);assertEquals(2,g.getPieceState(pos("c5")).cooldown());assertEquals(AbilityRejection.ABILITY_COOLDOWN,g.getAbilityAvailability(pos("c5"),ChronomlinAbility.GRAPPLE));cycle(g);assertEquals(1,g.getPieceState(pos("c5")).cooldown());cycle(g);assertEquals(Success.SUCCESS,g.getAbilityAvailability(pos("c5"),ChronomlinAbility.GRAPPLE));AresTest.roundtrip(g);
    }
    @Test public void onePawnCapturePaysForOneUpgradeAndEachFormUsesItsOwnMoves()throws Exception{
        var b=setup();for(String s:List.of("d3","e4","f6"))put(b,Pawn.class,BLACK,s);Game g=create(b,Map.of());long id=g.getPieceState(pos("c2")).id();
        move(g,"c2","d3");assertEquals(1,g.getPieceState(id).ultimateCharge());evolve(g,"d3");assertEquals(Bishop.class,g.getPieceState(id).type());assertEquals(0,g.getPieceState(id).ultimateCharge());end(g);move(g,"h8","g8");end(g);
        move(g,"d3","e4");evolve(g,"e4");assertEquals(Knight.class,g.getPieceState(id).type());end(g);move(g,"g8","h8");end(g);
        move(g,"e4","f6");evolve(g,"f6");assertEquals(Rook.class,g.getPieceState(id).type());assertEquals(id,g.getPieceState(pos("f6")).id());assertEquals(HeroType.CHRONOMLIN_PAWN,g.getPieceState(id).hero());assertEquals(AbilityRejection.MAX_EVOLUTION,HeroAbilities.availability(g.getPieceState(id),ChronomlinAbility.EVOLVE,g.getSnapshot().heroRules()));assertEquals(6,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void evolutionDoesNotReplaceTheRequiredMoveAndCannotBeUsedWithoutCharge()throws Exception{
        Game g=basic();var before=g.getSnapshot();assertEquals(AbilityRejection.ULTIMATE_NOT_READY,g.useAbility(g.getCurrentPlayer(),pos("c2"),null,ChronomlinAbility.EVOLVE));assertEquals(before,g.getSnapshot());g=AresTest.charge(g,"c2",1);long id=g.getPieceState(pos("c2")).id();evolve(g,"c2");assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));assertEquals(MovementRejection.ILLEGAL_MOVEMENT,g.moveFigure(g.getCurrentPlayer(),pos("c2"),pos("c3")));move(g,"c2","d3");assertEquals(id,g.getPieceState(pos("d3")).id());end(g);AresTest.roundtrip(g);
    }
    @Test public void evolutionPreservesCooldownAndOtherPerPieceState()throws Exception{
        Game g=ready();grapple(g,"c2","c6");cycle(g);var s=g.getSnapshot();var states=new HashMap<>(s.pieces());var old=s.pieceAt(pos("c5"));states.put(old.id(),old.withBlindness(1));g=Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),states,s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),s.summons()),()->0L);evolve(g,"c5");var next=g.getPieceState(old.id());assertEquals(Bishop.class,next.type());assertEquals(2,next.cooldown());assertEquals(1,next.blindTurns());assertEquals(old.inventory(),next.inventory());AresTest.roundtrip(g);
    }
    @Test public void afterEvolutionLastRankIsAnOrdinaryBishopMoveWithoutPawnPromotion()throws Exception{
        var b=setup();empty(b,"c2");put(b,Pawn.class,WHITE,"c7");Game g=AresTest.charge(at(b,Map.of(),"c7"),"c7",1);evolve(g,"c7");assertEquals(MovementRejection.INVALID_PROMOTION,g.moveFigure(g.getCurrentPlayer(),pos("c7"),pos("b8"),Promotion.QUEEN));move(g,"c7","b8");assertEquals(Bishop.class,g.getPieceState(pos("b8")).type());assertNull(g.getMoves().getLast().getPromotion());AresTest.roundtrip(g);
    }
    @Test public void aStillUnchangedPawnKeepsOrdinaryLastRankPromotionAndItsHero()throws Exception{
        var b=setup();empty(b,"c2");put(b,Pawn.class,WHITE,"c7");put(b,Knight.class,BLACK,"g8");Game g=at(b,Map.of(),"c7");assertEquals(MovementRejection.PROMOTION_REQUIRED,g.moveFigure(g.getCurrentPlayer(),pos("c7"),pos("c8")));assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos("c7"),pos("c8"),Promotion.QUEEN));assertEquals(Queen.class,g.getPieceState(pos("c8")).type());assertEquals(HeroType.CHRONOMLIN_PAWN,g.getPieceState(pos("c8")).hero());assertEquals(AbilityRejection.MAX_EVOLUTION,g.getAbilityAvailability(pos("c8"),ChronomlinAbility.EVOLVE));AresTest.roundtrip(g);
    }
    @Test public void grapplingOutOfAPinIsRejectedWithoutSpendingAnything(){
        var b=setup();empty(b,"c2");put(b,Pawn.class,WHITE,"a2");put(b,Rook.class,BLACK,"a8");put(b,Knight.class,BLACK,"f7");Game g=at(b,Map.of(),"a2");var before=g.getSnapshot();assertEquals(MovementRejection.KING_IN_CHECK,g.useAbility(g.getCurrentPlayer(),pos("a2"),pos("f7"),ChronomlinAbility.GRAPPLE));assertEquals(before,g.getSnapshot());
    }
    @Test public void enemyMudBlocksCrossingButItsLandingTrapStillWorks()throws Exception{
        for(String mud:List.of("d4","d5")){
            var b=setup();empty(b,"c2");put(b,Pawn.class,WHITE,"d2");put(b,Knight.class,BLACK,"d6");put(b,Knight.class,BLACK,"f6");Game g=at(b,Map.of(pos("f6"),HeroType.BOLOTNIK_KNIGHT),"d2");long id=g.getPieceState(pos("d2")).id();move(g,"b1","a3");end(g);cast(g,"f6",mud,BolotnikAbility.MUD);move(g,"h8","g8");end(g);
            if(mud.equals("d4")){var before=g.getSnapshot();assertEquals(AbilityRejection.MUD_BLOCKED,g.useAbility(g.getCurrentPlayer(),pos("d2"),pos("d6"),ChronomlinAbility.GRAPPLE));assertEquals(before,g.getSnapshot());}
            else{grapple(g,"d2","d6");assertTrue(g.getPieceState(id).isRemoved());assertEquals(HeroType.BOLOTNIK_KNIGHT,g.getPieceState(pos("d5")).hero());assertEquals(4,g.getEconomy().blackHalves());}AresTest.roundtrip(g);
        }
    }
    @Test public void landingOnAnEnemyMineKillsChronomlinAndCreditsItsOwner()throws Exception{
        var b=setup();empty(b,"c2");put(b,Pawn.class,WHITE,"d2");put(b,Pawn.class,WHITE,"e4");put(b,Knight.class,BLACK,"d6");put(b,Pawn.class,BLACK,"d5");Game g=at(b,Map.of(pos("d5"),HeroType.MOLEMINER_PAWN),"d2");long id=g.getPieceState(pos("d2")).id();move(g,"b1","a3");end(g);cast(g,"d5",null,MoleminerAbility.PLANT_MINE);move(g,"d5","e4");end(g);grapple(g,"d2","d6");assertTrue(g.getPieceState(id).isRemoved());assertTrue(g.getSnapshot().mines().cells().isEmpty());assertEquals(6,g.getEconomy().blackHalves());AresTest.roundtrip(g);
    }
    @Test public void ginsengCopiesGrappleWithoutItsCooldownAndRetainsBishopIdentity()throws Exception{
        var b=setup();empty(b,"c2");put(b,Pawn.class,WHITE,"e2");put(b,Bishop.class,WHITE,"c2");put(b,Knight.class,BLACK,"e6");put(b,Knight.class,BLACK,"c8");Game g=AresTest.charge(at(b,Map.of(pos("c2"),HeroType.GINSENG_BISHOP),"e2"),"c2",2);long id=g.getPieceState(pos("c2")).id();grapple(g,"e2","e6");cycle(g);cast(g,"c2","e5",GinsengAbility.COPY_SPELL);cycle(g);borrowed(g,id,ChronomlinAbility.GRAPPLE,"c8");assertEquals(id,g.getPieceState(pos("c7")).id());assertEquals(Bishop.class,g.getPieceState(id).type());assertEquals(HeroType.GINSENG_BISHOP,g.getPieceState(id).hero());assertEquals(0,g.getPieceState(id).cooldown());assertTrue(g.getSpells().borrowed().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void ginsengCopiesEvolutionTwiceAndKeepsItsOwnSpellsAndStartingColor()throws Exception{
        Game g=ginsengReady();long id=g.getPieceState(pos("e2")).id();var color=g.getPieceState(id).startingColor();borrowed(g,id,ChronomlinAbility.EVOLVE,null);assertEquals(Knight.class,g.getPieceState(id).type());assertEquals(0,g.getPieceState(id).ultimateCharge());assertEquals(color,g.getPieceState(id).startingColor());cycle(g);g=AresTest.charge(g,"e2",2);cast(g,"e2","c2",GinsengAbility.COPY_SPELL);cycle(g);borrowed(g,id,ChronomlinAbility.EVOLVE,null);assertEquals(Rook.class,g.getPieceState(id).type());assertEquals(HeroType.GINSENG_BISHOP,g.getPieceState(id).hero());assertTrue(g.getSpells().borrowed().isEmpty());cycle(g);assertEquals(Success.SUCCESS,g.getAbilityAvailability(pos("e2"),GinsengAbility.DISPLACE));assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("e2"),GinsengAbility.DISPLACE,pos("e2"),pos("f2"),false)));assertEquals(Rook.class,g.getPieceState(id).type());AresTest.roundtrip(g);
    }
    @Test public void existingGinsengIllusionsKeepTheirBirthFormWhenOriginalEvolves()throws Exception{
        var b=setup();put(b,Bishop.class,WHITE,"e2");put(b,Knight.class,WHITE,"g1");Game g=AresTest.charge(AresTest.charge(AresTest.charge(create(b,Map.of(pos("e2"),HeroType.GINSENG_BISHOP,pos("g1"),HeroType.CLOWNGANGER_KNIGHT)),"c2",1),"e2",2),"g1",1);long id=g.getPieceState(pos("e2")).id();evolve(g,"c2");cycle(g);cast(g,"g1","f2",ClowngangerAbility.ILLUSION);cycle(g);cast(g,"e2","g1",GinsengAbility.COPY_SPELL);cycle(g);borrowed(g,id,ClowngangerAbility.ILLUSION,"f3");var illusion=g.getSnapshot().clown().illusions().values().stream().filter(i->i.sourceId()==id).findFirst().orElseThrow();long dummy=illusion.id();assertEquals(Bishop.class,g.getPieceState(dummy).type());cycle(g);g=AresTest.charge(g,cell(g.getPieceState(id).position()),2);cast(g,cell(g.getPieceState(id).position()),"c2",GinsengAbility.COPY_SPELL);cycle(g);borrowed(g,id,ChronomlinAbility.EVOLVE,null);assertEquals(Knight.class,g.getPieceState(id).type());assertEquals(Bishop.class,g.getPieceState(dummy).type());AresTest.roundtrip(g);
    }
    @Test public void maxFormRejectsAnotherUpgradeAndKeepsResources(){
        Game g=ginsengReady();long id=g.getPieceState(pos("e2")).id();borrowed(g,id,ChronomlinAbility.EVOLVE,null);cycle(g);g=AresTest.charge(g,"e2",2);cast(g,"e2","c2",GinsengAbility.COPY_SPELL);cycle(g);borrowed(g,id,ChronomlinAbility.EVOLVE,null);cycle(g);g=AresTest.charge(g,"e2",2);cast(g,"e2","c2",GinsengAbility.COPY_SPELL);cycle(g);var before=g.getSnapshot();assertEquals(AbilityRejection.MAX_EVOLUTION,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("e2"),ChronomlinAbility.EVOLVE,null,null,true)));assertEquals(before,g.getSnapshot());assertTrue(g.getSpells().borrowed().containsKey(id));
    }
    @Test public void invisibleOrBoxedAnchorsCannotBeTargeted()throws Exception{
        for(var hero:List.of(HeroType.FAUN_KNIGHT,HeroType.CLOWNGANGER_KNIGHT)){
            var b=setup();put(b,Knight.class,BLACK,"c6");Game g=AresTest.charge(create(b,Map.of(pos("c6"),hero)),"c6",hero==HeroType.FAUN_KNIGHT?2:0);move(g,"b1","a3");end(g);cast(g,"c6",hero==HeroType.FAUN_KNIGHT?null:"c6",hero==HeroType.FAUN_KNIGHT?FaunAbility.INVISIBILITY:ClowngangerAbility.BOX);move(g,"h8","g8");end(g);var before=g.getSnapshot();assertEquals(hero==HeroType.FAUN_KNIGHT?StatusRejection.TARGET_INVISIBLE:StatusRejection.BOXED,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("c6"),ChronomlinAbility.GRAPPLE));assertEquals(before,g.getSnapshot());AresTest.roundtrip(g);
        }
    }
    @Test public void evolvedRookGinsengCanCopyAndCreateARookIllusion()throws Exception{
        var b=setup();put(b,Bishop.class,WHITE,"e2");put(b,Knight.class,WHITE,"g1");Game g=AresTest.charge(AresTest.charge(AresTest.charge(create(b,Map.of(pos("e2"),HeroType.GINSENG_BISHOP,pos("g1"),HeroType.CLOWNGANGER_KNIGHT)),"c2",1),"e2",2),"g1",1);long id=g.getPieceState(pos("e2")).id();evolve(g,"c2");cycle(g);cast(g,"e2","c2",GinsengAbility.COPY_SPELL);cycle(g);borrowed(g,id,ChronomlinAbility.EVOLVE,null);cycle(g);g=AresTest.charge(g,"e2",2);cast(g,"e2","c2",GinsengAbility.COPY_SPELL);cycle(g);borrowed(g,id,ChronomlinAbility.EVOLVE,null);cycle(g);cast(g,"g1","f2",ClowngangerAbility.ILLUSION);cycle(g);g=AresTest.charge(g,"e2",2);cast(g,"e2","g1",GinsengAbility.COPY_SPELL);cycle(g);borrowed(g,id,ClowngangerAbility.ILLUSION,"f3");var dummy=g.getSnapshot().clown().illusions().values().stream().filter(i->i.sourceId()==id).findFirst().orElseThrow();assertEquals(Rook.class,g.getPieceState(dummy.id()).type());AresTest.roundtrip(g);if(g.getPieceState(pos("b1"))!=null)move(g,"b1","a3");else move(g,"a3","b1");end(g);assertEquals(HeroType.GINSENG_BISHOP,ClownView.displayed(g.getSnapshot(),g.getPieceState(dummy.id())).hero());
    }
    @Test public void friendlyMudDoesNotBlockGrappleAndCloudBlocksBothActiveAbilities()throws Exception{
        var b=setup();put(b,Knight.class,WHITE,"e2");put(b,Knight.class,BLACK,"c6");Game g=create(b,Map.of(pos("e2"),HeroType.BOLOTNIK_KNIGHT));cast(g,"e2","c4",BolotnikAbility.MUD);cycle(g);grapple(g,"c2","c6");assertEquals(HeroType.CHRONOMLIN_PAWN,g.getPieceState(pos("c5")).hero());AresTest.roundtrip(g);
        b=setup();put(b,Knight.class,BLACK,"e5");g=AresTest.charge(create(b,Map.of(pos("e5"),HeroType.FAUN_KNIGHT)),"c2",1);move(g,"b1","a3");end(g);cast(g,"e5","c2",FaunAbility.CLOUD);move(g,"h8","g8");end(g);assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("c2"),ChronomlinAbility.GRAPPLE));assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("c2"),ChronomlinAbility.EVOLVE));AresTest.roundtrip(g);
    }
    @Test public void evolutionDoesNotRestoreCastlingRights()throws Exception{
        Game g=ready();evolve(g,"c2");cycle(g);g=AresTest.charge(g,"c2",1);evolve(g,"c2");cycle(g);g=AresTest.charge(g,"c2",1);evolve(g,"c2");assertEquals(CastlingRights.none(),g.getSnapshot().castlingRights());AresTest.roundtrip(g);
    }
    @Test public void draftUsesPawnSlotsKeepsHeroesUniqueAndSandboxAddsOneChronomlin()throws Exception{
        var d=new Draft();assertEquals(10,d.getSnapshot().rules().catalogVersion());assertEquals(13,HeroCatalog.heroes().size());d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.CHRONOMLIN_PAWN,pos("h2")));assertNotEquals(DraftActionResult.SUCCESS,d.pick(BLACK,HeroType.CHRONOMLIN_PAWN,pos("h7")));d.finishPicks(BLACK);d.finishPicks(WHITE);Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);assertEquals(HeroType.CHRONOMLIN_PAWN,g.getPieceState(pos("h2")).hero());AresTest.roundtrip(g);g=Game.mobaHeroesSandbox(600);assertEquals(HeroType.CHRONOMLIN_PAWN,g.getPieceState(pos("f2")).hero());assertEquals(1,g.getSnapshot().pieces().values().stream().filter(p->p.hero()==HeroType.CHRONOMLIN_PAWN).count());evolve(g,"f2");assertEquals(Bishop.class,g.getPieceState(pos("f2")).type());AresTest.roundtrip(g);
    }
}
