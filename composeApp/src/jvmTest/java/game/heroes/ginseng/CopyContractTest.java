package game.heroes.ginseng;

import game.core.Game;
import game.heroes.Abilities;
import game.heroes.AbstractHero;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroEffect;
import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRules;
import game.heroes.HeroRulesCodec;
import game.heroes.HeroTargeting;
import game.heroes.HeroType;
import game.history.AbilityEventTarget;
import game.history.GameEvent;
import game.moves.Success;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicCaptureReaction;
import game.mechanics.MechanicStateCodec;
import game.mechanics.MechanicPhase;
import game.mechanics.Mechanics;
import game.persistence.GameSaveFile;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import model.figures.Bishop;
import model.figures.Rook;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static game.core.Positions.pos;
import static model.FigureColor.WHITE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Копия способности, отсутствующей во всех производственных enum и адаптерах. */
public class CopyContractTest {
    private enum Spell implements Abilities {
        CAST, ULTIMATE;
        @Override public String id() { return "copy.contract." + name(); }
        @Override public String getDisplayName() { return name(); }
        @Override public HeroTargeting targeting() { return HeroTargeting.untargeted(); }
        @Override public AbilityEventTarget eventTarget() { return AbilityEventTarget.NONE; }
    }
    private record Rules(int cooldown) {}
    private record EchoState(Set<Long> owners) {
        EchoState { owners = Set.copyOf(owners); }
    }
    private record Arm(long owner) implements MechanicOperation {}
    private record Release(long owner, long killer) implements MechanicOperation {}
    private static final MechanicKey<EchoState> ECHO = new MechanicKey<>("copy.echo", EchoState.class,
            new EchoState(Set.of()));
    private static final MechanicDefinition<EchoState> MODULE = new MechanicDefinition<>() {
        @Override public MechanicKey<EchoState> key() { return ECHO; }
        @Override public MechanicStateCodec<EchoState> codec() {
            return new MechanicStateCodec<>() {
                @Override public Map<String, String> encode(EchoState state) {
                    return Map.of("owners", state.owners().stream().sorted().map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(",")));
                }
                @Override public EchoState decode(Map<String, String> fields) {
                    String ids = fields.get("owners");
                    return new EchoState(ids.isEmpty() ? Set.of() : java.util.Arrays.stream(ids.split(","))
                            .map(Long::parseLong).collect(java.util.stream.Collectors.toSet()));
                }
            };
        }
        @Override public List<MechanicHandler<?>> handlers() {
            return List.of(Mechanics.handler(Arm.class, "copy.echo.arm", MechanicPhase.ABILITY_PREPARE, null,
                    (session, operation) -> {
                        var owners = new java.util.HashSet<>(session.state(ECHO).owners());
                        owners.add(operation.owner());
                        session.state(ECHO, new EchoState(owners));
                        return Success.SUCCESS;
                    }), Mechanics.handler(Release.class, "copy.echo.release", MechanicPhase.ABILITY_PREPARE,
                    "copy.echo.arm", (session, operation) -> {
                        var owners = new java.util.HashSet<>(session.state(ECHO).owners());
                        owners.remove(operation.owner());
                        session.state(ECHO, new EchoState(owners));
                        PieceState killer = session.pieces().get(operation.killer());
                        if (killer != null && !killer.isRemoved())
                            session.update(Map.of(killer.id(), killer.withBlindness(1)));
                        return Success.SUCCESS;
                    }));
        }
        @Override public List<MechanicCaptureReaction> captureReactions() {
            return List.of(new MechanicCaptureReaction("copy.echo.death", death ->
                    death.context().state(ECHO).owners().contains(death.victim().id())
                            ? HeroEffect.empty().withOperations(new Release(death.victim().id(), death.killerId()))
                            : HeroEffect.empty()));
        }
    };
    private static final HeroRulesCodec<Rules> CODEC = new HeroRulesCodec<>() {
        @Override public String section() { return "copyContractRules"; }
        @Override public int sinceVersion() { return 16; }
        @Override public Map<String, String> encode(Rules rules) { return Map.of("cooldown", "" + rules.cooldown()); }
        @Override public Rules decode(HeroRuleAttributes attributes) { return new Rules(attributes.integer("cooldown")); }
    };
    // Test-only replacement of an existing identity; production additions register their own ID.
    private static final HeroDefinition<Rules> DEFINITION = new HeroDefinition<>(HeroType.PUDGE,
            "Independent caster", Set.of(Rook.class), Rules.class, new Rules(7), CODEC, Caster::new, List.of(MODULE));
    private static final class Caster extends AbstractHero {
        private final Rules rules;
        Caster(Rules rules) { super(DEFINITION, List.of(Spell.values()), Spell.ULTIMATE, 3); this.rules = rules; }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                        PieceState target, AbilityRequest request) {
            if (request.borrowed()) {
                assertSame(HeroType.GINSENG, actor.hero());
                assertSame(Bishop.class, actor.type());
                assertSame(actor, context.pieceById(actor.id()));
                assertEquals(2, actor.cooldown());
                assertEquals(0, actor.ultimateCharge());
            }
            return HeroEffect.success(Map.of(actor.id(), actor.withBlindness(1)))
                    .withOperations(new Arm(actor.id())).withCost(request.ability() == Spell.CAST ? AbilityCost.cooldown(rules.cooldown())
                            : AbilityCost.ultimate(3));
        }
    }

    private static HeroRules configuration() {
        var definitions = new ArrayList<>(HeroDefinitions.all());
        definitions.removeIf(d -> d.type() == HeroType.PUDGE);
        definitions.add(DEFINITION);
        return HeroRules.defaults(definitions).with(DEFINITION, new Rules(5));
    }
    private static Game game() {
        var board = GinsengFixtures.board();
        GinsengFixtures.put(board, Rook.class, WHITE, "e2");
        GinsengFixtures.put(board, Rook.class, model.FigureColor.BLACK, "c8");
        return Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                Map.of(pos("c2"), HeroType.GINSENG, pos("e2"), HeroType.PUDGE), configuration(), () -> 0L);
    }
    private static Game resources(Game game, String square, int cooldown, int charge) {
        var s = game.getSnapshot();
        var pieces = new HashMap<>(s.pieces());
        var actor = s.pieceAt(pos(square));
        pieces.put(actor.id(), actor.withAbilityState(cooldown, charge, actor.storedPieceId()));
        return Game.fromSnapshot(new GameSnapshot(s.version(), s.mode(), s.field(), pieces, s.turn(), s.active(),
                s.castlingRights(), s.enPassantTarget(), s.halfmoveClock(), s.repetitions(), s.clock(), s.outcome(),
                s.moves(), s.events(), s.whitePoints(), s.blackPoints(), s.whiteCaptures(), s.blackCaptures(),
                s.drawOffer(), s.heroRules(), s.draft(), s.mechanics()), () -> 0L);
    }
    private static void play(Game game, String from, String to) {
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to)));
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer()));
    }
    private static void cycle(Game game) {
        boolean initial = game.getPieceState(pos("b1")) != null;
        play(game, initial ? "b1" : "a3", initial ? "a3" : "b1");
        play(game, initial ? "g8" : "f6", initial ? "f6" : "g8");
    }
    private static Game roundtrip(Game game) throws Exception {
        byte[] xml = GameSaveFile.encode(game.getSnapshot());
        Game loaded = Game.fromSnapshot(GameSaveFile.decode(xml, configuration()), () -> 0L);
        assertArrayEquals(xml, GameSaveFile.encode(loaded.getSnapshot()));
        return loaded;
    }

    @Test public void copiedMechanicSurvivesConsumptionAndLoadAndReactsWithoutTheSourceHero() throws Exception {
        Game game = resources(game(), "c2", 0, 2);
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("e2"), null, Spell.CAST));
        cycle(game);
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("e2"), GinsengAbility.COPY_SPELL));
        cycle(game);
        game = resources(game, "c2", 2, 0);
        long actor = game.getPieceState(pos("c2")).id();
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(),
                new AbilityRequest(pos("c2"), Spell.CAST, null, null, true)));
        assertTrue(game.getSpells().borrowed().isEmpty());
        game = roundtrip(game);
        assertTrue(game.getSnapshot().mechanics().get(ECHO).owners().contains(actor));
        play(game, "b1", "a3");
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("c8"), pos("c2")));
        assertTrue(game.getPieceState(pos("c2")).isBlinded());
        assertFalse(game.getSnapshot().mechanics().get(ECHO).owners().contains(actor));
        roundtrip(game);
    }

    @Test public void newAbilitiesUseGameRulesAndRealCasterForCopiesIncludingAfterLoad() throws Exception {
        for (Spell spell : Spell.values()) {
            Game game = resources(resources(game(), "c2", 0, 2), "e2", 0, 3);
            assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("e2"), null, spell));
            assertEquals(spell == Spell.CAST ? 5 : 0, game.getPieceState(pos("e2")).cooldown());
            assertEquals(spell == Spell.CAST ? 3 : 0, game.getPieceState(pos("e2")).ultimateCharge());
            cycle(game);
            assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("e2"), GinsengAbility.COPY_SPELL));
            cycle(game);
            game = roundtrip(resources(game, "c2", 2, 0));
            PieceState actor = game.getPieceState(pos("c2"));
            assertSame(spell, game.getSpells().borrowed().get(actor.id()).ability());
            assertEquals(Success.SUCCESS, game.getAbilityAvailability(actor.position(), spell, true));
            assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(),
                    new AbilityRequest(actor.position(), spell, null, null, true)));
            assertEquals(actor.withBlindness(1), game.getPieceState(actor.position()));
            assertFalse(game.getSpells().borrowed().containsKey(actor.id()));
            GameEvent event = game.getSnapshot().events().getLast();
            assertSame(spell, event.ability());
            assertTrue(event.borrowed());
            game = roundtrip(game);
            cycle(game);
            assertSame(spell, game.getSpells().lastUsed().get(actor.id()));
        }
    }
}
