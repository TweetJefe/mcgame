package game.heroes.faun;

import game.heroes.pudge.PudgeAbility;
import game.heroes.lumeshard.LumeshardAbility;
import game.heroes.ginseng.GinsengAbility;

import game.core.*;
import game.heroes.*;
import game.heroes.ginseng.GinsengFixtures;
import game.moves.*;
import game.persistence.GameSaveFile;
import game.state.*;
import game.draft.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static game.heroes.ginseng.GinsengFixtures.*;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class FaunTest {
    public static Game fixture(Map<Position,Figure> board,Map<Position,HeroType> heroes){return Game.fromMobaPosition(new GamePosition(board,WHITE,CastlingRights.none(),null,0),heroes,HeroRules.prototype(),()->0L);}
    public static Game basic(){var b=board();put(b,Knight.class,WHITE,"c2");return fixture(b,Map.of(pos("c2"),HeroType.FAUN_KNIGHT));}
    public static Game charged(){return GinsengFixtures.charged(basic(),"c2");}
    private void move(Game g,String a,String b){assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos(a),pos(b)));}
    private void end(Game g){assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer()));}
    private void cast(Game g,String actor,String target,Abilities a){assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos(actor),target==null?null:pos(target),a));}
    private void cycle(Game g){boolean white=g.getPieceState(pos("b1"))!=null;move(g,white?"b1":"a3",white?"a3":"b1");end(g);boolean black=g.getPieceState(pos("g8"))!=null;move(g,black?"g8":"f6",black?"f6":"g8");end(g);}
    public static void roundtrip(Game g)throws Exception{var bytes=GameSaveFile.encode(g.getSnapshot());var other=Game.fromSnapshot(GameSaveFile.decode(bytes),()->0L);assertArrayEquals(bytes,GameSaveFile.encode(other.getSnapshot()));assertEquals(g.getFaun(),other.getFaun());}

    @Test public void cloudIsThreeByThreeAndCenterRangeIsFourIncludingDiagonals()throws Exception {
        Game g=basic();cast(g,"c2","g6",FaunAbility.CLOUD);assertEquals(1,g.getFaun().clouds().size());
        var c=g.getFaun().clouds().getFirst();assertTrue(c.contains(pos("h7")));assertTrue(c.contains(pos("f5")));assertFalse(c.contains(pos("e5")));
        assertEquals(3,g.getPieceState(pos("c2")).cooldown());assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));roundtrip(g);
    }
    @Test public void cloudClipsToBoardCanCoverOccupiedSquaresAndDoesNotAffectKingAttacks()throws Exception {
        Game g=basic();assertTrue(g.getAbilityTargets(pos("c2"),FaunAbility.CLOUD).contains(pos("a1")));cast(g,"c2","a1",FaunAbility.CLOUD);
        assertTrue(g.getFaun().silenced(pos("a1")));assertTrue(g.getFaun().silenced(pos("b2")));assertFalse(g.isInCheck());roundtrip(g);
    }
    @Test public void cloudRejectsOutsideRadiusWithoutSpendingAnything() {
        Game g=basic();var s=g.getSnapshot();assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("h7"),FaunAbility.CLOUD));assertEquals(s,g.getSnapshot());
    }
    @Test public void cloudLastsTwoFollowingOwnEndsWhileCooldownNeedsThreeOwnStarts()throws Exception {
        Game g=basic();cast(g,"c2","f4",FaunAbility.CLOUD);cycle(g);assertEquals(2,g.getPieceState(pos("c2")).cooldown());assertFalse(g.getFaun().clouds().isEmpty());
        cycle(g);assertEquals(1,g.getPieceState(pos("c2")).cooldown());assertFalse(g.getFaun().clouds().isEmpty());cycle(g);
        assertTrue(g.getFaun().clouds().isEmpty());assertEquals(0,g.getPieceState(pos("c2")).cooldown());roundtrip(g);
    }
    @Test public void cloudBlocksAlliedAndEnemyActivesAndUltsButOrdinaryMovementStillWorks()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Rook.class,WHITE,"e2");put(b,Rook.class,BLACK,"f2");
        Game g=GinsengFixtures.charged(fixture(b,Map.of(pos("c2"),HeroType.FAUN_KNIGHT,pos("e2"),HeroType.PUDGE_ROOK,pos("f2"),HeroType.PUDGE_ROOK)),"e2","f2");
        cast(g,"c2","e2",FaunAbility.CLOUD);move(g,"b1","a3");end(g);
        assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("f2"),PudgeAbility.HOOK));assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("f2"),PudgeAbility.SWALLOW));
        move(g,"g8","f6");end(g);assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("e2"),PudgeAbility.HOOK));
        move(g,"e2","e4");assertEquals(Success.SUCCESS,g.getAbilityAvailability(pos("e4"),PudgeAbility.HOOK));roundtrip(g);
    }
    @Test public void casterCanSilenceItselfAndMovingOutRestoresItsUltimate()throws Exception {
        Game g=charged();cast(g,"c2","c2",FaunAbility.CLOUD);cycle(g);assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("c2"),FaunAbility.INVISIBILITY));
        move(g,"c2","e3");cast(g,"e3",null,FaunAbility.INVISIBILITY);assertTrue(g.getFaun().invisible().containsKey(g.getPieceState(pos("e3")).id()));roundtrip(g);
    }
    @Test public void invisibilityKeepsRealBoardIdentityAndMovementDoesNotReveal()throws Exception {
        Game g=charged();long id=g.getPieceState(pos("c2")).id();cast(g,"c2",null,FaunAbility.INVISIBILITY);move(g,"c2","d4");end(g);
        assertEquals(id,g.getPieceState(pos("d4")).id());assertTrue(g.getFaun().invisible().containsKey(id));assertTrue(g.getFaun().hidden(g.getPieceState(pos("d4")),BLACK));
        assertFalse(g.getFaun().hidden(g.getPieceState(pos("d4")),WHITE));assertFalse(FaunView.visible(g.getSnapshot(),g.getPieceState(pos("d4"))));roundtrip(g);
    }
    @Test public void invisibilityExpiresAtSecondFollowingOwnEnd()throws Exception {
        Game g=charged();cast(g,"c2",null,FaunAbility.INVISIBILITY);cycle(g);assertFalse(g.getFaun().invisible().isEmpty());cycle(g);assertFalse(g.getFaun().invisible().isEmpty());cycle(g);assertTrue(g.getFaun().invisible().isEmpty());roundtrip(g);
    }
    @Test public void takingAPawnRevealsFaunAndChargesItsUltimateAgain()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Pawn.class,BLACK,"f5");Game g=GinsengFixtures.charged(fixture(b,Map.of(pos("c2"),HeroType.FAUN_KNIGHT)),"c2");
        cast(g,"c2",null,FaunAbility.INVISIBILITY);move(g,"c2","d4");end(g);move(g,"g8","f6");end(g);move(g,"d4","f5");
        assertTrue(g.getFaun().invisible().isEmpty());assertEquals(1,g.getPieceState(pos("f5")).ultimateCharge());assertEquals(2,g.getEconomy().whiteHalves());roundtrip(g);
    }
    @Test public void successfulCloudRevealsButRejectedCastKeepsInvisibility()throws Exception {
        Game g=charged();cast(g,"c2",null,FaunAbility.INVISIBILITY);cycle(g);var before=g.getSnapshot();
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("h7"),FaunAbility.CLOUD));assertEquals(before,g.getSnapshot());
        cast(g,"c2","f4",FaunAbility.CLOUD);assertTrue(g.getFaun().invisible().isEmpty());roundtrip(g);
    }
    @Test public void enemyCanBlindlyCaptureInvisibleFaun()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Rook.class,BLACK,"d8");Game g=GinsengFixtures.charged(fixture(b,Map.of(pos("c2"),HeroType.FAUN_KNIGHT)),"c2");
        cast(g,"c2",null,FaunAbility.INVISIBILITY);move(g,"c2","d4");end(g);
        var hints=FaunView.moves(g.getSnapshot(),pos("d8"));assertTrue(hints.getCanMove().contains(pos("d4")));assertFalse(hints.getCanAttack().contains(pos("d4")));
        assertTrue(g.getSnapshot().events().stream().filter(e->e.kind()==game.history.GameEvent.Kind.MOVE).map(e->FaunView.message(g.getSnapshot(),e)).noneMatch(m->m.contains("d4")));
        move(g,"d8","d4");assertTrue(g.getFaun().invisible().isEmpty());assertEquals(BLACK,g.getPieceState(pos("d4")).color());roundtrip(g);
    }
    @Test public void invisibleEnemyCannotBeSelectedForLaserOrCopy()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Bishop.class,BLACK,"f5");Game g=GinsengFixtures.charged(fixture(b,Map.of(pos("c2"),HeroType.FAUN_KNIGHT,pos("f5"),HeroType.LUMESHARD_BISHOP)),"c2");
        cast(g,"c2",null,FaunAbility.INVISIBILITY);move(g,"c2","d4");end(g);var s=g.getSnapshot();
        assertFalse(g.getAbilityTargets(pos("f5"),LumeshardAbility.LASER).contains(pos("d4")));assertEquals(StatusRejection.TARGET_INVISIBLE,g.useAbility(g.getCurrentPlayer(),pos("f5"),pos("d4"),LumeshardAbility.LASER));assertEquals(s,g.getSnapshot());roundtrip(g);
    }
    @Test public void faunCanBeDraftedIntoEitherKnightSlotAndBabaRemainsExcluded()throws Exception {
        for(String square:List.of("b1","g1")){
            Draft d=new Draft();assertEquals(10,d.getSnapshot().rules().catalogVersion());assertEquals(13,HeroCatalog.heroes().size());
            d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.FAUN_KNIGHT,pos(square)));d.finishPicks(BLACK);d.finishPicks(WHITE);
            Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);assertEquals(HeroType.FAUN_KNIGHT,g.getPieceState(pos(square)).hero());roundtrip(g);
        }
        assertFalse(HeroCatalog.heroes().contains(HeroType.BABA_YAGA_PAWN));assertEquals(6,HeroCatalog.heroes(3).size());
    }
    @Test public void ginsengCanCopyCloudAndSilenceAlsoBlocksItsCopiedChannel()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"e2");Game g=GinsengFixtures.charged(GinsengFixtures.create(b,Map.of(pos("e2"),HeroType.FAUN_KNIGHT)),"c2");
        cast(g,"e2","h4",FaunAbility.CLOUD);cycle(g);cast(g,"c2","e2",GinsengAbility.COPY_SPELL);cycle(g);
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),FaunAbility.CLOUD,pos("c2"),null,true)));
        assertFalse(g.getFaun().clouds().isEmpty());assertTrue(g.getSpells().borrowed().isEmpty());assertEquals(0,g.getPieceState(pos("c2")).cooldown());cycle(g);
        assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("c2"),GinsengAbility.DISPLACE));assertEquals(StatusRejection.SILENCED,g.getAbilityAvailability(pos("c2"),FaunAbility.CLOUD,true));roundtrip(g);
    }
    @Test public void ginsengInvisibilityRetainsBishopAndRevealsOnNativeDisplacement()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"e2");Game g=GinsengFixtures.charged(GinsengFixtures.create(b,Map.of(pos("e2"),HeroType.FAUN_KNIGHT)),"c2","e2");
        cast(g,"e2",null,FaunAbility.INVISIBILITY);cycle(g);cast(g,"c2","e2",GinsengAbility.COPY_SPELL);cycle(g);
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),FaunAbility.INVISIBILITY,null,null,true)));long id=g.getPieceState(pos("c2")).id();assertTrue(g.getFaun().invisible().containsKey(id));assertEquals(Bishop.class,g.getPieceState(pos("c2")).type());roundtrip(g);
        cycle(g);assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),GinsengAbility.DISPLACE,pos("a3"),pos("b3"),false)));assertFalse(g.getFaun().invisible().containsKey(id));roundtrip(g);
    }
    @Test public void invisibleKnightStillAttacksSquaresAndProtectsThemFromTheEnemyKing()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"c2");empty(b,"h8");put(b,King.class,BLACK,"f6");
        Game g=GinsengFixtures.charged(fixture(b,Map.of(pos("c2"),HeroType.FAUN_KNIGHT)),"c2");
        cast(g,"c2",null,FaunAbility.INVISIBILITY);move(g,"c2","d4");end(g);
        assertEquals(MovementRejection.KING_IN_CHECK,g.moveFigure(g.getCurrentPlayer(),pos("f6"),pos("e6")));assertFalse(g.getFaun().invisible().isEmpty());roundtrip(g);
    }
    @Test public void cloudContinuesAfterItsCasterIsCaptured()throws Exception {
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Rook.class,BLACK,"c8");Game g=fixture(b,Map.of(pos("c2"),HeroType.FAUN_KNIGHT));
        cast(g,"c2","f4",FaunAbility.CLOUD);move(g,"b1","a3");end(g);move(g,"c8","c2");assertTrue(g.getFaun().silenced(pos("f4")));roundtrip(g);
    }
}
