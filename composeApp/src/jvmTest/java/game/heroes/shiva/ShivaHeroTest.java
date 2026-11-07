package game.heroes.shiva;

import game.core.Game;
import game.heroes.chronomlin.ChronomlinAbility;
import game.summoning.SummonState;
import game.creation.CreationMechanic;
import game.draft.Draft;
import game.draft.DraftRules;
import game.draft.DraftActionResult;
import game.heroes.AbilityRequest;
import game.heroes.HeroRules;
import game.heroes.HeroTargeting;
import game.heroes.HeroType;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.GinsengFixtures;
import game.heroes.faun.FaunAbility;
import game.heroes.faun.FaunView;
import game.heroes.pudge.PudgeAbility;
import game.heroes.moleminer.MoleminerAbility;
import game.heroes.phoenix.PhoenixAbility;
import game.heroes.phoenix.PhoenixHeroTest;
import game.heroes.sunwukong.SunWukongAbility;
import game.heroes.sunwukong.SunWukongDefinition;
import game.heroes.sunwukong.SunWukongRules;
import game.heroes.leshy.LeshyAbility;
import game.heroes.clownganger.ClowngangerAbility;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.persistence.GameSaveFile;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.visibility.Concealment;
import game.visibility.VisibilityRules;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import model.Figure;
import model.Position;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import model.figures.Ward;
import org.junit.Test;

import static game.core.Positions.pos;
import static game.heroes.phoenix.PhoenixHeroTest.charge;
import static game.heroes.phoenix.PhoenixHeroTest.play;
import static game.heroes.phoenix.PhoenixHeroTest.restored;
import static game.heroes.phoenix.PhoenixHeroTest.ok;
import static model.FigureColor.WHITE;
import static model.FigureColor.BLACK;
import static org.junit.Assert.*;

