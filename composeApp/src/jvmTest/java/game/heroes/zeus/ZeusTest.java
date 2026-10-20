package game.heroes.zeus;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.TurnRejection;
import game.core.*;
import game.heroes.*;
import game.heroes.ares.AresTest;
import game.heroes.moleminer.MoleminerTest;
import game.history.GameEvent;
import game.state.*;
import game.moves.*;
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

public class ZeusTest {
    public static Map<Position,Figure> setup(){var b=board();empty(b,"c2");empty(b,"g8");put(b,Rook.class,WHITE,"e2");return b;}
    public static Game create(Map<Position,Figure> b,Map<Position,HeroType> other){var h=new HashMap<>(other);h.put(pos("e2"),HeroType.ZEUS_ROOK);return MoleminerTest.seed(Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),h,HeroRules.prototype(),()->0L),0);}
    public static Game basic(){var b=setup();put(b,Knight.class,BLACK,"e4");return create(b,Map.of());}
    public static void cycle(Game g){boolean w=g.getPieceState(pos("b1"))!=null;move(g,w?"b1":"a3",w?"a3":"b1");end(g);boolean b=g.getPieceState(pos("h8"))!=null;move(g,b?"h8":"g8",b?"g8":"h8");end(g);}
    public static void chain(Game g,String target){cast(g,"e2",target,Ability.CHAIN_LIGHTNING);}
    public static int count(Game g,String caster,String target){return g.getSnapshot().lightning().count(g.getPieceState(pos(caster)).id(),g.getPieceState(pos(target)).id());}
    @Test public void skillAddsOneChargeCooldownOneAndStillRequiresOrdinaryMovement()throws Exception{
        Game g=basic();chain(g,"e4");assertEquals(1,count(g,"e2","e4"));assertEquals(1,g.getPieceState(pos("e2")).cooldown());assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));assertFalse(g.getTurnState().moveUsed());AresTest.roundtrip(g);cycle(g);assertEquals(0,g.getPieceState(pos("e2")).cooldown());assertEquals(Success.SUCCESS,g.getAbilityAvailability(pos("e2"),Ability.CHAIN_LIGHTNING));
    }
    @Test public void threeHitsKillEndTheChainAndResetAllChargesOfThisCaster()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d4");put(b,Knight.class,BLACK,"f4");put(b,Knight.class,BLACK,"h4");Game g=create(b,Map.of());chain(g,"d4");cycle(g);chain(g,"d4");assertEquals(2,count(g,"e2","f4"));cycle(g);chain(g,"d4");assertNull(g.getPieceState(pos("d4")));assertNotNull(g.getPieceState(pos("f4")));assertNotNull(g.getPieceState(pos("h4")));assertTrue(g.getSnapshot().lightning().charges().isEmpty());assertEquals(6,g.getEconomy().whiteHalves());assertEquals(1,g.getPieceState(pos("e2")).ultimateCharge());assertEquals(1,g.getEvents().stream().filter(e->e.turn()==5 && e.kind()==GameEvent.Kind.LIGHTNING_HIT).count());AresTest.roundtrip(g);
    }
    @Test public void chainVisitsThreeDifferentNearestEnemiesAndCanJumpOneSquare()throws Exception{
        var b=setup();for(String s:List.of("d4","f4","h4","b4"))put(b,Knight.class,BLACK,s);Game g=create(b,Map.of());chain(g,"d4");var hits=g.getSnapshot().lightning().pulses();assertEquals(3,hits.size());assertEquals(pos("d4"),hits.getFirst().square());assertEquals(3,hits.stream().map(LightningState.Pulse::targetId).distinct().count());assertFalse(g.getSnapshot().lightning().charges().containsKey(new LightningState.Key(g.getPieceState(pos("e2")).id(),g.getPieceState(pos("b1")).id())));AresTest.roundtrip(g);
    }
    @Test public void gapOfThreeStopsChainAndInitialRadiusIsThreeByEachAxis()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"b5");put(b,Knight.class,BLACK,"f5");Game g=create(b,Map.of());chain(g,"b5");assertEquals(1,g.getSnapshot().lightning().charges().size());AresTest.roundtrip(g);
    }
    @Test public void obstaclesDoNotBlockLightningButAlliesEmptyCellsAndKingsAreInvalid(){
        var b=setup();put(b,Pawn.class,WHITE,"e3");put(b,Knight.class,BLACK,"e5");Game g=create(b,Map.of());var before=g.getSnapshot();assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("e3"),Ability.CHAIN_LIGHTNING));assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("d3"),Ability.CHAIN_LIGHTNING));assertEquals(StatusRejection.KING_IMMUNE,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("h8"),Ability.CHAIN_LIGHTNING));assertEquals(before,g.getSnapshot());chain(g,"e5");assertEquals(1,count(g,"e2","e5"));
    }
    @Test public void outOfRangeRejectsWithoutSpendingResourcesOrRandomness(){
        var b=setup();put(b,Knight.class,BLACK,"e6");Game g=create(b,Map.of());var before=g.getSnapshot();assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("e6"),Ability.CHAIN_LIGHTNING));assertEquals(before,g.getSnapshot());
    }
    @Test public void expiryExcludesCastingTurnAndRefreshesAllStacksOnTarget()throws Exception{
        Game g=basic();chain(g,"e4");assertEquals(5,g.getSnapshot().lightning().charges().values().iterator().next().expiresAfterTurn());cycle(g);chain(g,"e4");assertEquals(7,g.getSnapshot().lightning().charges().values().iterator().next().expiresAfterTurn());cycle(g);assertEquals(2,count(g,"e2","e4"));cycle(g);assertEquals(2,count(g,"e2","e4"));cycle(g);assertTrue(g.getSnapshot().lightning().charges().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void ultimateNeedsTwoOwnCapturesAndAtLeastOneOwnChargedEnemy(){
        Game g=basic();assertEquals(AbilityRejection.ULTIMATE_NOT_READY,g.getAbilityAvailability(pos("e2"),Ability.THUNDERSTORM));g=AresTest.charge(g,"e2",2);var before=g.getSnapshot();assertEquals(AbilityRejection.NO_CHARGED_TARGETS,g.useAbility(g.getCurrentPlayer(),pos("e2"),null,Ability.THUNDERSTORM));assertEquals(before,g.getSnapshot());
    }
    @Test public void ultimateKillsFromOneStackSpendsChargeAndResetsOnlyItsPool()throws Exception{
        Game g=AresTest.charge(basic(),"e2",2);chain(g,"e4");cycle(g);cast(g,"e2",null,Ability.THUNDERSTORM);assertNull(g.getPieceState(pos("e4")));assertEquals(1,g.getPieceState(pos("e2")).ultimateCharge());assertTrue(g.getSnapshot().lightning().charges().isEmpty());assertEquals(6,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void ultimateChoosesOneOfOwnChargedEnemiesAndPreviewDoesNotAdvanceRng()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d4");put(b,Knight.class,BLACK,"f4");var selected=new HashSet<Position>();
        for(long seed=0;seed<12;seed++){Game g=MoleminerTest.seed(AresTest.charge(create(b,Map.of()),"e2",2),seed);chain(g,"d4");cycle(g);var before=g.getSnapshot();g.getAbilityTargets(pos("e2"),Ability.THUNDERSTORM);assertEquals(before,g.getSnapshot());cast(g,"e2",null,Ability.THUNDERSTORM);selected.add(g.getSnapshot().whiteCaptures().getFirst().getPosition());assertEquals(1,g.getSnapshot().whiteCaptures().size());AresTest.roundtrip(g);}assertEquals(Set.of(pos("d4"),pos("f4")),selected);
    }
    @Test public void tiedChainChoicesAreRandomButPreviewsArePure()throws Exception{
        var b=setup();for(String s:List.of("d4","b4","f4"))put(b,Knight.class,BLACK,s);var selected=new HashSet<Position>();
        for(long seed=0;seed<12;seed++){Game g=MoleminerTest.seed(create(b,Map.of()),seed);var before=g.getSnapshot();g.getAbilityTargets(pos("e2"),Ability.CHAIN_LIGHTNING);g.getAbilityTargets(pos("e2"),Ability.CHAIN_LIGHTNING);assertEquals(before,g.getSnapshot());chain(g,"d4");selected.add(g.getSnapshot().lightning().pulses().get(1).square());AresTest.roundtrip(g);}assertEquals(Set.of(pos("b4"),pos("f4")),selected);
    }
    @Test public void deterministicChainAndSingleTargetUltimateDoNotSpendRng(){
        Game g=AresTest.charge(basic(),"e2",2);long seed=g.getSnapshot().summons().randomState();chain(g,"e4");assertEquals(seed,g.getSnapshot().summons().randomState());cycle(g);cast(g,"e2",null,Ability.THUNDERSTORM);assertEquals(seed,g.getSnapshot().summons().randomState());
    }
    @Test public void sameTargetChargesOfZeusAndGinsengNeverCombineAndOnlyKillerPoolResets(){
        Game g=basic();var s=g.getSnapshot();var actor=s.pieceAt(pos("e2"));var target=s.pieceAt(pos("e4"));var ally=new PieceState(999,Bishop.class,WHITE,pos("c2"),PieceState.Status.BOARD,HeroType.GINSENG_BISHOP,0,0,actor.inventory(),null,0,SquareColor.at(pos("c2")));var pieces=new HashMap<>(s.pieces());pieces.put(ally.id(),ally);
        var lightning=LightningState.empty();for(int i=0;i<2;i++)lightning=lightning.pulse(new LightningState.Pulse(actor.id(),target.id(),1,target.position(),LightningState.Kind.HIT),s.heroRules().zeus());
        var first=ZeusAbilities.cast(s.field(),pieces,ally,target,Ability.CHAIN_LIGHTNING,lightning,s.summons(),1,s.heroRules(),true);assertTrue(first.effect().captures().isEmpty());assertEquals(2,first.lightning().count(actor.id(),target.id()));assertEquals(1,first.lightning().count(ally.id(),target.id()));
        putOtherTarget(pieces,s);long other=998;lightning=first.lightning().pulse(new LightningState.Pulse(ally.id(),other,1,pos("h4"),LightningState.Kind.HIT),s.heroRules().zeus());var killed=ZeusAbilities.cast(s.field(),pieces,actor,target,Ability.CHAIN_LIGHTNING,lightning,s.summons(),1,s.heroRules(),false);assertEquals(1,killed.effect().captures().size());assertEquals(0,killed.lightning().count(actor.id(),target.id()));assertEquals(1,killed.lightning().count(ally.id(),other));assertEquals(0,killed.lightning().count(ally.id(),target.id()));
    }
    private static void putOtherTarget(Map<Long,PieceState> pieces,GameSnapshot s){pieces.put(998L,PieceState.initial(998,s.field().createFigure(Knight.class,pos("h4"),BLACK),HeroType.NONE));}
    @Test public void ginsengCanCopyLightningAndOwnsTheResultingCharges()throws Exception{
        var b=setup();put(b,Bishop.class,WHITE,"c2");put(b,Knight.class,BLACK,"e4");Game g=AresTest.charge(create(b,Map.of(pos("c2"),HeroType.GINSENG_BISHOP)),"c2",2);chain(g,"e4");cycle(g);cast(g,"c2","e2",Ability.COPY_SPELL);cycle(g);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.CHAIN_LIGHTNING,pos("e4"),null,true)));assertEquals(1,count(g,"e2","e4"));assertEquals(1,count(g,"c2","e4"));assertEquals(0,g.getPieceState(pos("c2")).cooldown());assertTrue(g.getSpells().borrowed().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void remoteAresRevengeKillsZeusAtItsRealSquare()throws Exception{
        var b=setup();put(b,Bishop.class,BLACK,"e4");Game g=AresTest.charge(AresTest.charge(create(b,Map.of(pos("e4"),HeroType.ARES_BISHOP)),"e4",1),"e2",2);chain(g,"e4");cycle(g);cast(g,"e2",null,Ability.THUNDERSTORM);assertNull(g.getPieceState(pos("e2")));assertNull(g.getPieceState(pos("e4")));assertEquals(6,g.getEconomy().whiteHalves());assertEquals(10,g.getEconomy().blackHalves());AresTest.roundtrip(g);
    }
    @Test public void lightningDeathOfIzbaTriggersItsPassiveExactlyOnce()throws Exception{
        var b=setup();put(b,Rook.class,BLACK,"e4");Game g=AresTest.charge(create(b,Map.of(pos("e4"),HeroType.IZBA_ROOK)),"e2",2);chain(g,"e4");cycle(g);cast(g,"e2",null,Ability.THUNDERSTORM);assertEquals(1,g.getSnapshot().summons().records().size());assertEquals(10,g.getEconomy().whiteHalves());AresTest.roundtrip(g);
    }
    @Test public void chargedPudgeReleasesPassengerAndRetaliatesAgainstRemoteZeus()throws Exception{
        var b=setup();put(b,Rook.class,BLACK,"e4");put(b,Knight.class,BLACK,"d5");Game g=AresTest.charge(AresTest.charge(create(b,Map.of(pos("e4"),HeroType.PUDGE_ROOK)),"e4",2),"e2",2);chain(g,"e4");move(g,"b1","a3");end(g);cast(g,"e4","d5",Ability.SWALLOW);move(g,"h8","g8");end(g);cast(g,"e2",null,Ability.THUNDERSTORM);assertNull(g.getPieceState(pos("e2")));assertEquals(Knight.class,g.getPieceState(pos("e4")).type());assertEquals(BLACK,g.getPieceState(pos("e4")).color());AresTest.roundtrip(g);
    }
    @Test public void ordinaryCaptureRemovesOnlyTheDeadTargetsCharges()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"e4");put(b,Knight.class,BLACK,"f4");Game g=create(b,Map.of());chain(g,"e4");move(g,"e2","e4");assertEquals(1,g.getSnapshot().lightning().charges().size());assertEquals(1,count(g,"e4","f4"));AresTest.roundtrip(g);
    }
    @Test public void cloudSilencesZeusAndBoxedTargetsAreProtected()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"e4");put(b,Knight.class,BLACK,"d4");Game g=create(b,Map.of(pos("d4"),HeroType.FAUN_KNIGHT));move(g,"b1","a3");end(g);cast(g,"d4","e2",Ability.CLOUD);move(g,"h8","g8");end(g);assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("e2"),Ability.CHAIN_LIGHTNING));AresTest.roundtrip(g);
        b=setup();put(b,Knight.class,BLACK,"e4");put(b,Knight.class,BLACK,"d4");g=create(b,Map.of(pos("d4"),HeroType.CLOWNGANGER_KNIGHT));move(g,"b1","a3");end(g);cast(g,"d4","e4",Ability.BOX);move(g,"h8","g8");end(g);var before=g.getSnapshot();assertEquals(StatusRejection.BOXED,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("e4"),Ability.CHAIN_LIGHTNING));assertEquals(before,g.getSnapshot());AresTest.roundtrip(g);
    }
    @Test public void protectedTargetsAreSkippedByBouncesAndUltimate()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"e4");put(b,Knight.class,BLACK,"f4");put(b,Knight.class,BLACK,"d4");Game g=AresTest.charge(create(b,Map.of(pos("d4"),HeroType.CLOWNGANGER_KNIGHT)),"e2",2);chain(g,"e4");move(g,"b1","a3");end(g);cast(g,"d4","f4",Ability.BOX);move(g,"h8","g8");end(g);cast(g,"e2",null,Ability.THUNDERSTORM);assertNotNull(g.getPieceState(pos("f4")));assertTrue(g.getPieceState(pos("f4")).isBoxed());AresTest.roundtrip(g);
    }
    @Test public void lightningCannotLeaveOwnKingExposedAndRejectionIsAtomic()throws Exception{
        var b=setup();empty(b,"a1");empty(b,"b1");put(b,King.class,WHITE,"b1");put(b,Knight.class,WHITE,"c1");put(b,Rook.class,BLACK,"b8");put(b,Knight.class,BLACK,"b4");Game g=create(b,Map.of());chain(g,"b4");move(g,"c1","a2");end(g);move(g,"h8","g8");end(g);chain(g,"b4");move(g,"a2","c1");end(g);move(g,"g8","h8");end(g);var before=g.getSnapshot();assertEquals(MovementRejection.KING_IN_CHECK,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("b4"),Ability.CHAIN_LIGHTNING));assertEquals(before,g.getSnapshot());AresTest.roundtrip(g);
    }
    @Test public void invisibleEnemiesCannotBeFirstTargetsButCanBeHitByBouncesAndUltimate()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d4");put(b,Knight.class,BLACK,"e4");Game g=AresTest.charge(AresTest.charge(create(b,Map.of(pos("d4"),HeroType.FAUN_KNIGHT)),"d4",2),"e2",2);move(g,"b1","a3");end(g);cast(g,"d4",null,Ability.INVISIBILITY);move(g,"h8","g8");end(g);assertEquals(StatusRejection.TARGET_INVISIBLE,g.useAbility(g.getCurrentPlayer(),pos("e2"),pos("d4"),Ability.CHAIN_LIGHTNING));chain(g,"e4");assertEquals(1,count(g,"e2","d4"));move(g,"e2","e4");end(g);move(g,"g8","h8");end(g);assertTrue(g.getSnapshot().faun().invisible().containsKey(g.getPieceState(pos("d4")).id()));cast(g,"e4",null,Ability.THUNDERSTORM);assertNull(g.getPieceState(pos("d4")));assertTrue(g.getSnapshot().faun().invisible().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void dischargingAnIllusionGivesNoCaptureRewardAndExtendsItOnce()throws Exception{
        var b=setup();put(b,Knight.class,BLACK,"d4");Game g=AresTest.charge(AresTest.charge(create(b,Map.of(pos("d4"),HeroType.CLOWNGANGER_KNIGHT)),"d4",1),"e2",2);move(g,"b1","a3");end(g);cast(g,"d4","c4",Ability.ILLUSION);assertEquals(HeroType.ILLUSION,g.getPieceState(pos("c4")).hero());long dummy=g.getPieceState(pos("c4")).id();move(g,"d4","f5");end(g);chain(g,"c4");assertEquals(1,g.getSnapshot().lightning().charges().size());move(g,"a3","b1");end(g);move(g,"h8","g8");end(g);cast(g,"e2",null,Ability.THUNDERSTORM);assertEquals(dummy,g.getPieceState(pos("c4")).id());assertTrue(g.getSnapshot().clown().illusions().get(dummy).extended());assertEquals(8,g.getSnapshot().clown().illusions().get(dummy).expiresAfterTurn());assertEquals(0,g.getEconomy().whiteHalves());assertEquals(0,g.getPieceState(pos("e2")).ultimateCharge());assertTrue(g.getSnapshot().whiteCaptures().isEmpty());assertTrue(g.getSnapshot().lightning().charges().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void ginsengCanCopyUltimateAndUseItsOwnEarlierChargesWithoutPayingAgain()throws Exception{
        var b=board();empty(b,"g8");put(b,Rook.class,BLACK,"e5");put(b,Knight.class,BLACK,"d4");put(b,Pawn.class,WHITE,"b5");Game g=MoleminerTest.seed(Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),Map.of(pos("c2"),HeroType.GINSENG_BISHOP,pos("e5"),HeroType.ZEUS_ROOK),HeroRules.prototype(),()->0L),0);g=AresTest.charge(AresTest.charge(g,"c2",2),"e5",2);
        move(g,"a1","a2");end(g);cast(g,"e5","b5",Ability.CHAIN_LIGHTNING);move(g,"h8","g8");end(g);cast(g,"c2","e5",Ability.COPY_SPELL);move(g,"a2","a1");end(g);move(g,"g8","h8");end(g);
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.CHAIN_LIGHTNING,pos("d4"),null,true)));assertEquals(1,count(g,"c2","d4"));move(g,"a1","a2");end(g);cast(g,"e5",null,Ability.THUNDERSTORM);move(g,"h8","g8");end(g);assertEquals(2,g.getSnapshot().lightning().charges().size());g=AresTest.charge(g,"c2",2);cast(g,"c2","e5",Ability.COPY_SPELL);move(g,"a2","a1");end(g);move(g,"g8","h8");end(g);
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.THUNDERSTORM,null,null,true)));assertEquals(1,g.getSnapshot().whiteCaptures().size());assertEquals(1,g.getPieceState(pos("c2")).ultimateCharge());assertTrue(g.getSnapshot().lightning().charges().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void draftHasElevenUniqueHeroesAndSandboxContainsOnlyOneZeus()throws Exception{
        var d=new Draft();assertEquals(10,d.getSnapshot().rules().catalogVersion());assertEquals(13,HeroCatalog.heroes().size());d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.ZEUS_ROOK,pos("h1")));assertNotEquals(DraftActionResult.SUCCESS,d.pick(BLACK,HeroType.ZEUS_ROOK,pos("a8")));d.finishPicks(BLACK);d.finishPicks(WHITE);Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);assertEquals(HeroType.ZEUS_ROOK,g.getPieceState(pos("h1")).hero());AresTest.roundtrip(g);g=Game.mobaHeroesSandbox(600);assertEquals(1,g.getSnapshot().pieces().values().stream().filter(p->p.hero()==HeroType.ZEUS_ROOK).count());assertEquals(HeroType.ZEUS_ROOK,g.getPieceState(pos("e2")).hero());AresTest.roundtrip(g);
    }
    @Test public void duplicateZeusAssignmentsAreRejected(){var b=setup();put(b,Rook.class,BLACK,"e7");assertThrows(IllegalArgumentException.class,()->create(b,Map.of(pos("e7"),HeroType.ZEUS_ROOK)));}
}
