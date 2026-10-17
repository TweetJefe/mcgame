package game.heroes.ginseng;

import game.core.Game;
import game.draft.*;
import game.heroes.*;
import game.moves.*;
import game.persistence.GameSaveFile;
import game.state.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.ginseng.GinsengFixtures.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class GinsengTest {
    private void move(Game g,String a,String b){assertEquals(MoveResult.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos(a),pos(b)));}
    private void end(Game g){assertEquals(MoveResult.SUCCESS,g.endTurn(g.getCurrentPlayer()));}
    private void cast(Game g,String a,String t,Ability spell){assertEquals(MoveResult.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos(a),t==null?null:pos(t),spell));}
    private void copy(Game g,String source){cast(g,"c2",source,Ability.COPY_SPELL);}
    private MoveResult borrowed(Game g,Ability spell,String target){return g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),spell,target==null?null:pos(target),null,true));}
    private void shift(Game g,String target,String landing){assertEquals(MoveResult.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.DISPLACE,pos(target),pos(landing),false)));}
    private void cycle(Game g) {
        move(g,g.getPieceState(pos("b1"))!=null?"b1":"a3",g.getPieceState(pos("b1"))!=null?"a3":"b1");end(g);
        move(g,g.getPieceState(pos("g8"))!=null?"g8":"f6",g.getPieceState(pos("g8"))!=null?"f6":"g8");end(g);
    }
    private void readyCopy(Game g,String source){cycle(g);copy(g,source);cycle(g);}
    private BorrowedAbility loan(Game g){return g.getSpells().borrowed().get(g.getPieceState(pos("c2")).id());}
    private void roundtrip(Game g)throws Exception {
        byte[] bytes=GameSaveFile.encode(g.getSnapshot());Game restored=Game.fromSnapshot(GameSaveFile.decode(bytes),()->0L);
        assertArrayEquals(bytes,GameSaveFile.encode(restored.getSnapshot()));assertEquals(g.getSpells(),restored.getSpells());
    }
    @Test public void displacementMovesOneFriendlyOrEnemyIntoAnyFreeNeighborAndDoesNotUseOrdinaryMove()throws Exception {
        for(FigureColor color:List.of(WHITE,BLACK)) {
            var b=board();put(b,Rook.class,color,"f5");Game g=create(b,Map.of());long id=g.getPieceState(pos("f5")).id();
            assertTrue(g.getAbilityTargets(pos("c2"),Ability.DISPLACE).contains(pos("f5")));
            assertTrue(g.getDisplacementDestinations(pos("c2"),pos("f5"),false).contains(pos("g6")));
            shift(g,"f5","g6");assertEquals(id,g.getPieceState(pos("g6")).id());assertEquals(3,g.getPieceState(pos("c2")).cooldown());
            assertFalse(g.getTurnState().moveUsed());assertTrue(g.getTurnState().abilityUsed());assertEquals(MoveResult.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));roundtrip(g);
        }
    }
    @Test public void kingWrongRangeOccupiedLandingAndOwnCheckAreAtomicRejections() {
        var b=board();put(b,Rook.class,BLACK,"a8");put(b,Pawn.class,WHITE,"a2");put(b,Pawn.class,BLACK,"h7");Game g=create(b,Map.of());var s=g.getSnapshot();
        assertEquals(MoveResult.KING_IMMUNE,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.DISPLACE,pos("a1"),pos("b2"),false)));
        assertEquals(MoveResult.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.DISPLACE,pos("h7"),pos("h6"),false)));
        assertEquals(MoveResult.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.DISPLACE,pos("a2"),pos("a1"),false)));
        assertEquals(MoveResult.KING_IN_CHECK,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.DISPLACE,pos("a2"),pos("b2"),false)));
        assertEquals(s,g.getSnapshot());
    }
    @Test public void casterCanShiftItselfAcrossSquareColorsAndCooldownLastsThreeOwnStarts() {
        Game g=create(board(),Map.of());long id=g.getPieceState(pos("c2")).id();SquareColor original=g.getPieceState(pos("c2")).startingColor();
        shift(g,"c2","d2");assertEquals(id,g.getPieceState(pos("d2")).id());assertEquals(original,g.getPieceState(pos("d2")).startingColor());
        cycle(g);assertEquals(2,g.getPieceState(pos("d2")).cooldown());cycle(g);assertEquals(1,g.getPieceState(pos("d2")).cooldown());cycle(g);
        assertEquals(MoveResult.SUCCESS,g.getAbilityAvailability(pos("d2"),Ability.DISPLACE));
    }
    @Test public void displacementCanOpenAnOrdinaryDiscoveredCheck() {
        var b=board();empty(b,"h8");put(b,King.class,BLACK,"e8");put(b,Rook.class,WHITE,"e1");put(b,Knight.class,BLACK,"e2");Game g=create(b,Map.of());
        shift(g,"e2","d2");move(g,"b1","a3");end(g);assertTrue(g.isInCheck());
    }
    @Test public void chargeCountsPawnsAndNoAbilityCanBeCopiedBeforeTheSourceCasts() {
        var b=board();put(b,Pawn.class,BLACK,"d3");put(b,Pawn.class,BLACK,"e4");put(b,Rook.class,WHITE,"e2");Game g=create(b,Map.of(pos("e2"),HeroType.PUDGE_ROOK));
        move(g,"c2","d3");assertEquals(1,g.getPieceState(pos("d3")).ultimateCharge());end(g);move(g,"g8","f6");end(g);move(g,"d3","e4");
        assertEquals(2,g.getPieceState(pos("e4")).ultimateCharge());var s=g.getSnapshot();
        assertEquals(MoveResult.NO_USED_ABILITY,g.useAbility(g.getCurrentPlayer(),pos("e4"),pos("e2"),Ability.COPY_SPELL));assertEquals(s,g.getSnapshot());
    }
    @Test public void copyUsesLatestSuccessfulCastAndOriginalKeepsItsState()throws Exception {
        Game g=hook();cast(g,"e2","e5",Ability.HOOK);cycle(g);cast(g,"e2","f2",Ability.SWALLOW);cycle(g);
        var source=g.getPieceState(pos("e2"));copy(g,"e2");assertEquals(Ability.SWALLOW,loan(g).ability());assertEquals(source,g.getPieceState(pos("e2")));
        assertEquals(0,g.getPieceState(pos("c2")).ultimateCharge());roundtrip(g);
    }
    @Test public void failedSourceCastDoesNotReplaceLastSuccessfulAbilityAndCopyFollowsPieceId()throws Exception {
        Game g=hook();cast(g,"e2","e5",Ability.HOOK);move(g,"e2","d2");end(g);move(g,"g8","f6");end(g);
        assertEquals(MoveResult.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("d2"),pos("a4"),Ability.SWALLOW));
        copy(g,"d2");assertEquals(Ability.HOOK,loan(g).ability());roundtrip(g);
    }
    @Test public void borrowedHookIsOneShotReadyNextOwnTurnAndPreservesNativeResources()throws Exception {
        Game g=hook();cast(g,"e2","e5",Ability.HOOK);cycle(g);copy(g,"e2");
        assertEquals(MoveResult.ABILITY_ALREADY_USED,borrowed(g,Ability.HOOK,"a4"));assertFalse(loan(g).isReady(g.getTurnState().number()));roundtrip(g);
        cycle(g);var p=g.getPieceState(pos("c2"));assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.HOOK,"a4"));
        assertNotNull(g.getPieceState(pos("b3")));assertEquals(p.ultimateCharge(),g.getPieceState(pos("c2")).ultimateCharge());assertEquals(p.cooldown(),g.getPieceState(pos("c2")).cooldown());
        assertNull(loan(g));cycle(g);assertEquals(MoveResult.NO_BORROWED_ABILITY,borrowed(g,Ability.HOOK,"b3"));roundtrip(g);
    }
    @Test public void failedBorrowedHookRetainsItsCopyAndNativeDisplacementStillHasAnIndependentCooldown()throws Exception {
        Game g=hook();cast(g,"e2","e5",Ability.HOOK);readyCopy(g,"e2");var s=g.getSnapshot();
        assertEquals(MoveResult.ABILITY_OUT_OF_RANGE,borrowed(g,Ability.HOOK,"e3"));assertEquals(s,g.getSnapshot());assertEquals(Ability.HOOK,loan(g).ability());
        shift(g,"e3","d3");assertEquals(3,g.getPieceState(pos("c2")).cooldown());cycle(g);
        assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.HOOK,"a4"));assertEquals(2,g.getPieceState(pos("c2")).cooldown());roundtrip(g);
    }
    @Test public void borrowedLaserBlindsWithoutChargingOrChangingNativeCooldown()throws Exception {
        var b=board();put(b,Bishop.class,WHITE,"e2");put(b,Rook.class,BLACK,"f4");Game g=charged(create(b,Map.of(pos("e2"),HeroType.LUMESHARD_BISHOP)),"c2");
        cast(g,"e2","f4",Ability.LASER);readyCopy(g,"e2");assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.LASER,"f4"));
        assertTrue(g.getPieceState(pos("f4")).isBlinded());assertEquals(0,g.getPieceState(pos("c2")).cooldown());assertNull(loan(g));roundtrip(g);
    }
    @Test public void borrowedTeleportUsesGinsengOriginalColorWithoutAnotherUltimateCharge()throws Exception {
        var b=board();put(b,Bishop.class,WHITE,"e2");Game g=charged(create(b,Map.of(pos("e2"),HeroType.LUMESHARD_BISHOP)),"c2","e2");
        cast(g,"e2","e4",Ability.TELEPORT);readyCopy(g,"e4");var before=g.getSnapshot();
        assertEquals(MoveResult.WRONG_SQUARE_COLOR,borrowed(g,Ability.TELEPORT,"h4"));assertEquals(before,g.getSnapshot());
        long id=g.getPieceState(pos("c2")).id();assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.TELEPORT,"a2"));
        assertEquals(id,g.getPieceState(pos("a2")).id());assertEquals(HeroType.GINSENG_BISHOP,g.getPieceState(pos("a2")).hero());assertEquals(Bishop.class,g.getPieceState(pos("a2")).type());
        assertEquals(0,g.getPieceState(pos("a2")).ultimateCharge());roundtrip(g);
    }
    @Test public void borrowedBountyPaysMarkBonusButDoesNotCopyCrowPassive()throws Exception {
        var b=board();put(b,Pawn.class,WHITE,"d2");put(b,Rook.class,BLACK,"h7");Game g=charged(create(b,Map.of(pos("d2"),HeroType.BOUNTY_CROW_PAWN)),"c2");
        cast(g,"d2","h7",Ability.BOUNTY);readyCopy(g,"d2");assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.BOUNTY,"h7"));move(g,"c2","h7");
        assertEquals(13,g.getEconomy().whiteHalves());assertEquals(1,g.getPieceState(pos("h7")).ultimateCharge());roundtrip(g);
    }
    @Test public void borrowedSwallowRetainsAllyAndCounterCapturesAfterTheCopyIsConsumed()throws Exception {
        var b=board();put(b,Rook.class,WHITE,"e2");put(b,Knight.class,WHITE,"f2");put(b,Knight.class,WHITE,"d2");put(b,Rook.class,BLACK,"c8");
        Game g=charged(create(b,Map.of(pos("e2"),HeroType.PUDGE_ROOK)),"c2","e2");cast(g,"e2","f2",Ability.SWALLOW);
        move(g,"a1","a2");end(g);move(g,"g8","f6");end(g);copy(g,"e2");move(g,"a2","a1");end(g);move(g,"f6","g8");end(g);
        long ally=g.getPieceState(pos("d2")).id();assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.SWALLOW,"d2"));assertNull(loan(g));roundtrip(g);
        move(g,"a1","a2");end(g);move(g,"c8","c2");assertEquals(ally,g.getPieceState(pos("c2")).id());assertEquals(Knight.class,g.getPieceState(pos("c2")).type());roundtrip(g);
    }
    @Test public void borrowedMudRemainsOwnedByGinsengAndTrapCaptureChargesItsNativeUltimate()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"e2");put(b,Rook.class,BLACK,"d8");Game g=charged(create(b,Map.of(pos("e2"),HeroType.BOLOTNIK_KNIGHT)),"c2");
        cast(g,"e2","f4",Ability.MUD);readyCopy(g,"e2");long id=g.getPieceState(pos("c2")).id();assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.MUD,"d3"));
        assertEquals(id,g.getTerrain().cells().get(pos("d3")).ownerId());move(g,"b1","a3");end(g);move(g,"d8","d3");
        assertEquals(id,g.getPieceState(pos("d3")).id());assertEquals(Bishop.class,g.getPieceState(pos("d3")).type());assertEquals(1,g.getPieceState(pos("d3")).ultimateCharge());roundtrip(g);
    }
    @Test public void borrowedAmbushHidesTheActualBishopAndItReturnsAsGinseng()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"e2");put(b,Knight.class,BLACK,"g4");put(b,Rook.class,BLACK,"c8");Game g=charged(create(b,Map.of(pos("e2"),HeroType.BOLOTNIK_KNIGHT)),"c2","e2");
        cast(g,"e2",null,Ability.AMBUSH);move(g,"b1","a3");end(g);move(g,"g4","f2");end(g);copy(g,"f2");cycle(g);
        long id=g.getPieceState(pos("c2")).id();assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.AMBUSH,null));
        assertEquals(PieceState.Status.HIDDEN_BOG,g.getSnapshot().pieces().get(id).status());assertFalse(g.getSpells().borrowed().containsKey(id));roundtrip(g);
        move(g,"b1","a3");end(g);move(g,"c8","c2");assertEquals(id,g.getPieceState(pos("c2")).id());assertEquals(Bishop.class,g.getPieceState(pos("c2")).type());assertEquals(HeroType.GINSENG_BISHOP,g.getPieceState(pos("c2")).hero());roundtrip(g);
    }
    @Test public void borrowedIzbaUltimateProducesARealBabaYagaWithGinsengAsItsSource()throws Exception {
        var b=board();put(b,Rook.class,WHITE,"e2");Game g=charged(create(b,Map.of(pos("e2"),HeroType.IZBA_ROOK)),"c2","e2");
        cast(g,"e2",null,Ability.SUMMON_YAGA);readyCopy(g,"e2");long id=g.getPieceState(pos("c2")).id();assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.SUMMON_YAGA,null));
        assertEquals(2,g.getSummons().records().size());assertTrue(g.getSummons().records().values().stream().anyMatch(r->r.sourceId()==id));assertNull(loan(g));roundtrip(g);
    }
    @Test public void copiedEffectsKeepExistingSwallowedAllyAndDoNotChangeNativeIdentity() {
        Game g=hook();var actor=g.getPieceState(pos("c2"));var ally=g.getPieceState(pos("f2"));actor=actor.withAbilityState(2,1,ally.id());
        var b=board();put(b,Rook.class,BLACK,"f4");var factory=new Field(8);var target=PieceState.initial(999,factory.createFigure(Rook.class,pos("f4"),BLACK),HeroType.NONE);
        var effect=BorrowedEffects.preview(g.getGameField(),actor,target,pos("f4"),Ability.LASER,g.getSnapshot().heroRules());var result=effect.updates().get(actor.id());
        assertEquals(MoveResult.SUCCESS,effect.result());assertEquals(Long.valueOf(ally.id()),result.storedPieceId());assertEquals(2,result.cooldown());assertEquals(1,result.ultimateCharge());assertEquals(HeroType.GINSENG_BISHOP,result.hero());
    }
    @Test public void borrowedDisplacementUsesTwoTargetsAndDoesNotStartNativeCooldown()throws Exception {
        var b=board();put(b,Bishop.class,WHITE,"f2");put(b,Pawn.class,BLACK,"f4");Game g=charged(create(b,Map.of(pos("f2"),HeroType.GINSENG_BISHOP)),"c2");
        assertEquals(MoveResult.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("f2"),Ability.DISPLACE,pos("f4"),pos("f5"),false)));
        readyCopy(g,"f2");assertEquals(Ability.DISPLACE,loan(g).ability());
        assertEquals(MoveResult.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.DISPLACE,pos("f5"),pos("g5"),true)));
        assertNotNull(g.getPieceState(pos("g5")));assertEquals(0,g.getPieceState(pos("c2")).cooldown());assertNull(loan(g));roundtrip(g);
    }
    @Test public void borrowingTheCopyUltimateCreatesANewOneShotCopyWithoutRecursiveFreeActions()throws Exception {
        var b=board();put(b,Rook.class,WHITE,"e2");put(b,Bishop.class,WHITE,"f2");put(b,Pawn.class,BLACK,"e5");
        Game g=charged(create(b,Map.of(pos("e2"),HeroType.PUDGE_ROOK,pos("f2"),HeroType.GINSENG_BISHOP)),"c2","f2");
        cast(g,"e2","e5",Ability.HOOK);cycle(g);cast(g,"f2","e2",Ability.COPY_SPELL);cycle(g);copy(g,"f2");cycle(g);
        assertEquals(Ability.COPY_SPELL,loan(g).ability());assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.COPY_SPELL,"e2"));
        assertEquals(Ability.HOOK,loan(g).ability());assertFalse(loan(g).isReady(g.getTurnState().number()));assertEquals(0,g.getPieceState(pos("c2")).ultimateCharge());
        assertEquals(MoveResult.ABILITY_ALREADY_USED,borrowed(g,Ability.HOOK,"e3"));roundtrip(g);
    }
    @Test public void anotherNativeCopyReplacesAnUnusedBorrowedSpell()throws Exception {
        var b=board();put(b,Rook.class,WHITE,"e2");put(b,Bishop.class,WHITE,"g2");put(b,Pawn.class,BLACK,"e5");Game g=charged(create(b,Map.of(pos("e2"),HeroType.PUDGE_ROOK,pos("g2"),HeroType.LUMESHARD_BISHOP)),"c2");
        cast(g,"e2","e5",Ability.HOOK);readyCopy(g,"e2");assertEquals(Ability.HOOK,loan(g).ability());cast(g,"g2","e3",Ability.LASER);cycle(g);
        g=charged(g,"c2");copy(g,"g2");assertEquals(Ability.LASER,loan(g).ability());assertEquals(g.getPieceState(pos("g2")).id(),loan(g).sourceId());roundtrip(g);
        var stored=g.getSpells();assertThrows(UnsupportedOperationException.class,()->stored.borrowed().clear());assertThrows(UnsupportedOperationException.class,()->stored.lastUsed().clear());
    }
    @Test public void enemyCanBeCopiedAcrossTheBoardAndItsDeathDoesNotRemoveTheCopy()throws Exception {
        var b=board();put(b,Rook.class,BLACK,"e7");put(b,Rook.class,WHITE,"e1");put(b,Pawn.class,WHITE,"b7");put(b,Pawn.class,BLACK,"a4");
        Game g=charged(create(b,Map.of(pos("e7"),HeroType.PUDGE_ROOK)),"c2");move(g,"b1","a3");end(g);cast(g,"e7","b7",Ability.HOOK);move(g,"g8","f6");end(g);
        long source=g.getPieceState(pos("e7")).id();copy(g,"e7");move(g,"e1","e7");end(g);move(g,"f6","g8");end(g);
        assertEquals(PieceState.Status.CAPTURED,g.getSnapshot().pieces().get(source).status());assertEquals(source,loan(g).sourceId());assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.HOOK,"a4"));roundtrip(g);
    }
    private Game readySteal(boolean remainingGold) {
        var b=board();empty(b,"g8");put(b,Pawn.class,WHITE,"d4");put(b,Pawn.class,BLACK,"e5");put(b,Pawn.class,BLACK,"f6");put(b,Rook.class,BLACK,"h4");
        put(b,remainingGold?Knight.class:Pawn.class,WHITE,"h3");Game g=charged(create(b,Map.of(pos("d4"),HeroType.BOUNTY_CROW_PAWN)),"c2");
        move(g,"d4","e5");end(g);move(g,"h4","h3");end(g);move(g,"e5","f6");end(g);move(g,"h8","g8");end(g);
        cast(g,"f6",null,Ability.STEAL);move(g,"f6","f7");end(g);move(g,"g8","h8");end(g);
        copy(g,"f7");move(g,"b1","a3");end(g);move(g,"h8","g7");end(g);return g;
    }
    @Test public void borrowedGoldTheftTakesOnlyTheRemainingBalanceAndUsesNoNewCharge()throws Exception {
        Game g=readySteal(true);assertEquals(2,g.getEconomy().blackHalves());assertEquals(MoveResult.SUCCESS,borrowed(g,Ability.STEAL,null));
        assertEquals(0,g.getEconomy().blackHalves());assertEquals(12,g.getEconomy().whiteHalves());assertEquals(0,g.getPieceState(pos("c2")).ultimateCharge());assertNull(loan(g));roundtrip(g);
    }
    @Test public void emptyEnemyWalletRejectsBorrowedTheftWithoutLosingItsCopy()throws Exception {
        Game g=readySteal(false);var before=g.getSnapshot();assertEquals(MoveResult.NO_GOLD_TO_STEAL,borrowed(g,Ability.STEAL,null));assertEquals(before,g.getSnapshot());assertEquals(Ability.STEAL,loan(g).ability());roundtrip(g);
    }
    @Test public void displacementIntoMudTriggersTheExistingTrapWithoutCreditingGinsengACapture()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"e2");put(b,Rook.class,BLACK,"f5");Game g=create(b,Map.of(pos("e2"),HeroType.BOLOTNIK_KNIGHT));
        cast(g,"e2","f4",Ability.MUD);cycle(g);long id=g.getPieceState(pos("e2")).id();shift(g,"f5","f4");
        assertEquals(id,g.getPieceState(pos("f4")).id());assertEquals(10,g.getEconomy().whiteHalves());assertEquals(0,g.getPieceState(pos("c2")).ultimateCharge());roundtrip(g);
    }
    @Test public void ordinaryMoveAndDisplacementCanBeUsedByTheSameBishopInEitherOrder() {
        Game first=create(board(),Map.of());shift(first,"c2","d2");move(first,"d2","e3");end(first);
        Game second=create(board(),Map.of());move(second,"c2","d3");assertEquals(MoveResult.SUCCESS,second.useAbility(second.getCurrentPlayer(),new AbilityRequest(pos("d3"),Ability.DISPLACE,pos("b1"),pos("b2"),false)));end(second);
    }
}
