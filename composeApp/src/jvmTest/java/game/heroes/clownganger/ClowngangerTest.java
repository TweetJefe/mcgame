package game.heroes.clownganger;
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
public class ClowngangerTest {
    public static Game basic(){var b=board();put(b,Knight.class,WHITE,"c2");return createClown(b,Map.of());}
    public static Game createClown(Map<Position,Figure>b,Map<Position,HeroType> other){var h=new HashMap<>(other);h.put(pos("c2"),HeroType.CLOWNGANGER_KNIGHT);var g=Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),h,HeroRules.prototype(),()->0L);var s=g.getSnapshot();return Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),s.pieces(),s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),game.summoning.SummonState.initial(0)),()->0L);}
    public static Game withIllusion(){Game g=AresTest.charge(basic(),"c2",1);cast(g,"c2","d2",Ability.ILLUSION);return g;}
    public static void move(Game g,String a,String b){assertEquals(Success.SUCCESS,g.moveFigure(g.getCurrentPlayer(),pos(a),pos(b)));}
    public static void end(Game g){assertEquals(Success.SUCCESS,g.endTurn(g.getCurrentPlayer()));}
    public static void cast(Game g,String a,String b,Ability spell){assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos(a),pos(b),spell));}
    private void cycle(Game g){boolean w=g.getPieceState(pos("b1"))!=null;move(g,w?"b1":"a3",w?"a3":"b1");end(g);boolean b=g.getPieceState(pos("g8"))!=null;move(g,b?"g8":"f6",b?"f6":"g8");end(g);}
    @Test public void boxCanProtectSelfAndStillRequiresAnotherOrdinaryMove()throws Exception{
        Game g=basic();cast(g,"c2","c2",Ability.BOX);assertTrue(g.getPieceState(pos("c2")).isBoxed());assertEquals(3,g.getPieceState(pos("c2")).cooldown());
        assertEquals(StatusRejection.BOXED,g.moveFigure(g.getCurrentPlayer(),pos("c2"),pos("e3")));assertEquals(TurnRejection.ABILITY_ALREADY_USED,g.getAbilityAvailability(pos("c2"),Ability.BOX));assertEquals(TurnRejection.MOVE_REQUIRED,g.endTurn(g.getCurrentPlayer()));AresTest.roundtrip(g);
    }
    @Test public void boxRangeEnemyAndEmptyTargetsRejectAtomically(){var b=board();put(b,Knight.class,WHITE,"c2");put(b,Pawn.class,WHITE,"h6");Game g=createClown(b,Map.of());var s=g.getSnapshot();assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("h6"),Ability.BOX));assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("g8"),Ability.BOX));assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,g.useAbility(g.getCurrentPlayer(),pos("c2"),pos("d2"),Ability.BOX));assertEquals(s,g.getSnapshot());}
    @Test public void boxedKingIsSafeButTheTurnCannotEndWhenProtectionExpiresIntoCheck()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Rook.class,BLACK,"a8");put(b,Rook.class,WHITE,"b2");Game g=createClown(b,Map.of());assertTrue(g.isInCheck());
        cast(g,"c2","a1",Ability.BOX);assertFalse(g.isInCheck());move(g,"b1","a3");end(g);move(g,"g8","f6");end(g);assertTrue(g.getPieceState(pos("a1")).isBoxed());
        assertEquals(StatusRejection.BOXED,g.moveFigure(g.getCurrentPlayer(),pos("a1"),pos("b1")));assertEquals(TurnRejection.NO_TURN_CONTINUATION,g.moveFigure(g.getCurrentPlayer(),pos("a3"),pos("b1")));
        move(g,"b2","a2");end(g);assertFalse(g.getPieceState(pos("a1")).isBoxed());AresTest.roundtrip(g);
    }
    @Test public void boxExpiresAfterOneFollowingOwnEndAndCooldownNeedsThreeOwnStarts()throws Exception{
        Game g=basic();cast(g,"c2","c2",Ability.BOX);cycle(g);assertTrue(g.getPieceState(pos("c2")).isBoxed());assertEquals(2,g.getPieceState(pos("c2")).cooldown());cycle(g);assertFalse(g.getPieceState(pos("c2")).isBoxed());assertEquals(1,g.getPieceState(pos("c2")).cooldown());cycle(g);assertEquals(Success.SUCCESS,g.getAbilityAvailability(pos("c2"),Ability.BOX));AresTest.roundtrip(g);
    }
    @Test public void protectedAllyCannotBeCapturedOrPulled()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Pawn.class,WHITE,"d2");put(b,Rook.class,BLACK,"d5");Game g=createClown(b,Map.of(pos("d5"),HeroType.PUDGE_ROOK));cast(g,"c2","d2",Ability.BOX);move(g,"b1","a3");end(g);var s=g.getSnapshot();
        assertEquals(StatusRejection.BOXED,g.moveFigure(g.getCurrentPlayer(),pos("d5"),pos("d2")));assertEquals(StatusRejection.BOXED,g.useAbility(g.getCurrentPlayer(),pos("d5"),pos("d2"),Ability.HOOK));assertEquals(s,g.getSnapshot());AresTest.roundtrip(g);
    }
    @Test public void boxBlocksAttacksAndSpearsStopBeforeIt()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Rook.class,WHITE,"d2");put(b,Bishop.class,BLACK,"a2");Game g=createClown(b,Map.of(pos("a2"),HeroType.ARES_BISHOP));cast(g,"c2","d2",Ability.BOX);move(g,"b1","a3");end(g);cast(g,"a2","d2",Ability.SPEAR);assertTrue(g.getPieceState(pos("d2")).isBoxed());assertFalse(g.getPieceState(pos("d2")).isStunned());AresTest.roundtrip(g);
    }
    @Test public void illusionCostsOneOwnCaptureIncludingPawn()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Pawn.class,BLACK,"e3");Game g=createClown(b,Map.of());assertEquals(AbilityRejection.ULTIMATE_NOT_READY,g.getAbilityAvailability(pos("c2"),Ability.ILLUSION));move(g,"c2","e3");assertEquals(1,g.getPieceState(pos("e3")).ultimateCharge());cast(g,"e3","d3",Ability.ILLUSION);assertEquals(0,g.getPieceState(pos("e3")).ultimateCharge());assertEquals(HeroType.ILLUSION,g.getPieceState(pos("d3")).hero());AresTest.roundtrip(g);
    }
    @Test public void bothBodiesMoveInEitherOrderButNoBodyMovesTwiceOrThirdPieceMoves()throws Exception{
        for(boolean originalFirst:List.of(true,false)){Game g=withIllusion();move(g,originalFirst?"c2":"d2",originalFirst?"e3":"f3");assertTrue(g.getTurnState().moveUsed());assertFalse(g.getLegalMoves(pos(originalFirst?"d2":"c2")).getCanMove().isEmpty());
            move(g,originalFirst?"d2":"c2",originalFirst?"f3":"e3");assertEquals(TurnRejection.MOVE_ALREADY_USED,g.moveFigure(g.getCurrentPlayer(),pos("e3"),pos("c2")));assertEquals(TurnRejection.MOVE_ALREADY_USED,g.moveFigure(g.getCurrentPlayer(),pos("b1"),pos("a3")));AresTest.roundtrip(g);end(g);AresTest.roundtrip(g);}
    }
    @Test public void oneBodyIsEnoughButAnUnrelatedMovePreventsBothBodiesFromMoving()throws Exception{
        Game g=withIllusion();move(g,"c2","e3");end(g);AresTest.roundtrip(g);g=withIllusion();move(g,"b1","a3");assertEquals(TurnRejection.MOVE_ALREADY_USED,g.moveFigure(g.getCurrentPlayer(),pos("d2"),pos("f3")));AresTest.roundtrip(g);
    }
    @Test public void illusionBlocksPassageButCannotCaptureOrGiveCheck()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Pawn.class,BLACK,"f3");put(b,Rook.class,BLACK,"d8");Game g=AresTest.charge(createClown(b,Map.of()),"c2",1);cast(g,"c2","d2",Ability.ILLUSION);var s=g.getSnapshot();assertEquals(StatusRejection.ILLUSION_CANNOT_CAPTURE,g.moveFigure(g.getCurrentPlayer(),pos("d2"),pos("f3")));assertEquals(s,g.getSnapshot());assertTrue(rules.moba.AttackRules.blindSquares(g.getSnapshot().pieces()).contains(pos("d2")));move(g,"b1","a3");end(g);assertEquals(MovementRejection.ILLEGAL_MOVEMENT,g.moveFigure(g.getCurrentPlayer(),pos("d8"),pos("d1")));AresTest.roundtrip(g);
    }
    @Test public void attemptedCaptureReturnsAttackerAndExtendsOnlyOnceWithoutGoldOrCharge()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Rook.class,BLACK,"d8");Game g=AresTest.charge(createClown(b,Map.of(pos("d8"),HeroType.PUDGE_ROOK)),"c2",1);cast(g,"c2","d2",Ability.ILLUSION);long id=g.getPieceState(pos("d2")).id();move(g,"b1","a3");end(g);move(g,"d8","d2");
        assertEquals(HeroType.ILLUSION,g.getPieceState(pos("d2")).hero());assertEquals(HeroType.PUDGE_ROOK,g.getPieceState(pos("d8")).hero());assertEquals(0,g.getPieceState(pos("d8")).ultimateCharge());assertEquals(0,g.getEconomy().blackHalves());assertEquals(7,g.getSnapshot().clown().illusions().get(id).expiresAfterTurn());assertTrue(g.getSnapshot().blackCaptures().isEmpty());AresTest.roundtrip(g);end(g);
        move(g,"a3","b1");end(g);move(g,"d8","d2");assertEquals(7,g.getSnapshot().clown().illusions().get(id).expiresAfterTurn());AresTest.roundtrip(g);
    }
    @Test public void illusionExpiresAtSecondFollowingOwnEnd()throws Exception{
        Game g=withIllusion();long id=g.getPieceState(pos("d2")).id();cycle(g);assertEquals(PieceState.Status.BOARD,g.getPieceState(id).status());cycle(g);assertEquals(PieceState.Status.BOARD,g.getPieceState(id).status());move(g,"b1","a3");end(g);assertEquals(PieceState.Status.DISSOLVED,g.getPieceState(id).status());AresTest.roundtrip(g);
    }
    @Test public void originalDeathImmediatelyDismissesIllusionWithoutPayingForDummy()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Rook.class,BLACK,"c8");Game g=AresTest.charge(createClown(b,Map.of()),"c2",1);cast(g,"c2","d2",Ability.ILLUSION);long id=g.getPieceState(pos("d2")).id();move(g,"b1","a3");end(g);move(g,"c8","c2");assertEquals(PieceState.Status.DISSOLVED,g.getPieceState(id).status());assertEquals(6,g.getEconomy().blackHalves());AresTest.roundtrip(g);
    }
    @Test public void clownCanBoxHisOwnIllusion()throws Exception{
        Game g=withIllusion();cycle(g);cast(g,"c2","d2",Ability.BOX);assertTrue(g.getPieceState(pos("d2")).isBoxed());assertEquals(StatusRejection.BOXED,g.moveFigure(g.getCurrentPlayer(),pos("d2"),pos("f3")));AresTest.roundtrip(g);
    }
    @Test public void ginsengSpendsCopyOnDummyAndGetsNothing()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Bishop.class,BLACK,"f7");Game g=AresTest.charge(AresTest.charge(createClown(b,Map.of(pos("f7"),HeroType.GINSENG_BISHOP)),"c2",1),"f7",2);cast(g,"c2","d2",Ability.ILLUSION);move(g,"b1","a3");end(g);cast(g,"f7","d2",Ability.COPY_SPELL);assertEquals(0,g.getPieceState(pos("f7")).ultimateCharge());assertTrue(g.getTurnState().abilityUsed());assertTrue(g.getSpells().borrowed().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void ginsengCopiesAndCastsBoxWithNoExtraCooldown()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"e2");Game g=AresTest.charge(game.heroes.ginseng.GinsengFixtures.create(b,Map.of(pos("e2"),HeroType.CLOWNGANGER_KNIGHT)),"c2",2);cast(g,"e2","b1",Ability.BOX);move(g,"c2","d3");end(g);move(g,"g8","f6");end(g);cast(g,"d3","e2",Ability.COPY_SPELL);move(g,"d3","c2");end(g);move(g,"f6","g8");end(g);
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.BOX,pos("a1"),null,true)));assertTrue(g.getPieceState(pos("a1")).isBoxed());assertEquals(0,g.getPieceState(pos("c2")).cooldown());AresTest.roundtrip(g);
    }
    @Test public void ninthDraftHeroUsesEitherKnightHomeAndSandboxHasIt()throws Exception{
        for(String square:List.of("b1","g1")){var d=new Draft();assertEquals(10,d.getSnapshot().rules().catalogVersion());assertEquals(13,HeroCatalog.heroes().size());d.ban(WHITE,HeroType.PUDGE_ROOK);d.ban(BLACK,HeroType.IZBA_ROOK);assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.CLOWNGANGER_KNIGHT,pos(square)));d.finishPicks(BLACK);d.finishPicks(WHITE);Game g=Game.mobaFromDraft(d.getSnapshot(),HeroRules.prototype(),600,()->0L);assertEquals(HeroType.CLOWNGANGER_KNIGHT,g.getPieceState(pos(square)).hero());AresTest.roundtrip(g);}
        assertEquals(HeroType.CLOWNGANGER_KNIGHT,Game.mobaHeroesSandbox(600).getPieceState(pos("c2")).hero());
    }

    @Test public void copiedIllusionBelongsToGinsengAndMovesAsItsBishop()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"e2");Game g=AresTest.charge(AresTest.charge(game.heroes.ginseng.GinsengFixtures.create(b,Map.of(pos("e2"),HeroType.CLOWNGANGER_KNIGHT)),"e2",1),"c2",2);
        cast(g,"e2","f2",Ability.ILLUSION);move(g,"b1","a3");end(g);move(g,"g8","f6");end(g);
        var original=g.getSnapshot().pieces().values().stream().filter(p->p.hero()==HeroType.CLOWNGANGER_KNIGHT).findFirst().orElseThrow();
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),pos("c2"),original.position(),Ability.COPY_SPELL));move(g,"a3","b1");end(g);move(g,"f6","g8");end(g);
        assertEquals(Success.SUCCESS,g.useAbility(g.getCurrentPlayer(),new AbilityRequest(pos("c2"),Ability.ILLUSION,pos("d2"),null,true)));
        long source=g.getSnapshot().pieces().values().stream().filter(p->p.hero()==HeroType.GINSENG_BISHOP).findFirst().orElseThrow().id();
        var copy=g.getSnapshot().clown().illusions().values().stream().filter(i->i.sourceId()==source).findFirst().orElseThrow();assertEquals(Bishop.class,g.getPieceState(copy.id()).type());assertTrue(g.getSpells().borrowed().isEmpty());AresTest.roundtrip(g);
    }
    @Test public void hiddenShuffleHasBothOutcomesAndPreviewsDoNotConsumeRandomness()throws Exception{
        var outcomes=new HashSet<Boolean>();
        for(long seed=0;seed<12;seed++){
            Game g=AresTest.charge(basic(),"c2",1);var s=g.getSnapshot();g=Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),s.pieces(),s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),game.summoning.SummonState.initial(seed)),()->0L);
            var before=g.getSnapshot();g.getAbilityTargets(pos("c2"),Ability.ILLUSION);g.getAbilityTargets(pos("c2"),Ability.ILLUSION);assertEquals(before,g.getSnapshot());cast(g,"c2","d2",Ability.ILLUSION);outcomes.add(g.getPieceState(pos("c2")).hero()==HeroType.ILLUSION);AresTest.roundtrip(g);
        }
        assertEquals(2,outcomes.size());
    }
    @Test public void dummyInEnemyMudSurvivesAndExtendsWithoutChargingBolotnik()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Knight.class,BLACK,"e6");Game g=AresTest.charge(createClown(b,Map.of(pos("e6"),HeroType.BOLOTNIK_KNIGHT)),"c2",1);cast(g,"c2","d2",Ability.ILLUSION);long dummy=g.getPieceState(pos("d2")).id();move(g,"b1","a3");end(g);cast(g,"e6","f3",Ability.MUD);move(g,"g8","f6");end(g);move(g,"d2","f3");
        assertEquals(dummy,g.getPieceState(pos("f3")).id());assertEquals(HeroType.BOLOTNIK_KNIGHT,g.getPieceState(pos("e6")).hero());assertEquals(0,g.getPieceState(pos("e6")).ultimateCharge());assertEquals(0,g.getEconomy().blackHalves());assertTrue(g.getSnapshot().clown().illusions().get(dummy).extended());AresTest.roundtrip(g);
    }
    @Test public void attemptedCaptureCannotBeUsedToEscapeActualCheck(){
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Rook.class,WHITE,"g1");put(b,Rook.class,BLACK,"d8");Game g=AresTest.charge(createClown(b,Map.of()),"c2",1);cast(g,"c2","d2",Ability.ILLUSION);move(g,"g1","h1");end(g);assertTrue(g.isInCheck());var s=g.getSnapshot();assertEquals(MovementRejection.KING_IN_CHECK,g.moveFigure(g.getCurrentPlayer(),pos("d8"),pos("d2")));assertEquals(s,g.getSnapshot());
    }


    @Test public void firstBodyCanMoveWhenSecondBodyCanResolveCheckAtBoxExpiry()throws Exception{
        var b=board();put(b,Knight.class,WHITE,"c2");put(b,Knight.class,WHITE,"b3");put(b,Rook.class,BLACK,"a8");
        Game g=AresTest.charge(createClown(b,Map.of(pos("b3"),HeroType.CLOWNGANGER_KNIGHT)),"c2",1);
        cast(g,"b3","a1",Ability.BOX);move(g,"b1","c3");end(g);move(g,"g8","f6");end(g);cast(g,"c2","d2",Ability.ILLUSION);
        move(g,"d2","f3");AresTest.roundtrip(g);move(g,"c2","a3");end(g);assertFalse(g.isInCheck());AresTest.roundtrip(g);
    }
    @Test public void disappearingIllusionCannotExposeKingAtTheEndOfTheTurn()throws Exception{
        var b=board();put(b,Rook.class,BLACK,"a8");
        Game base=Game.fromMobaPosition(new GamePosition(b,WHITE,CastlingRights.none(),null,0),Map.of(pos("b1"),HeroType.CLOWNGANGER_KNIGHT),HeroRules.prototype(),()->0L);
        var s=base.getSnapshot();Game g=Game.fromSnapshot(new GameSnapshot(s.version(),s.mode(),s.field(),s.pieces(),s.turn(),s.active(),s.castlingRights(),s.enPassantTarget(),s.halfmoveClock(),s.repetitions(),s.clock(),s.outcome(),s.moves(),s.events(),s.whitePoints(),s.blackPoints(),s.whiteCaptures(),s.blackCaptures(),s.drawOffer(),s.heroRules(),s.economy(),s.terrain(),s.draft(),game.summoning.SummonState.initial(0)),()->0L);
        g=AresTest.charge(g,"b1",1);cast(g,"b1","a2",Ability.ILLUSION);long dummy=g.getPieceState(pos("a2")).id();move(g,"b1","c3");end(g);move(g,"g8","f6");end(g);move(g,"c3","b5");end(g);move(g,"f6","g8");end(g);
        cast(g,"b5","c2",Ability.BOX);var before=g.getSnapshot();assertEquals(TurnRejection.NO_TURN_CONTINUATION,g.moveFigure(g.getCurrentPlayer(),pos("b5"),pos("c3")));assertEquals(before,g.getSnapshot());move(g,"b5","a3");end(g);assertEquals(PieceState.Status.DISSOLVED,g.getPieceState(dummy).status());AresTest.roundtrip(g);
    }
}
