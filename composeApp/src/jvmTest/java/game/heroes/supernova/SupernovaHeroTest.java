package game.heroes.supernova;

import game.core.Game;
import game.heroes.AbilityRequest;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.GinsengFixtures;
import game.heroes.phoenix.PhoenixAbility;
import game.heroes.phoenix.PhoenixMechanic;
import game.heroes.moleminer.MoleminerAbility;
import game.heroes.bolotnik.BolotnikAbility;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.persistence.GameSaveFile;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import model.Figure;
import model.Position;
import model.figures.Archbishop;
import model.figures.Bishop;
import model.figures.King;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;
import rules.moba.TerrainGeometry;
import rules.standard.MoveValidator;

import static game.core.Positions.pos;
import static game.heroes.phoenix.PhoenixHeroTest.*;
import static model.FigureColor.WHITE;
import static model.FigureColor.BLACK;
import static org.junit.Assert.*;

public class SupernovaHeroTest {
    private static Game seeded(Game game, long seed) {
        var s = game.getSnapshot();
        return Game.fromSnapshot(s.withMechanics(s.mechanics().with(SupernovaMechanic.KEY,
                new SupernovaMechanic.State(seed, seed))), () -> 0L);
    }
    private static void cast(Game game, String from, String center, SupernovaAbility ability) {
        ok(game.useAbility(game.getCurrentPlayer(), pos(from), pos(center), ability));
    }
    @Test public void pullUsesBothSidesAndSelfLeavesKingsAndOutsidePiecesAndChargesCooldown() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Pawn.class, BLACK, "c6");
        GinsengFixtures.put(board, Pawn.class, WHITE, "g4");
        Game game = hatch(create(board, Map.of()));
        long actor = game.getPieceState(pos("b1")).id(), enemy = game.getPieceState(pos("c6")).id();
        cast(game, "b1", "c3", SupernovaAbility.PULL);
        assertEquals(1, SupernovaHero.distance(game.getPieceState(actor).position(), pos("c3")));
        assertEquals(1, SupernovaHero.distance(game.getPieceState(enemy).position(), pos("c3")));
        assertEquals(2, game.getPieceState(actor).cooldown());
        assertNotNull(game.getPieceState(pos("h1"))); assertNotNull(game.getPieceState(pos("h8")));
        assertNotNull(game.getPieceState(pos("g4")));
        assertFalse(game.getTurnState().moveUsed());
        assertEquals(game.getSnapshot().pieces().values().stream().filter(p -> p.position() != null).count(),
                game.getSnapshot().pieces().values().stream().map(PieceState::position).filter(java.util.Objects::nonNull).distinct().count());
        game = restored(game);
        String square = cell(game.getPieceState(actor).position());
        play(game, "h5", "h6");
        // После сдвига конь соперника мог оказаться в другой клетке; ходим его королём.
        play(game, "h8", "g8");
        assertEquals(1, game.getPieceState(actor).cooldown());
        assertEquals(AbilityRejection.ABILITY_COOLDOWN, game.getAbilityAvailability(pos(square), SupernovaAbility.PULL));
        restored(game);
    }
    private static String cell(Position p) { return "" + (char)('a' + p.getColumn()) + (p.getRow() + 1); }
    @Test public void fullRingDoesNotOverwritePiecesOrConsumeTheSkill() throws Exception {
        var board = board();
        for (String cell : new String[]{"b2","d2","b3","d3","b4","c4","d4"})
            GinsengFixtures.put(board, Pawn.class, WHITE, cell);
        Game game = hatch(create(board, Map.of()));
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertFalse(game.useAbility(game.getCurrentPlayer(), pos("b1"), pos("c3"), SupernovaAbility.PULL).isSuccess());
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
    }
    @Test public void shuffleUsesOnlyOccupiedSquaresIsDeterministicAndPreviewsArePure() throws Exception {
        Game game = seeded(charge(supernova(), "b1", 1), 123L);
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        var targets = game.getAbilityTargets(pos("b1"), SupernovaAbility.SHUFFLE);
        assertTrue(targets.contains(pos("c3")));
        assertEquals(targets, game.getAbilityTargets(pos("b1"), SupernovaAbility.SHUFFLE));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
        Game other = restored(game);
        var occupied = game.getSnapshot().pieces().values().stream().filter(p -> p.position() != null)
                .map(PieceState::position).collect(java.util.stream.Collectors.toSet());
        cast(game, "b1", "c3", SupernovaAbility.SHUFFLE);
        cast(other, "b1", "c3", SupernovaAbility.SHUFFLE);
        assertArrayEquals(GameSaveFile.encode(game.getSnapshot()), GameSaveFile.encode(other.getSnapshot()));
        assertEquals(occupied, game.getSnapshot().pieces().values().stream().filter(p -> p.position() != null)
                .map(PieceState::position).collect(java.util.stream.Collectors.toSet()));
        assertEquals(0, game.getSnapshot().pieces().values().stream().filter(p -> p.hero() == HeroType.SUPERNOVA)
                .findFirst().orElseThrow().ultimateCharge());
        restored(game);
    }
    @Test public void distantCenterIsRejectedWithoutChargeOrRandomChanges() throws Exception {
        Game game = charge(supernova(), "b1", 1);
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,
                game.useAbility(game.getCurrentPlayer(), pos("b1"), pos("h8"), SupernovaAbility.SHUFFLE));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
    }
    @Test public void movedKingLosesBothCastlingRights() throws Exception {
        var board = board();
        GinsengFixtures.empty(board, "h1");
        GinsengFixtures.put(board, King.class, WHITE, "e1");
        GinsengFixtures.put(board, Rook.class, WHITE, "a1");
        GinsengFixtures.put(board, Rook.class, WHITE, "h1");
        Game initial = Game.fromMobaPosition(new GamePosition(board, WHITE, new CastlingRights(true, true, false, false), null, 0),
                Map.of(pos("b1"), HeroType.PHOENIX), HeroRules.defaults(), () -> 0L);
        Game ready = charge(hatch(initial), "b1", 1);
        long king = ready.getPieceState(pos("e1")).id();
        for (long seed = 0; seed < 100; seed++) {
            Game game = seeded(ready, seed);
            var result = game.useAbility(game.getCurrentPlayer(), pos("b1"), pos("d3"), SupernovaAbility.SHUFFLE);
            if (!result.isSuccess() || pos("e1").equals(game.getPieceState(king).position())) continue;
            assertFalse(game.getPosition().castlingRights().whiteKingSide());
            assertFalse(game.getPosition().castlingRights().whiteQueenSide());
            restored(game); return;
        }
        fail("No legal king relocation found");
    }
    @Test public void unsafeKingPermutationRejectsWholeCastAndKeepsRandomSequence() throws Exception {
        var board = board(); GinsengFixtures.empty(board, "h1");
        GinsengFixtures.put(board, King.class, WHITE, "e1");
        GinsengFixtures.put(board, Rook.class, BLACK, "e8");
        GinsengFixtures.put(board, Pawn.class, WHITE, "e2");
        Game ready = charge(hatch(create(board, Map.of())), "b1", 1);
        boolean rejected = false;
        for (long seed = 0; seed < 100; seed++) {
            Game game = seeded(ready, seed);
            byte[] before = GameSaveFile.encode(game.getSnapshot());
            var result = game.useAbility(game.getCurrentPlayer(), pos("b1"), pos("d3"), SupernovaAbility.SHUFFLE);
            if (result != MovementRejection.KING_IN_CHECK) continue;
            rejected = true;
            assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot())); restored(game); break;
        }
        assertTrue("Expected a permutation exposing the king", rejected);
    }
    @Test public void archbishopAttacksLikeBishopAndKnightAndMudBlocksBothPatterns() {
        var board = board(); GinsengFixtures.put(board, Archbishop.class, BLACK, "f2");
        assertTrue(MoveValidator.isInCheck(board, WHITE)); // f2 → h1, ход коня.
        GinsengFixtures.empty(board, "f2"); GinsengFixtures.put(board, Archbishop.class, BLACK, "e4");
        assertTrue(MoveValidator.isInCheck(board, WHITE)); // e4 → h1, диагональ.
        GinsengFixtures.put(board, Pawn.class, WHITE, "f3");
        assertFalse(MoveValidator.isInCheck(board, WHITE));
        assertTrue(TerrainGeometry.blocks(pos("b1"), pos("c3"), Archbishop.class, Set.of(pos("b2"))));
        assertTrue(TerrainGeometry.blocks(pos("b1"), pos("e4"), Archbishop.class, Set.of(pos("c2"))));
    }
    @Test public void forgedEggDeadlineAndSupernovaOriginAreRejected() throws Exception {
        Game game = charge(basic(), "b1", 2); rebirth(game, "b1");
        String xml = new String(GameSaveFile.encode(game.getSnapshot()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("name=\"until\" value=\"5\""));
        assertThrows(java.io.IOException.class, () -> Game.fromSnapshot(GameSaveFile.decode(xml.replace("name=\"until\" value=\"5\"", "name=\"until\" value=\"7\"")
                .getBytes(StandardCharsets.UTF_8)), () -> 0L));
        Game nova = supernova();
        assertThrows(IllegalArgumentException.class, () -> Game.fromSnapshot(nova.getSnapshot().withMechanics(
                nova.getSnapshot().mechanics().with(PhoenixMechanic.KEY, PhoenixMechanic.KEY.initial())), () -> 0L));
    }

    private static Game enemyNova() {
        var b = board();
        GinsengFixtures.put(b, Knight.class, BLACK, "b1");
        Game game = charge(create(b, Map.of(pos("c2"), HeroType.GINSENG)), "b1", 2);
        play(game, "h2", "h3"); rebirth(game, "b1"); play(game, "g8", "f6");
        play(game, "h3", "h4"); play(game, "f6", "g8");
        play(game, "h4", "h5"); play(game, "g8", "f6");
        return game;
    }
    @Test public void ginsengCopiesPullWithItsOwnIdentityAndWithoutCooldownCost() throws Exception {
        Game game = charge(enemyNova(), "c2", 2);
        long ginseng = game.getPieceState(pos("c2")).id(), nova = game.getPieceState(pos("b1")).id();
        play(game, "h5", "h6");
        cast(game, "b1", "c3", SupernovaAbility.PULL); play(game, "h8", "g8");
        ok(game.useAbility(game.getCurrentPlayer(), game.getPieceState(ginseng).position(),
                game.getPieceState(nova).position(), GinsengAbility.COPY_SPELL));
        play(game, "h6", "h7"); play(game, "g8", "h8");
        int sourceCooldown = game.getPieceState(nova).cooldown();
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(game.getPieceState(ginseng).position(),
                SupernovaAbility.PULL, pos("d4"), null, true)));
        assertEquals(HeroType.GINSENG, game.getPieceState(ginseng).hero());
        assertEquals(Bishop.class, game.getPieceState(ginseng).type());
        assertEquals(0, game.getPieceState(ginseng).cooldown());
        assertEquals(sourceCooldown, game.getPieceState(nova).cooldown());
        assertFalse(game.getSpells().borrowed().containsKey(ginseng)); restored(game);
    }
    @Test public void ginsengCopiesShuffleWithoutAnotherCaptureCharge() throws Exception {
        Game game = seeded(charge(charge(enemyNova(),
                "c2", 2), "b1", 1), 123L);
        long ginseng = game.getPieceState(pos("c2")).id(), nova = game.getPieceState(pos("b1")).id();
        play(game, "h5", "h6");
        cast(game, "b1", "b1", SupernovaAbility.SHUFFLE); play(game, "f6", "g8");
        ok(game.useAbility(game.getCurrentPlayer(), game.getPieceState(ginseng).position(),
                game.getPieceState(nova).position(), GinsengAbility.COPY_SPELL));
        play(game, "h6", "h7"); play(game, "g8", "f6");
        assertEquals(0, game.getPieceState(ginseng).ultimateCharge());
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(game.getPieceState(ginseng).position(),
                SupernovaAbility.SHUFFLE, pos("b1"), null, true)));
        assertEquals(HeroType.GINSENG, game.getPieceState(ginseng).hero());
        assertEquals(0, game.getPieceState(nova).ultimateCharge());
        assertFalse(game.getSpells().borrowed().containsKey(ginseng)); restored(game);
    }
}