public class ShivaHeroTest {
    public static Map<Position,Figure> board() {
        var board=PhoenixHeroTest.board();
        GinsengFixtures.put(board,Rook.class,WHITE,"c2");
        return board;
    }
    public static Game create(Map<Position,Figure> board,Map<Position,HeroType> other) {
        var heroes=new HashMap<>(other); heroes.put(pos("c2"),HeroType.SHIVA);
        return seeded(Game.fromMobaPosition(new GamePosition(board,WHITE,CastlingRights.none(),null,0),heroes,HeroRules.defaults(),()->0L),0);
    }
    private static Game seeded(Game game,int firstRole) {
        long seed=0;
        while(SummonState.initial(seed).choose(2).index()!=firstRole) seed++;
        var s=game.getSnapshot();
        return Game.fromSnapshot(s.withMechanics(s.mechanics().with(ShivaMechanic.KEY,new ShivaMechanic.State(seed,seed,Map.of()))),()->0L);
    }
    public static Game basic() { return create(board(),Map.of()); }
    public static void lotus(Game game,String actor,String target) { ok(game.useAbility(game.getCurrentPlayer(),pos(actor),pos(target),ShivaAbility.LOTUS)); }
    public static void revive(Game game,String actor) { ok(game.useAbility(game.getCurrentPlayer(),pos(actor),null,ShivaAbility.REBIRTH)); }
    public static Game revivalReady() {
        var b=board();
        GinsengFixtures.put(b,Pawn.class,BLACK,"c3");
        GinsengFixtures.put(b,Pawn.class,WHITE,"d4");
        GinsengFixtures.put(b,Rook.class,BLACK,"d5");
        Game game=create(b,Map.of(pos("d4"),HeroType.BOUNTY_CROW));
        play(game,"c2","c3"); play(game,"d5","d4");
        return charge(game,"c3",2);
    }
    private static List<PieceState> newLives(Game game) {
        return game.getSnapshot().mechanics().get(CreationMechanic.KEY).births().values().stream()
                .filter(b -> b.corpse()!=null).map(b -> game.getPieceState(b.id())).toList();
    }
    @Test public void lotusHasConfiguredRangeCooldownAndDoesNotConsumeMandatoryMove() throws Exception {
        Game game=basic(); lotus(game,"c2","e4");
        var p=game.getPieceState(pos("e4"));
        assertEquals(Ward.class,p.type()); assertEquals(HeroType.NONE,p.hero());
        assertEquals(3,game.getPieceState(pos("c2")).cooldown());
        assertFalse(game.getTurnState().moveUsed()); assertTrue(game.getTurnState().abilityUsed());
        assertFalse(game.endTurn(game.getCurrentPlayer()).isSuccess());
        assertTrue(game.getLegalMoves(pos("e4")).getCanMove().isEmpty());
        restored(game);
    }
    @Test public void rejectsOccupiedAndOutOfRangeSquaresWithoutMutation() throws Exception {
        Game game=basic(); byte[] before=GameSaveFile.encode(game.getSnapshot());
        assertFalse(game.useAbility(game.getCurrentPlayer(),pos("c2"),pos("h6"),ShivaAbility.LOTUS).isSuccess());
        assertFalse(game.useAbility(game.getCurrentPlayer(),pos("c2"),pos("b1"),ShivaAbility.LOTUS).isSuccess());
        assertArrayEquals(before,GameSaveFile.encode(game.getSnapshot()));
    }
    @Test public void lotusExpiresAtEndOfSecondFollowingOwnTurn() throws Exception {
        Game game=basic(); lotus(game,"c2","c3"); long id=game.getPieceState(pos("c3")).id();
        play(game,"h2","h3"); play(game,"g8","f6");
        play(game,"h3","h4"); assertNotNull(game.getPieceState(pos("c3"))); play(game,"f6","g8");
        play(game,"h4","h5"); assertNull(game.getPieceState(pos("c3")));
        assertEquals(PieceState.Status.REPLACED,game.getPieceState(id).status());
        assertTrue(game.getSnapshot().mechanics().get(ShivaMechanic.KEY).lotuses().isEmpty());
        restored(game);
    }
    @Test public void lotusCanBeCapturedForOneGoldAndIsNotAResurrectionCandidate() throws Exception {
        var b=board(); GinsengFixtures.put(b,Knight.class,BLACK,"e5"); Game game=create(b,Map.of());
        lotus(game,"c2","d3"); long id=game.getPieceState(pos("d3")).id();
        play(game,"h2","h3"); play(game,"e5","d3");
        assertEquals(2,game.getEconomy().blackHalves());
        assertFalse(CreationMechanic.eligibleCorpse(game.getPieceState(id),game.getSnapshot().field()));
        restored(game);
    }
    @Test public void lotusCannotBeHooked() throws Exception {
        var b=board(); GinsengFixtures.put(b,Rook.class,WHITE,"d5");
        Game game=create(b,Map.of(pos("d5"),HeroType.PUDGE)); lotus(game,"c2","d3");
        play(game,"h2","h3"); play(game,"g8","f6");
        assertFalse(game.useAbility(game.getCurrentPlayer(),pos("d5"),pos("d3"),PudgeAbility.HOOK).isSuccess()); restored(game);
    }
    @Test public void revealsInvisibleEnemyOnlyInsideTheAuraWithoutRemovingInvisibility() throws Exception {
        var b=board(); GinsengFixtures.put(b,Knight.class,BLACK,"f4");
        Game game=charge(create(b,Map.of(pos("f4"),HeroType.FAUN)),"f4",2);
        play(game,"h2","h3"); ok(game.useAbility(game.getCurrentPlayer(),pos("f4"),null,FaunAbility.INVISIBILITY));
        play(game,"g8","f6"); assertFalse(FaunView.visible(game.getSnapshot(),game.getPieceState(pos("f4"))));
        lotus(game,"c2","c3");
        var s=game.getSnapshot(); var faun=s.pieceAt(pos("f4"));
        assertTrue(FaunView.visible(s,faun)); assertTrue(s.faun().invisible().containsKey(faun.id()));
        assertFalse(VisibilityRules.reveals(VisibilityRules.context(s),BLACK,pos("f4"),Concealment.INVISIBILITY));
        assertTrue(HeroTargeting.square().validate(VisibilityRules.context(s),s.pieceAt(pos("c2")),
                new AbilityRequest(pos("c2"),PudgeAbility.HOOK,pos("f4"))).isSuccess());
        play(game,"h3","h4"); play(game,"f4","h5");
        assertFalse(FaunView.visible(game.getSnapshot(),game.getPieceState(pos("h5")))); restored(game);
    }
    @Test public void exposesEnemyMineToLotusOwner() throws Exception {
        var b=board(); GinsengFixtures.put(b,Pawn.class,BLACK,"d4");
        Game game=create(b,Map.of(pos("d4"),HeroType.MOLEMINER));
        play(game,"h2","h3"); ok(game.useAbility(game.getCurrentPlayer(),pos("d4"),null,MoleminerAbility.PLANT_MINE));
        play(game,"g8","f6"); assertFalse(VisibilityRules.mineVisible(game.getSnapshot(),pos("d4")));
        lotus(game,"c2","c3"); assertTrue(VisibilityRules.mineVisible(game.getSnapshot(),pos("d4")));
        assertFalse(FaunView.hasHiddenEnemy(game.getSnapshot())); restored(game);
    }
    @Test public void doesNotRevealClowngangerIllusion() throws Exception {
        var b=board(); GinsengFixtures.put(b,Knight.class,BLACK,"d4");
        Game game=charge(create(b,Map.of(pos("d4"),HeroType.CLOWNGANGER)),"d4",1);
        play(game,"h2","h3"); ok(game.useAbility(game.getCurrentPlayer(),pos("d4"),pos("e4"),ClowngangerAbility.ILLUSION));
        play(game,"g8","f6"); lotus(game,"c2","c3");
        assertFalse(VisibilityRules.reveals(VisibilityRules.context(game.getSnapshot()),WHITE,pos("e4"),Concealment.ILLUSION));
        assertTrue(FaunView.hasHiddenEnemy(game.getSnapshot())); restored(game);
    }
    @Test public void ginsengCreatesLotusFromItselfWithoutNativeCooldownOrAnotherCharge() throws Exception {
        var b=board(); GinsengFixtures.put(b,Bishop.class,WHITE,"d2");
        Game game=charge(create(b,Map.of(pos("d2"),HeroType.GINSENG)),"d2",2);
        lotus(game,"c2","c3"); play(game,"h2","h3"); play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(),pos("d2"),pos("c2"),GinsengAbility.COPY_SPELL));
        play(game,"h3","h4"); play(game,"f6","g8");
        ok(game.useAbility(game.getCurrentPlayer(),new AbilityRequest(pos("d2"),ShivaAbility.LOTUS,pos("e3"),null,true)));
        var s=game.getSnapshot();
        assertEquals(s.pieceAt(pos("d2")).id(),s.mechanics().get(ShivaMechanic.KEY).lotuses().get(s.pieceAt(pos("e3")).id()).source());
        assertEquals(0,s.pieceAt(pos("d2")).cooldown()); assertEquals(HeroType.GINSENG,s.pieceAt(pos("d2")).hero()); restored(game);
    }
    @Test public void resurrectsOneHeroAndOppositeOrdinaryPieceWithNewIdsAndKeepsGoldAndCaptures() throws Exception {
        Game game=revivalReady(); var before=game.getSnapshot();
        revive(game,"c3"); var lives=newLives(game); assertEquals(2,lives.size());
        for(var p:lives) {
            var birth=game.getSnapshot().mechanics().get(CreationMechanic.KEY).births().get(p.id());
            var corpse=game.getPieceState(birth.corpse());
            assertTrue(p.id()>corpse.id()); assertTrue(corpse.isRemoved()); assertEquals(corpse.hero(),p.hero());
            assertEquals(p.color()==WHITE,p.position().getRow()<4);
            assertEquals(0,p.cooldown()); assertEquals(0,p.ultimateCharge()); assertFalse(p.isBlinded()); assertFalse(p.isStunned());
        }
        assertEquals(before.whiteCaptures(),game.getSnapshot().whiteCaptures()); assertEquals(before.blackCaptures(),game.getSnapshot().blackCaptures());
        assertEquals(before.economy().whiteHalves(),game.getEconomy().whiteHalves());
        assertEquals(before.economy().blackHalves(),game.getEconomy().blackHalves());
        assertEquals(0,game.getPieceState(pos("c3")).ultimateCharge()); restored(game);
    }
    @Test public void emptyResurrectionIsRejectedWithoutSpendingChargeOrRandomness() throws Exception {
        Game game=charge(basic(),"c2",2); byte[] before=GameSaveFile.encode(game.getSnapshot());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,game.useAbility(game.getCurrentPlayer(),pos("c2"),null,ShivaAbility.REBIRTH));
        assertArrayEquals(before,GameSaveFile.encode(game.getSnapshot()));
    }
    @Test public void missingOppositeCorpseSkipsOnlyThatSide() throws Exception {
        var b=board(); GinsengFixtures.put(b,Pawn.class,WHITE,"d4"); GinsengFixtures.put(b,Rook.class,BLACK,"d5");
        Game game=create(b,Map.of(pos("d4"),HeroType.BOUNTY_CROW));
        play(game,"h2","h3"); play(game,"d5","d4"); game=charge(game,"c2",2); revive(game,"c2");
        assertEquals(1,newLives(game).size()); assertEquals(WHITE,newLives(game).getFirst().color()); restored(game);
    }
    @Test public void consumedCorpseCannotBeResurrectedTwice() throws Exception {
        Game game=revivalReady(); revive(game,"c3");
        play(game,"h2","h3"); play(game,"g8","f6"); game=charge(game,"c3",2);
        assertFalse(game.useAbility(game.getCurrentPlayer(),pos("c3"),null,ShivaAbility.REBIRTH).isSuccess()); restored(game);
    }
    @Test public void previewAndReloadKeepTheSameRandomChoices() throws Exception {
        Game game=revivalReady(); byte[] before=GameSaveFile.encode(game.getSnapshot());
        for(int i=0;i<4;i++) game.getAbilityTargets(pos("c3"),ShivaAbility.REBIRTH);
        assertArrayEquals(before,GameSaveFile.encode(game.getSnapshot()));
        Game copy=restored(game); revive(game,"c3"); revive(copy,"c3");
        assertArrayEquals(GameSaveFile.encode(game.getSnapshot()),GameSaveFile.encode(copy.getSnapshot()));
    }
    @Test public void deadPhoenixEggReturnsAsPhoenixKnight() throws Exception {
        var b=board(); GinsengFixtures.put(b,Knight.class,WHITE,"d4"); GinsengFixtures.put(b,Rook.class,BLACK,"d5");
        Game game=charge(create(b,Map.of(pos("d4"),HeroType.PHOENIX)),"d4",2);
        ok(game.useAbility(game.getCurrentPlayer(),pos("d4"),null,PhoenixAbility.REBIRTH));
        play(game,"h2","h3"); play(game,"d5","d4"); game=charge(game,"c2",2); revive(game,"c2");
        var live=newLives(game).getFirst(); assertEquals(HeroType.PHOENIX,live.hero()); assertEquals(Knight.class,live.type()); restored(game);
    }
    @Test public void resurrectionRetainsTheExistingSharedWukongCooldown() throws Exception {
        var b=board(); GinsengFixtures.put(b,Pawn.class,WHITE,"d4"); GinsengFixtures.put(b,Pawn.class,WHITE,"a2"); GinsengFixtures.put(b,Rook.class,BLACK,"d5");
        Game game=create(b,Map.of(pos("d4"),HeroType.SUN_WUKONG,pos("a2"),HeroType.SUN_WUKONG));
        ok(game.useAbility(game.getCurrentPlayer(),pos("a2"),null,SunWukongAbility.SHAPESHIFT));
        play(game,"h2","h3"); play(game,"d5","d4"); game=charge(game,"c2",2);
        int cooldown=game.getPieceState(pos("a2")).cooldown(); assertTrue(cooldown>0);
        revive(game,"c2"); assertEquals(cooldown,newLives(game).getFirst().cooldown()); restored(game);
    }
    @Test public void revivedLumeshardKeepsItsOriginalSquareColor() throws Exception {
        var b=board(); GinsengFixtures.put(b,Bishop.class,WHITE,"d3"); GinsengFixtures.put(b,Rook.class,BLACK,"d4");
        Game game=create(b,Map.of(pos("d3"),HeroType.LUMESHARD));
        var color=game.getPieceState(pos("d3")).startingColor();
        play(game,"h2","h3"); play(game,"d4","d3"); game=charge(game,"c2",2); revive(game,"c2");
        assertEquals(color,newLives(game).getFirst().startingColor()); restored(game);
    }
    @Test public void shivaIsPickableOnBothRookHomesAndCannotBePickedTwice() throws Exception {
        var d=new Draft(new DraftRules(0,2));
        assertEquals(List.of(pos("a1"),pos("h1")),d.getPlacementTargets(WHITE,HeroType.SHIVA));
        assertEquals(DraftActionResult.SUCCESS,d.pick(WHITE,HeroType.SHIVA,pos("a1")));
        assertNotEquals(DraftActionResult.SUCCESS,d.pick(BLACK,HeroType.SHIVA,pos("a8")));
        d.finishPicks(BLACK); d.finishPicks(WHITE);
        Game game=Game.mobaFromDraft(d.getSnapshot(),HeroRules.defaults(),600,()->0L); restored(game);
    }
    @Test public void newSaveFormatAndCustomRulesRoundTrip() throws Exception {
        var rules=HeroRules.defaults().with(ShivaDefinition.INSTANCE,new ShivaRules(1,2,1,4,1));
        Game game=Game.fromMobaPosition(new GamePosition(board(),WHITE,CastlingRights.none(),null,0),Map.of(pos("c2"),HeroType.SHIVA),rules,()->0L);
        lotus(game,"c2","c3"); assertEquals(4,game.getPieceState(pos("c2")).cooldown());
        assertTrue(new String(GameSaveFile.encode(game.getSnapshot()),StandardCharsets.UTF_8).contains("formatVersion=\"22\""));
        assertEquals(rules,restored(game).getSnapshot().heroRules());
    }

    @Test public void aMissingRandomRoleDoesNotChangeTheChosenSides() throws Exception {
        var b=board(); GinsengFixtures.put(b,Pawn.class,WHITE,"d4"); GinsengFixtures.put(b,Rook.class,BLACK,"d5");
        Game game=seeded(create(b,Map.of(pos("d4"),HeroType.BOUNTY_CROW)),1);
        play(game,"h2","h3"); play(game,"d5","d4"); game=charge(game,"c2",2); revive(game,"c2");
        assertTrue(newLives(game).isEmpty()); assertEquals(0,game.getPieceState(pos("c2")).ultimateCharge()); restored(game);
    }
    @Test public void everyPickableHeroCanReceiveANewLifeThroughItsOwnPolicy() throws Exception {
        for(var type : game.draft.HeroCatalog.heroes()) {
            if(type==HeroType.SHIVA) continue;
            var body=game.heroes.HeroDefinitions.get(type).supportedBodies().iterator().next();
            var b=board(); GinsengFixtures.put(b,body,WHITE,"d3"); GinsengFixtures.put(b,Rook.class,BLACK,"d4");
            Game current=create(b,Map.of(pos("d3"),type));
            play(current,"h2","h3"); play(current,"d4","d3"); current=charge(current,"c2",2);
            revive(current,"c2");
            var live=newLives(current).stream().filter(p -> p.hero()==type).findFirst().orElseThrow();
            assertEquals(type.name(),body,live.type()); restored(current);
        }
    }
    @Test public void treeDefersLotusExpiryAndDisablesDetectionUntilProtectionEnds() throws Exception {
        var b=board(); GinsengFixtures.put(b,Pawn.class,WHITE,"c4");
        Game game=charge(create(b,Map.of(pos("c4"),HeroType.LESHY)),"c4",1);
        lotus(game,"c2","d4"); long id=game.getPieceState(pos("d4")).id();
        play(game,"h2","h3"); play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(),pos("c4"),null,LeshyAbility.FOREST));
        assertFalse(VisibilityRules.reveals(VisibilityRules.context(game.getSnapshot()),WHITE,pos("d4"),Concealment.MINE));
        play(game,"h3","h4"); play(game,"f6","g8"); play(game,"h4","h5");
        assertNotNull(game.getPieceState(pos("d4"))); restored(game);
        play(game,"g8","f6"); play(game,"h5","h6");
        assertTrue(game.getPieceState(id).isRemoved()); restored(game);
    }
    @Test public void lotusLandingOnAnEnemyMineDiesAndCannotRevealIt() throws Exception {
        var b=board(); GinsengFixtures.put(b,Pawn.class,BLACK,"d4");
        Game game=create(b,Map.of(pos("d4"),HeroType.MOLEMINER));
        play(game,"h2","h3"); ok(game.useAbility(game.getCurrentPlayer(),pos("d4"),null,MoleminerAbility.PLANT_MINE));
        play(game,"d4","d3"); lotus(game,"c2","d4");
        assertNull(game.getPieceState(pos("d4"))); assertTrue(game.getSnapshot().mines().cells().isEmpty()); restored(game);
    }
    @Test public void deadSupernovaReturnsWithItsCombinedMovementAndThreeGoldValue() throws Exception {
        var b=board(); GinsengFixtures.put(b,Rook.class,BLACK,"b4");
        Game game=create(b,Map.of(pos("b1"),HeroType.PHOENIX));
        game=PhoenixHeroTest.hatch(game);
        play(game,"h5","h6"); play(game,"b4","b1");
        game=charge(game,"c2",2); ok(game.moveFigure(game.getCurrentPlayer(),pos("h1"),pos("g2"))); revive(game,"c2");
        var live=newLives(game).getFirst(); assertEquals(HeroType.SUPERNOVA,live.hero());
        assertEquals(model.figures.Archbishop.class,live.type()); assertEquals(6,game.getEconomy().blackHalves()); restored(game);
    }
    @Test public void revivedBabaYagaGetsANewLifetimeThenBecomesIzba() throws Exception {
        var b=board(); GinsengFixtures.put(b,Rook.class,WHITE,"d3"); GinsengFixtures.put(b,Rook.class,BLACK,"d4");
        Game game=create(b,Map.of(pos("d3"),HeroType.IZBA));
        play(game,"h2","h3"); play(game,"d4","d3");
        var baby=game.getSnapshot().pieces().values().stream().filter(p -> p.hero()==HeroType.BABA_YAGA).findFirst().orElseThrow();
        var square=baby.position();
        play(game,"h3","h4");
        if(square.getRow()==2 || square.getColumn()==3) play(game,"d3",cell(square));
        else {
            var step=new Position(2,square.getColumn());
            play(game,"d3",cell(step)); play(game,"h4","h5"); play(game,cell(step),cell(square));
        }
        var s=game.getSnapshot();
        long seed=0;
        while(true) {
            var role=SummonState.initial(seed).choose(2);
            if(role.index()==0 && SummonState.initial(role.nextState()).choose(2).index()==1) break;
            seed++;
        }
        game=Game.fromSnapshot(s.withMechanics(s.mechanics().with(ShivaMechanic.KEY,new ShivaMechanic.State(seed,seed,Map.of()))),()->0L);
        game=charge(game,"c2",2); revive(game,"c2");
        var live=newLives(game).getFirst(); assertEquals(HeroType.BABA_YAGA,live.hero()); long id=live.id();
        restored(game);
        for(int i=0;i<4;i++) {
            boolean moved=false;
            for(var piece : game.getSnapshot().pieces().values()) {
                if(piece.color()!=WHITE || piece.position()==null || piece.id()==id) continue;
                var targets=game.getLegalMoves(piece.position()).getCanMove();
                if(!targets.isEmpty()) { play(game,cell(piece.position()),cell(targets.getFirst())); moved=true; break; }
            }
            assertTrue(moved);
            if(i<3) { var knight=game.getPieceState(pos("g8"))!=null ? "g8":"f6"; play(game,knight,knight.equals("g8")?"f6":"g8"); }
        }
        assertEquals(HeroType.IZBA,game.getPieceState(id).hero()); assertEquals(0,game.getPieceState(id).ultimateCharge()); restored(game);
    }
    @Test public void ginsengCanCopyAndCastResurrectionFromItsOwnPosition() throws Exception {
        var b=board(); GinsengFixtures.put(b,Bishop.class,WHITE,"d2");
        GinsengFixtures.put(b,Pawn.class,BLACK,"c3"); GinsengFixtures.put(b,Pawn.class,BLACK,"e3");
        GinsengFixtures.put(b,Pawn.class,WHITE,"d4"); GinsengFixtures.put(b,Rook.class,BLACK,"d5");
        Game game=charge(create(b,Map.of(pos("d2"),HeroType.GINSENG,pos("d4"),HeroType.BOUNTY_CROW)),"d2",2);
        var s=game.getSnapshot(); long seed=0;
        while(SummonState.initial(seed).choose(2).index()!=0 || SummonState.initial(seed+5*0x9e3779b97f4a7c15L).choose(2).index()!=0) seed++;
        game=Game.fromSnapshot(s.withMechanics(s.mechanics().with(ShivaMechanic.KEY,new ShivaMechanic.State(seed,seed,Map.of()))),()->0L);
        play(game,"c2","c3"); play(game,"d5","d4"); play(game,"c3","e3"); play(game,"d4","d5");
        game=charge(game,"e3",2); revive(game,"e3");
        play(game,"h2","h3"); play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(),pos("d2"),pos("e3"),GinsengAbility.COPY_SPELL));
        play(game,"h3","h4"); play(game,"f6","g8");
        ok(game.useAbility(game.getCurrentPlayer(),new AbilityRequest(pos("d2"),ShivaAbility.REBIRTH,null,null,true)));
        assertEquals(3,newLives(game).size()); assertEquals(HeroType.GINSENG,game.getPieceState(pos("d2")).hero()); restored(game);
    }
    private static String cell(Position p) { return ""+(char)('a'+p.getColumn())+(p.getRow()+1); }

    @Test public void newLivesDoNotAddDraftPicksOrOriginalMaterial() throws Exception {
        var draft=new Draft(new DraftRules(0,2));
        draft.pick(WHITE,HeroType.SHIVA,pos("a1")); draft.finishPicks(BLACK);
        draft.pick(WHITE,HeroType.BOUNTY_CROW,pos("e2")); draft.finishPicks(WHITE);
        Game game=seeded(Game.mobaFromDraft(draft.getSnapshot(),HeroRules.defaults(),600,()->0L),0);
        play(game,"e2","e4"); play(game,"d7","d5"); play(game,"e4","d5"); play(game,"d8","d5");
        game=charge(game,"a1",2); revive(game,"a1");
        assertEquals(34,game.getSnapshot().pieces().size()); assertEquals(draft.getSnapshot(),restored(game).getDraft());
    }
    @Test public void ownExplosionDeathIsEligibleAndDoesNotRestoreSpentResources() throws Exception {
        var b=board(); GinsengFixtures.put(b,Pawn.class,WHITE,"a3");
        Game game=charge(create(b,Map.of(pos("a3"),HeroType.MOLEMINER)),"a3",2);
        ok(game.useAbility(game.getCurrentPlayer(),pos("a3"),pos("c4"),MoleminerAbility.DETONATE));
        play(game,"h2","h3"); play(game,"g8","f6"); game=charge(game,"c2",2); revive(game,"c2");
        var live=newLives(game).getFirst(); assertEquals(HeroType.MOLEMINER,live.hero()); assertEquals(0,live.ultimateCharge()); restored(game);
    }
    @Test public void forgedLotusBodyCannotBecomeAMovableRook() throws Exception {
        Game game=basic(); lotus(game,"c2","c3");
        String xml=new String(GameSaveFile.encode(game.getSnapshot()),StandardCharsets.UTF_8);
        String forged=xml.replace("type=\"Ward\"","type=\"Rook\""); assertNotEquals(xml,forged);
        assertThrows(java.io.IOException.class,()->GameSaveFile.decode(forged.getBytes(StandardCharsets.UTF_8)));
    }
    @Test public void legacyVersionCannotLoadTheNewBodyAndRules() throws Exception {
        Game game=basic(); lotus(game,"c2","c3");
        String xml=new String(GameSaveFile.encode(game.getSnapshot()),StandardCharsets.UTF_8);
        String forged=xml.replace("formatVersion=\"22\"","formatVersion=\"21\"");
        assertThrows(java.io.IOException.class,()->GameSaveFile.decode(forged.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void improvedChronomlinKeepsItsBodyInTheNewLife() throws Exception {
        var b=board(); GinsengFixtures.put(b,Pawn.class,WHITE,"d3"); GinsengFixtures.put(b,Rook.class,BLACK,"d4");
        Game game=charge(create(b,Map.of(pos("d3"),HeroType.CHRONOMLIN)),"d3",1);
        ok(game.useAbility(game.getCurrentPlayer(),pos("d3"),null,ChronomlinAbility.EVOLVE));
        play(game,"h2","h3"); play(game,"d4","d3"); game=charge(game,"c2",2); revive(game,"c2");
        var live=newLives(game).getFirst(); assertEquals(HeroType.CHRONOMLIN,live.hero()); assertEquals(Bishop.class,live.type()); restored(game);
    }
    @Test public void ginsengEggReturnsAsGinsengAndANewEggCanStillHatchIntoSupernova() throws Exception {
        var b=board(); GinsengFixtures.put(b,Bishop.class,WHITE,"d3"); GinsengFixtures.put(b,Rook.class,BLACK,"d4");
        GinsengFixtures.put(b,Pawn.class,BLACK,"g7");
        Game game=charge(charge(create(b,Map.of(pos("d3"),HeroType.GINSENG,pos("b1"),HeroType.PHOENIX)),"b1",2),"d3",2);
        ok(game.useAbility(game.getCurrentPlayer(),pos("b1"),null,PhoenixAbility.REBIRTH));
        play(game,"h2","h3"); play(game,"g8","f6");
        ok(game.useAbility(game.getCurrentPlayer(),pos("d3"),pos("b1"),GinsengAbility.COPY_SPELL));
        play(game,"h3","h4"); play(game,"f6","g8");
        ok(game.useAbility(game.getCurrentPlayer(),new AbilityRequest(pos("d3"),PhoenixAbility.REBIRTH,null,null,true)));
        play(game,"h4","h5"); play(game,"d4","d3");
        game=charge(game,"c2",2); revive(game,"c2");
        var live=newLives(game).getFirst(); long id=live.id(); String square=cell(live.position());
        assertEquals(HeroType.GINSENG,live.hero()); assertEquals(Bishop.class,live.type()); restored(game);
        play(game,"h5","h6"); play(game,"g8","f6");
        game=charge(game,square,2);
        ok(game.useAbility(game.getCurrentPlayer(),pos(square),pos("b1"),GinsengAbility.COPY_SPELL));
        play(game,"h6","h7"); play(game,"f6","g8");
        ok(game.useAbility(game.getCurrentPlayer(),new AbilityRequest(pos(square),PhoenixAbility.REBIRTH,null,null,true)));
        play(game,"b1","a3"); play(game,"g8","f6"); restored(game);
        play(game,"a3","b1"); play(game,"f6","g8"); play(game,"b1","a3");
        assertEquals(HeroType.SUPERNOVA,game.getPieceState(id).hero()); restored(game);
    }
}
