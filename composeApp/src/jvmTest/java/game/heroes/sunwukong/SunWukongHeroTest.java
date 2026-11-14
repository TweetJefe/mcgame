package game.heroes.sunwukong;

import game.core.Game;
import game.draft.Draft;
import game.draft.DraftActionResult;
import game.draft.DraftRules;
import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroRegistry;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.PieceCreationContext;
import game.heroes.HeroPieceFactory;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.GinsengFixtures;
import game.heroes.pudge.PudgeAbility;
import game.heroes.pudge.PudgeDefinition;
import game.heroes.pudge.PudgeRules;
import game.history.GameEvent;
import game.moves.AbilityRejection;
import game.moves.MoveResult;
import game.moves.Promotion;
import game.heroes.lumeshard.LumeshardAbility;
import game.heroes.chronomlin.ChronomlinAbility;
import game.heroes.clownganger.ClowngangerAbility;
import game.mechanics.PieceBodyHistory;
import game.moves.Success;
import game.moves.TurnRejection;
import game.persistence.GameSaveFile;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.summoning.SummonState;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Figure;
import model.FigureColor;
import model.Field;
import model.Position;
import model.figures.Bishop;
import model.figures.King;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static model.FigureColor.BLACK;
import static model.FigureColor.WHITE;
import static org.junit.Assert.*;

public class SunWukongHeroTest {
    private static final HeroRules RULES = HeroRules.defaults();

    public static Game basic() { return create(board(), Map.of(pos("c2"), HeroType.SUN_WUKONG), RULES); }
    private static Map<Position, Figure> board() {
        var board = GinsengFixtures.board();
        GinsengFixtures.put(board, Pawn.class, WHITE, "c2");
        GinsengFixtures.put(board, Pawn.class, WHITE, "f2");
        GinsengFixtures.put(board, Pawn.class, WHITE, "g2");
        return board;
    }
    private static Game create(Map<Position, Figure> board, Map<Position, HeroType> assignments, HeroRules rules) {
        return Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0), assignments, rules, () -> 0L);
    }
    public static Game ready() { return charge(basic(), "c2", 1); }
    private static Game charge(Game game, String square, int charge) {
        var snapshot = game.getSnapshot();
        var pieces = new HashMap<>(snapshot.pieces());
        var piece = snapshot.pieceAt(pos(square));
        pieces.put(piece.id(), piece.withAbilityState(piece.cooldown(), charge, piece.storedPieceId()));
        return Game.fromSnapshot(withPieces(snapshot, pieces), () -> 0L);
    }
    private static GameSnapshot withPieces(GameSnapshot s, Map<Long, PieceState> pieces) {
        return new GameSnapshot(s.version(), s.mode(), s.field(), pieces, s.turn(), s.active(), s.castlingRights(),
                s.enPassantTarget(), s.halfmoveClock(), s.repetitions(), s.clock(), s.outcome(), s.moves(), s.events(),
                s.whitePoints(), s.blackPoints(), s.whiteCaptures(), s.blackCaptures(), s.drawOffer(), s.heroRules(), s.draft(), s.mechanics());
    }
    private static Game seed(Game game, long seed) {
        var snapshot = game.getSnapshot();
        return Game.fromSnapshot(snapshot.withMechanics(snapshot.mechanics().with(SunWukongMechanic.KEY,
                new WukongState(seed, seed, Map.of(), Map.of()))), () -> 0L);
    }
    public static Game restored(Game game) throws Exception {
        byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        Game loaded = Game.fromSnapshot(GameSaveFile.decode(bytes), () -> 0L);
        assertArrayEquals(bytes, GameSaveFile.encode(loaded.getSnapshot()));
        return loaded;
    }
    private static void ok(MoveResult result) { assertEquals(Success.SUCCESS, result); }
    private static void play(Game game, String from, String to) {
        ok(game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to)));
        ok(game.endTurn(game.getCurrentPlayer()));
    }
    private static void nextWhite(Game game) { play(game, "b1", "a3"); play(game, "g8", "f6"); }
    private static void clone(Game game, String source, String target) {
        ok(game.useAbility(game.getCurrentPlayer(), pos(source), pos(target), SunWukongAbility.CLONE));
    }
    private static void shift(Game game, String source) {
        ok(game.useAbility(game.getCurrentPlayer(), pos(source), null, SunWukongAbility.SHAPESHIFT));
    }

    @Test public void registrationHasPawnOriginsAndDoesNotExpandHistoricalCatalogs() {
        var registry = HeroRegistry.fromRules(RULES);
        assertTrue(registry.get(HeroType.SUN_WUKONG) instanceof SunWukongHero);
        assertTrue(registry.get(HeroType.SUN_WUKONG).supports(Pawn.class));
        assertFalse(registry.get(HeroType.SUN_WUKONG).supports(Rook.class));
        var draft = new Draft();
        assertEquals(18, draft.getSnapshot().catalog().heroes().size());
        assertEquals(8, draft.getSnapshot().catalog().startingSquares(HeroType.SUN_WUKONG, WHITE).size());
        assertFalse(game.draft.HeroCatalog.heroes(10).contains(HeroType.SUN_WUKONG));
        assertEquals(4, new SunWukongRules(4, 1, 1).shapeshiftCooldown());
        assertThrows(IllegalArgumentException.class, () -> new SunWukongRules(-1, 1, 1));
    }

    @Test public void everyRandomBodyAppearsAndPreviewsNeverConsumeTheChoice() throws Exception {
        Set<Class<? extends Figure>> bodies = new HashSet<>();
        for (long seed = 0; seed < 12; seed++) {
            Game game = seed(ready(), seed);
            byte[] before = GameSaveFile.encode(game.getSnapshot());
            long id = game.getPieceState(pos("c2")).id();
            for (int i = 0; i < 3; i++) {
                game.getAbilityTargets(pos("c2"), SunWukongAbility.SHAPESHIFT);
                assertTrue(game.getAbilityAvailability(pos("c2"), SunWukongAbility.SHAPESHIFT).isSuccess());
            }
            assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
            Game loaded = restored(game);
            shift(game, "c2"); shift(loaded, "c2");
            var actor = game.getPieceState(pos("c2"));
            assertEquals(id, actor.id());
            assertEquals(HeroType.SUN_WUKONG, actor.hero());
            assertEquals(4, actor.cooldown());
            assertEquals(1, actor.ultimateCharge());
            assertEquals(actor, loaded.getPieceState(pos("c2")));
            assertTrue(game.getTurnState().abilityUsed());
            assertFalse(game.getTurnState().moveUsed());
            assertEquals(TurnRejection.MOVE_REQUIRED, game.endTurn(game.getCurrentPlayer()));
            assertTrue(game.getEvents().stream().anyMatch(e -> e.kind() == GameEvent.Kind.ABILITY_USED
                    && e.ability() == SunWukongAbility.SHAPESHIFT));
            bodies.add(actor.type()); restored(game);
        }
        assertEquals(Set.of(Knight.class, Bishop.class, Rook.class), bodies);
    }

    @Test public void shapeLastsThroughNextOwnTurnAndCooldownBecomesReadyOnFourth() throws Exception {
        Game game = seed(basic(), 0);
        shift(game, "c2");
        var body = game.getPieceState(pos("c2")).type();
        nextWhite(game);
        assertEquals(body, game.getPieceState(pos("c2")).type());
        assertEquals(3, game.getPieceState(pos("c2")).cooldown());
        game = restored(game);
        play(game, "a3", "b1");
        assertEquals(Pawn.class, game.getPieceState(pos("c2")).type());
        assertTrue(game.getEvents().stream().anyMatch(e -> e.isEffect(SunWukongMechanic.REVERT)));
        play(game, "f6", "g8");
        assertEquals(2, game.getPieceState(pos("c2")).cooldown());
        assertEquals(AbilityRejection.ABILITY_COOLDOWN, game.getAbilityAvailability(pos("c2"), SunWukongAbility.SHAPESHIFT));
        nextWhite(game);
        assertEquals(1, game.getPieceState(pos("c2")).cooldown());
        play(game, "a3", "b1"); play(game, "f6", "g8");
        assertEquals(0, game.getPieceState(pos("c2")).cooldown());
        shift(game, "c2"); restored(game);
    }

    @Test public void cloneKeepsPawnIdentityAndEffectsAndHasNoSeparateDraftPick() throws Exception {
        Game game = ready();
        var target = game.getPieceState(pos("f2"));
        clone(game, "c2", "f2");
        var clone = game.getPieceState(pos("f2"));
        assertEquals(target.id(), clone.id());
        assertEquals(HeroType.SUN_WUKONG, clone.hero());
        assertEquals(Pawn.class, clone.type());
        assertEquals(0, clone.ultimateCharge());
        assertEquals(target.inventory(), clone.inventory());
        assertEquals(0, game.getPieceState(pos("c2")).ultimateCharge());
        assertEquals(0, game.getEconomy().whiteHalves());
        assertEquals(1, game.getSnapshot().mechanics().get(SunWukongMechanic.KEY).clones().size());
        assertEquals(HeroType.NONE, game.getHeroRegistry().mechanics().initialHero(game.getSnapshot(), clone));
        restored(game);
    }

    @Test public void clonesJoinExistingCooldownAndEitherOneStartsTheSharedCooldown() throws Exception {
        Game game = ready();
        shift(game, "c2"); nextWhite(game);
        clone(game, "c2", "f2");
        assertEquals(3, game.getPieceState(pos("f2")).cooldown());
        game = restored(game);
        play(game, "a3", "b1"); play(game, "f6", "g8");
        nextWhite(game); play(game, "a3", "b1"); play(game, "f6", "g8");
        assertEquals(0, game.getPieceState(pos("f2")).cooldown());
        shift(game, "f2");
        assertEquals(4, game.getPieceState(pos("c2")).cooldown());
        assertEquals(4, game.getPieceState(pos("f2")).cooldown());
        assertEquals(Pawn.class, game.getPieceState(pos("c2")).type());
        restored(game);
    }

    @Test public void aCloneCanEarnItsOwnChargeAndCreateAnotherFullClone() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Pawn.class, BLACK, "e3");
        Game game = charge(create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG), RULES), "c2", 1);
        clone(game, "c2", "f2");
        play(game, "f2", "e3"); play(game, "g8", "f6");
        assertEquals(1, game.getPieceState(pos("e3")).ultimateCharge());
        assertEquals(0, game.getPieceState(pos("c2")).ultimateCharge());
        clone(game, "e3", "g2");
        assertEquals(HeroType.SUN_WUKONG, game.getPieceState(pos("g2")).hero());
        assertEquals(2, game.getSnapshot().mechanics().get(SunWukongMechanic.KEY).clones().size());
        restored(game);
    }

    @Test public void clonesSurviveOriginalDeathAndContinueUsingBothAbilities() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Rook.class, BLACK, "c4");
        Game game = charge(create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG), RULES), "c2", 1);
        clone(game, "c2", "f2");
        play(game, "b1", "a3"); play(game, "c4", "c2");
        assertEquals(HeroType.SUN_WUKONG, game.getPieceState(pos("f2")).hero());
        shift(game, "f2");
        restored(game);
    }

    @Test public void ultimateTargetsOnlyOrdinaryAlliedPawnsAcrossTheWholeBoard() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Pawn.class, WHITE, "b7");
        Game game = charge(create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG, pos("g2"), HeroType.BOUNTY_CROW), RULES), "c2", 1);
        assertEquals(Set.of(pos("f2"), pos("b7")), Set.copyOf(game.getAbilityTargets(pos("c2"), SunWukongAbility.CLONE)));
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertFalse(game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("g2"), SunWukongAbility.CLONE).isSuccess());
        assertFalse(game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("g8"), SunWukongAbility.CLONE).isSuccess());
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
        clone(game, "c2", "b7"); restored(game);
    }

    @Test public void draftKeepsItsOriginalRosterWhenAPawnBecomesAClone() throws Exception {
        var draft = new Draft(new DraftRules(0, 1));
        assertEquals(DraftActionResult.SUCCESS, draft.pick(WHITE, HeroType.SUN_WUKONG, pos("c2")));
        assertEquals(DraftActionResult.SUCCESS, draft.finishPicks(BLACK));
        Game game = Game.mobaFromDraft(draft.getSnapshot(), RULES, 600, () -> 0L);
        game = charge(game, "c2", 1);
        clone(game, "c2", "f2");
        assertEquals(1, game.getDraft().picksFor(WHITE).size());
        assertEquals(draft.getSnapshot(), restored(game).getDraft());
        assertEquals(2, game.getSnapshot().pieces().values().stream().filter(p -> p.hero() == HeroType.SUN_WUKONG).count());
    }

    @Test public void bothCopiedAbilitiesKeepGinsengIdentityAndOwnResources() throws Exception {
        for (boolean ultimate : List.of(false, true)) {
            var board = board();
            GinsengFixtures.put(board, Pawn.class, BLACK, "c2");
            GinsengFixtures.put(board, Pawn.class, BLACK, "g2");
            GinsengFixtures.empty(board, "a1"); GinsengFixtures.put(board, King.class, WHITE, "a8");
            GinsengFixtures.put(board, Bishop.class, WHITE, "e2");
            Game game = create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG, pos("e2"), HeroType.GINSENG), RULES);
            game = charge(charge(game, "c2", 1), "e2", 2);
            enemyCast(game,"c2",ultimate ? SunWukongAbility.CLONE : SunWukongAbility.SHAPESHIFT,ultimate ? "g2" : null);
            ok(game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("c2"), GinsengAbility.COPY_SPELL));
            play(game, "a3", "b1"); play(game, "f6", "g8");
            var actor = game.getPieceState(pos("e2"));
            var spell = ultimate ? SunWukongAbility.CLONE : SunWukongAbility.SHAPESHIFT;
            ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("e2"), spell,
                    ultimate ? pos("f2") : null, null, true)));
            assertEquals(HeroType.GINSENG, game.getPieceState(pos("e2")).hero());
            assertEquals(actor.cooldown(), game.getPieceState(pos("e2")).cooldown());
            assertEquals(actor.ultimateCharge(), game.getPieceState(pos("e2")).ultimateCharge());
            assertFalse(game.getSpells().borrowed().containsKey(actor.id()));
            game = restored(game);
            nextWhite(game); play(game, "a3", "b1");
            if (!ultimate) assertEquals(Bishop.class, game.getPieceState(pos("e2")).type());
            else assertEquals(HeroType.SUN_WUKONG, game.getPieceState(pos("f2")).hero());
            restored(game);
        }
    }

    @Test public void copiesHaveTheSamePawnHeroCapturePrice() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Rook.class, BLACK, "f4");
        Game game = charge(create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG), RULES), "c2", 1);
        clone(game, "c2", "f2");
        play(game, "b1", "a3"); play(game, "f4", "f2");
        assertEquals(4, game.getEconomy().blackHalves());
        restored(game);
    }

    @Test public void forgedCloneOriginsAndRandomFormsAreRejected() throws Exception {
        Game game = ready(); clone(game, "c2", "f2");
        var snapshot = game.getSnapshot();
        var state = snapshot.mechanics().get(SunWukongMechanic.KEY);
        assertThrows(IllegalArgumentException.class, () -> Game.fromSnapshot(snapshot.withMechanics(snapshot.mechanics()
                .with(SunWukongMechanic.KEY, new WukongState(state.initialSeed(), state.randomState(), state.forms(), Map.of()))), () -> 0L));
        byte[] encoded = GameSaveFile.encode(snapshot);
        String xml = new String(encoded, StandardCharsets.UTF_8);
        assertTrue(xml.contains("formatVersion=\"18\""));
        assertTrue(xml.contains("sunWukongRules"));
        assertTrue(xml.contains("sunwukong.cloned"));
        String downgraded = xml.replace("formatVersion=\"18\"", "formatVersion=\"17\"")
                .replaceAll("<sunWukongRules[^>]*/>", "");
        assertThrows(java.io.IOException.class, () -> GameSaveFile.decode(downgraded.getBytes(StandardCharsets.UTF_8)));
        assertThrows(java.io.IOException.class, () -> GameSaveFile.decode(xml.replace("sunwukong.cloned", "sunwukong.unknown").getBytes(StandardCharsets.UTF_8)));
    }

    private static long seedFor(WukongState.Body body) {
        for (long seed = 0; seed < 100; seed++) {
            var chosen = List.of(WukongState.Body.KNIGHT, WukongState.Body.BISHOP, WukongState.Body.ROOK)
                    .get(SummonState.initial(seed).choose(3).index());
            if (chosen == body) return seed;
        }
        throw new AssertionError("No deterministic seed for body");
    }

    @Test public void temporaryBodyMovesAsItsChessTypeAndReturnsAtItsNewSquare() throws Exception {
        for (var body : List.of(WukongState.Body.KNIGHT, WukongState.Body.BISHOP, WukongState.Body.ROOK)) {
            Game game = seed(basic(), seedFor(body));
            long id = game.getPieceState(pos("c2")).id();
            shift(game, "c2");
            String to = body == WukongState.Body.KNIGHT ? "d4" : body == WukongState.Body.BISHOP ? "d3" : "c3";
            play(game, "c2", to); play(game, "g8", "f6");
            game = restored(game);
            play(game, "b1", "a3");
            assertEquals(Pawn.class, game.getPieceState(pos(to)).type());
            assertEquals(id, game.getPieceState(pos(to)).id());
            restored(game);
        }
    }

    @Test public void aCapturedTemporaryBodyKeepsItsCapturePriceAndNeverResurrects() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Rook.class, BLACK, "c4");
        Game game = seed(create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG), RULES), seedFor(WukongState.Body.ROOK));
        long id = game.getPieceState(pos("c2")).id();
        shift(game, "c2");
        play(game, "b1", "a3"); play(game, "c4", "c2");
        assertEquals(10, game.getEconomy().blackHalves());
        game = restored(game);
        play(game, "a3", "b1");
        assertEquals(PieceState.Status.CAPTURED, game.getSnapshot().pieces().get(id).status());
        assertEquals(Rook.class, game.getSnapshot().pieces().get(id).type());
        assertFalse(game.getSnapshot().mechanics().get(SunWukongMechanic.KEY).forms().containsKey(id));
        restored(game);
    }

    @Test public void swallowedTemporaryHeroReturnsToPawnInsidePudge() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Rook.class, WHITE, "d2");
        Game game = create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG, pos("d2"), HeroType.PUDGE), RULES);
        game = charge(game, "d2", 2);
        long id = game.getPieceState(pos("c2")).id();
        shift(game, "c2"); nextWhite(game);
        ok(game.useAbility(game.getCurrentPlayer(), pos("d2"), pos("c2"), PudgeAbility.SWALLOW));
        play(game, "a3", "b1");
        var swallowed = game.getSnapshot().pieces().get(id);
        assertEquals(PieceState.Status.SWALLOWED, swallowed.status());
        assertEquals(Pawn.class, swallowed.type());
        assertEquals(Long.valueOf(id), game.getPieceState(pos("d2")).storedPieceId());
        restored(game);
    }

    @Test public void rejectedTransformationDoesNotConsumeRandomnessResourcesOrBudget() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Rook.class, BLACK, "a4");
        Game game = create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG), RULES);
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertFalse(game.useAbility(game.getCurrentPlayer(), pos("c2"), null, SunWukongAbility.SHAPESHIFT).isSuccess());
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
    }

    @Test public void blackAndWhiteHaveIndependentSharedCooldowns() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Pawn.class, BLACK, "b7");
        Game game = create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG, pos("b7"), HeroType.SUN_WUKONG), RULES);
        shift(game, "c2"); play(game, "b1", "a3");
        assertEquals(0, game.getPieceState(pos("b7")).cooldown());
        shift(game, "b7"); play(game, "g8", "f6");
        assertEquals(3, game.getPieceState(pos("c2")).cooldown());
        assertEquals(4, game.getPieceState(pos("b7")).cooldown());
        restored(game);
    }

    @Test public void configuredLifetimeAndCooldownAreUsedInsteadOfPrototypeConstants() throws Exception {
        var rules = RULES.with(SunWukongDefinition.INSTANCE, new SunWukongRules(6, 2, 3));
        Game game = create(board(), Map.of(pos("c2"), HeroType.SUN_WUKONG), rules);
        shift(game, "c2");
        assertEquals(6, game.getPieceState(pos("c2")).cooldown());
        nextWhite(game); play(game, "a3", "b1");
        assertNotEquals(Pawn.class, game.getPieceState(pos("c2")).type());
        play(game, "f6", "g8"); play(game, "b1", "a3");
        assertEquals(Pawn.class, game.getPieceState(pos("c2")).type());
        restored(game);
    }

    @Test public void promotionKeepsHeroAndTemporarySkillRestoresThePromotedBody() throws Exception {
        var board = board();
        GinsengFixtures.empty(board, "c2");
        GinsengFixtures.put(board, Pawn.class, WHITE, "c7");
        Game game = create(board, Map.of(pos("c7"), HeroType.SUN_WUKONG), RULES);
        ok(game.moveFigure(game.getCurrentPlayer(), pos("c7"), pos("c8"), Promotion.QUEEN));
        shift(game, "c8");
        ok(game.endTurn(game.getCurrentPlayer()));
        play(game, "h8", "h7"); play(game, "b1", "a3");
        assertEquals(model.figures.Queen.class, game.getPieceState(pos("c8")).type());
        assertEquals(HeroType.SUN_WUKONG, game.getPieceState(pos("c8")).hero());
        restored(game);
    }

    @Test public void clonePreservesBlindnessInsteadOfCleansingTheTarget() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Bishop.class, BLACK, "f5");
        Game game = create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG, pos("f5"), HeroType.LUMESHARD), RULES);
        game = charge(game, "c2", 1);
        play(game, "b1", "a3");
        ok(game.useAbility(game.getCurrentPlayer(), pos("f5"), pos("f2"), LumeshardAbility.LASER));
        play(game, "g8", "f6");
        assertTrue(game.getPieceState(pos("f2")).isBlinded());
        clone(game, "c2", "f2");
        assertTrue(game.getPieceState(pos("f2")).isBlinded());
        restored(game);
    }

    private static void enemyCast(Game game, String source, game.heroes.Abilities ability, String target) {
        boolean white = game.getPieceState(pos("b1")) != null;
        play(game, white ? "b1" : "a3", white ? "a3" : "b1");
        ok(game.useAbility(game.getCurrentPlayer(), pos(source), target == null ? null : pos(target), ability));
        boolean black = game.getPieceState(pos("g8")) != null;
        play(game, black ? "g8" : "f6", black ? "f6" : "g8");
    }
    private static Game withCopySource(HeroType sourceHero, Class<? extends Figure> sourceBody, HeroRules rules) {
        var board = board();
        GinsengFixtures.put(board, Bishop.class, WHITE, "e2");
        GinsengFixtures.put(board, sourceBody, BLACK, "f2");
        GinsengFixtures.put(board, Pawn.class, BLACK, "c2");
        GinsengFixtures.empty(board, "a1"); GinsengFixtures.put(board, King.class, WHITE, "a8");
        Game game = create(board, Map.of(pos("c2"), HeroType.SUN_WUKONG,
                pos("e2"), HeroType.GINSENG, pos("f2"), sourceHero), rules);
        return charge(game, "e2", 2);
    }

    private static void borrowed(Game game, game.heroes.Abilities ability, String target) {
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("e2"), ability,
                target == null ? null : pos(target), null, true)));
    }

    @Test public void evolvedGinsengReturnsToKnightAfterBorrowedTemporaryFormAndLoading() throws Exception {
        Game game = charge(withCopySource(HeroType.CHRONOMLIN, Pawn.class, RULES), "f2", 1);
        enemyCast(game,"f2",ChronomlinAbility.EVOLVE,null);
        ok(game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("f2"), GinsengAbility.COPY_SPELL));
        play(game, "a3", "b1"); play(game, "f6", "g8");
        borrowed(game, ChronomlinAbility.EVOLVE, null);
        assertEquals(Knight.class, game.getPieceState(pos("e2")).type());
        nextWhite(game);
        enemyCast(game,"c2",SunWukongAbility.SHAPESHIFT,null);
        game = charge(game, "e2", 2);
        ok(game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("c2"), GinsengAbility.COPY_SPELL));
        nextWhite(game);
        borrowed(game, SunWukongAbility.SHAPESHIFT, null);
        long id = game.getPieceState(pos("e2")).id();
        assertEquals(WukongState.Body.KNIGHT,
                game.getSnapshot().mechanics().get(SunWukongMechanic.KEY).forms().get(id).original());
        var event = game.getEvents().stream().filter(e -> e.isEffect(SunWukongMechanic.SHIFT)
                && e.actorId() == id).reduce((a, b) -> b).orElseThrow();
        var timeline = PieceBodyHistory.forPieces(game.getSnapshot(), Set.of(id));
        assertEquals(Knight.class, timeline.before(id, event.sequence()));
        assertEquals(game.getPieceState(pos("e2")).type(), timeline.latest(id));
        game = restored(game);
        play(game, "a3", "b1"); play(game, "f6", "g8"); play(game, "b1", "a3");
        assertEquals(Knight.class, game.getPieceState(pos("e2")).type());
        restored(game);
    }


    private static long seedForSecond(WukongState.Body body) {
        for (long seed = 0; seed < 100; seed++) {
            long afterFirst = SummonState.initial(seed).choose(3).nextState();
            var second = List.of(WukongState.Body.KNIGHT, WukongState.Body.BISHOP, WukongState.Body.ROOK)
                    .get(SummonState.initial(afterFirst).choose(3).index());
            if (second == body) return seed;
        }
        throw new AssertionError("No seed for the second transformation");
    }

    @Test public void borrowedEvolutionInsideTemporaryFormDoesNotChangeTheRememberedOriginal() throws Exception {
        var rules = RULES.with(SunWukongDefinition.INSTANCE, new SunWukongRules(4, 2, 1));
        Game game = charge(withCopySource(HeroType.CHRONOMLIN, Pawn.class, rules), "f2", 1);
        game = seed(game, seedForSecond(WukongState.Body.KNIGHT));
        enemyCast(game,"f2",ChronomlinAbility.EVOLVE,null);
        enemyCast(game,"c2",SunWukongAbility.SHAPESHIFT,null);
        ok(game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("c2"), GinsengAbility.COPY_SPELL));
        nextWhite(game);
        borrowed(game, SunWukongAbility.SHAPESHIFT, null);
        assertEquals(Knight.class, game.getPieceState(pos("e2")).type());
        play(game, "a3", "b1"); play(game, "f6", "g8");
        game = charge(game, "e2", 2);
        ok(game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("f2"), GinsengAbility.COPY_SPELL));
        nextWhite(game);
        borrowed(game, ChronomlinAbility.EVOLVE, null);
        assertEquals(Rook.class, game.getPieceState(pos("e2")).type());
        game = restored(game);
        play(game, "a3", "b1");
        assertEquals(Bishop.class, game.getPieceState(pos("e2")).type());
        restored(game);
    }

    @Test public void illusionKeepsItsBirthBodyWhenGinsengsTemporaryFormExpires() throws Exception {
        var rules = RULES.with(SunWukongDefinition.INSTANCE, new SunWukongRules(4, 3, 1));
        Game game = charge(withCopySource(HeroType.CLOWNGANGER, Knight.class, rules), "f2", 1);
        game = seed(game, seedForSecond(WukongState.Body.ROOK));
        enemyCast(game,"c2",SunWukongAbility.SHAPESHIFT,null);
        ok(game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("c2"), GinsengAbility.COPY_SPELL));
        play(game, "a3", "b1"); play(game, "f6", "g8");
        borrowed(game, SunWukongAbility.SHAPESHIFT, null);
        assertEquals(Rook.class, game.getPieceState(pos("e2")).type());
        long source = game.getPieceState(pos("e2")).id();
        nextWhite(game);
        enemyCast(game,"f2",ClowngangerAbility.ILLUSION,"f3");
        game = charge(game, "e2", 2);
        Position original = game.getSnapshot().pieces().values().stream()
                .filter(p -> p.hero() == HeroType.CLOWNGANGER).findFirst().orElseThrow().position();
        ok(game.useAbility(game.getCurrentPlayer(), pos("e2"), original, GinsengAbility.COPY_SPELL));
        nextWhite(game);
        borrowed(game, ClowngangerAbility.ILLUSION, "d2");
        long illusion = game.getSnapshot().clown().illusions().values().stream()
                .filter(i -> i.sourceId() == source).findFirst().orElseThrow().id();
        assertEquals(Rook.class, game.getPieceState(illusion).type());
        game = restored(game);
        play(game, "a3", "b1");
        assertEquals(Bishop.class, game.getPieceState(source).type());
        assertEquals(Rook.class, game.getPieceState(illusion).type());
        restored(game);
    }
}
