package game.mechanics;

import game.heroes.AbilityRequest;

import game.core.Game;
import game.heroes.Abilities;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroEffect;
import game.heroes.HeroRules;
import game.heroes.HeroTargeting;
import game.heroes.HeroType;
import game.heroes.NoHeroRules;
import game.history.AbilityEventTarget;
import game.history.GameEvent;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
import game.persistence.GameSaveFile;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.King;
import model.figures.Rook;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.END_TURN;
import static game.mechanics.MechanicPhase.ENTRY;
import static game.mechanics.MechanicPhase.HISTORY;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.step;
import static game.mechanics.Mechanics.update;
import static org.junit.Assert.*;

/** Независимая механика: ни движок, ни общий XML не знают её ID или операции. */
public class MechanicIntegrationTest {
    private record Counter(int value) {
        Counter { if (value < 0 || value > 1_000_000) throw new IllegalArgumentException("Counter outside range"); }
    }
    private record Pulse(long owner, int value) {}
    private record Increment(long owner, int amount, boolean reject) implements MechanicOperation {}
    private record Kill(long owner, long victim) implements MechanicOperation {}
    private enum Ability implements Abilities {
        CAST, REJECT, JOURNAL_REJECT, UNSTABLE, KILL;
        @Override public String id() { return "counter." + name(); }
        @Override public String getDisplayName() { return name(); }
        @Override public HeroTargeting targeting() { return this == KILL ? HeroTargeting.square() : HeroTargeting.untargeted(); }
        @Override public AbilityEventTarget eventTarget() { return this == KILL ? AbilityEventTarget.REQUIRED : AbilityEventTarget.NONE; }
    }
    private static final MechanicKey<Counter> KEY = new MechanicKey<>("counter", Counter.class, new Counter(0));
    private static final MechanicEventType<Pulse> PULSE = new MechanicEventType<>("counter.pulse", Pulse.class,
            p -> Map.of("owner", Long.toString(p.owner()), "value", Integer.toString(p.value())),
            data -> {
                if (!data.keySet().equals(Set.of("owner", "value"))) throw new IllegalArgumentException("Pulse fields");
                return new Pulse(Long.parseLong(data.get("owner")), Integer.parseInt(data.get("value")));
            }, new MechanicEventPolicy(false, true, false, true));
    private static final MechanicDefinition<Counter> MODULE = new MechanicDefinition<>() {
        @Override public MechanicKey<Counter> key() { return KEY; }
        @Override public List<MechanicEventType<?>> events() { return List.of(PULSE); }
        @Override public MechanicStateCodec<Counter> codec() {
            return new MechanicStateCodec<>() {
                @Override public Map<String, String> encode(Counter state) { return Map.of("value", Integer.toString(state.value())); }
                @Override public Counter decode(Map<String, String> fields) {
                    if (!fields.keySet().equals(Set.of("value"))) throw new IllegalArgumentException("Counter fields");
                    return new Counter(Integer.parseInt(fields.get("value")));
                }
            };
        }
        @Override public List<MechanicHandler<?>> handlers() {
            return List.of(handler(Increment.class, "counter.increment", ABILITY_PREPARE, null, (session, op) -> {
                increment(session, op.owner(), op.amount());
                return op.reject() ? AbilityRejection.INVALID_ABILITY_TARGET : Success.SUCCESS;
            }), handler(Kill.class, "counter.kill", ABILITY_PREPARE, "counter.increment",
                    (session, op) -> session.capture(op.owner(), op.victim())));
        }
        @Override public List<MechanicStep> steps() {
            return List.of(update("counter.end", END_TURN, null, session -> {
                long owner = session.pieces().values().stream().filter(p -> p.color() == session.side()
                        && p.position() != null).mapToLong(PieceState::id).min().orElseThrow();
                increment(session, owner, 1);
            }), step("counter.history", HISTORY, null, session -> session.history().stream()
                    .anyMatch(e -> e.turn() == session.turn() && e.isAbility(Ability.JOURNAL_REJECT))
                    ? AbilityRejection.INVALID_ABILITY_TARGET : Success.SUCCESS),
                update("counter.unstable", ENTRY, null, session -> {
                    if (session.request() != null && session.request().ability() == Ability.UNSTABLE)
                        session.state(KEY, new Counter(session.state(KEY).value() + 1));
                }));
        }
        @Override public void validate(GameSnapshot snapshot) {
            int last = snapshot.events().stream().filter(event -> event.isEffect(PULSE))
                    .map(event -> PULSE.type().cast(event.effect().data()).value()).reduce((first, second) -> second).orElse(0);
            if (last != snapshot.mechanics().get(KEY).value()) throw new IllegalArgumentException("Counter differs from history");
        }
    };
    private static void increment(MechanicSession session, long owner, int amount) {
        var next = new Counter(session.state(KEY).value() + amount);
        session.state(KEY, next);
        var piece = session.before().pieceById(owner);
        Position square = piece.position() == null ? new Position(0, 0) : piece.position();
        session.propose(new MechanicEventDraft(session.turn(), piece.color(), owner, null,
                square, square, "counter", PULSE.event(new Pulse(owner, next.value()))));
    }
    private static final MechanicKey<Integer> DERIVED = new MechanicKey<>("counter.moves", Integer.class, 0);
    private static final MechanicDefinition<Integer> DERIVED_MODULE = new MechanicDefinition<>() {
        @Override public MechanicKey<Integer> key() { return DERIVED; }
        @Override public List<MechanicDefinition<?>> dependencies() { return List.of(MODULE); }
        @Override public Integer restore(GameSnapshot snapshot) { return snapshot.moves().size(); }
        @Override public List<MechanicStep> steps() {
            return List.of(update("counter.countMove", MechanicPhase.AFTER_MOVE, null,
                    session -> session.state(DERIVED, session.state(DERIVED) + 1)));
        }
    };
    private static final HeroDefinition<NoHeroRules> DEFINITION = new HeroDefinition<>(HeroType.NONE, "Counter",
            Set.of(Rook.class), NoHeroRules.class, NoHeroRules.INSTANCE, null, CounterHero::new, List.of(DERIVED_MODULE));
    private static final class CounterHero extends AbstractHero {
        CounterHero(NoHeroRules rules) { super(DEFINITION, List.of(Ability.values()), null, 0); }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                PieceState target, AbilityRequest request) {
            Abilities ability = request.ability();
            return ability == Ability.KILL
                    ? HeroEffect.empty().withOperations(new Kill(actor.id(), target.id()))
                    : HeroEffect.empty().withOperations(new Increment(actor.id(), 1, ability == Ability.REJECT));
        }
        @Override public HeroEffect onCaptured(HeroActionContext context, PieceState victim, long killer, Position square) {
            return HeroEffect.empty().withOperations(new Increment(killer, context.state(KEY).value() + 2, square.equals(p("a3"))));
        }
    }
    private static HeroRules rules() {
        var definitions = new ArrayList<>(HeroDefinitions.all());
        definitions.removeIf(d -> d.type() == HeroType.NONE);
        definitions.add(DEFINITION);
        return HeroRules.defaults(definitions);
    }
    private static Game game() {
        return Game.fromMobaPosition(new GamePosition(new Field(8).getGameField(), FigureColor.WHITE,
                CastlingRights.initial(), null, 0), Map.of(), rules(), () -> 0L);
    }
    private static Position p(String cell) { return new Position(cell.charAt(1) - '1', cell.charAt(0) - 'a'); }
    private static Game restored(Game game) throws Exception {
        byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        Game loaded = Game.fromSnapshot(GameSaveFile.decode(bytes, rules()), () -> 0L);
        assertArrayEquals(bytes, GameSaveFile.encode(loaded.getSnapshot()));
        return loaded;
    }

    @Test public void registeredModuleExecutesPersistsItsOwnEventAndStateAndContinuesAfterLoading() throws Exception {
        Game game = game();
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), p("a1"), null, Ability.CAST));
        assertEquals(1, game.getSnapshot().mechanics().get(KEY).value());
        GameEvent event = game.getEvents().getLast();
        assertEquals(GameEvent.Kind.MECHANIC_EFFECT, event.kind());
        assertEquals(new Pulse(game.getPieceState(p("a1")).id(), 1), event.effect().data());
        String xml = new String(GameSaveFile.encode(game.getSnapshot()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("formatVersion=\"16\""));
        assertTrue(xml.contains("effectId=\"counter.pulse\""));
        assertTrue(xml.contains("<state id=\"counter\""));
        game = restored(game);
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), p("a2"), p("a3")));
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer()));
        assertEquals(2, game.getSnapshot().mechanics().get(KEY).value());
        game = restored(game);
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), p("a8"), null, Ability.CAST));
        assertEquals(3, game.getSnapshot().mechanics().get(KEY).value());
        restored(game);
    }

    @Test public void moduleDependencyRestoresDerivedStateWithoutDuplicatingItInXml() throws Exception {
        Game game = game();
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), p("a2"), p("a3")));
        assertEquals(Integer.valueOf(1), game.getSnapshot().mechanics().get(DERIVED));
        byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        assertFalse(new String(bytes, StandardCharsets.UTF_8).contains("<state id=\"counter.moves\""));
        Game loaded = restored(game);
        assertEquals(Integer.valueOf(1), loaded.getSnapshot().mechanics().get(DERIVED));
        assertEquals(Success.SUCCESS, loaded.endTurn(loaded.getCurrentPlayer()));
        restored(loaded);
    }

    @Test public void repeatedTargetPreviewsAndRejectedOperationOrHistoryPreserveAllPublishedState() throws Exception {
        for (Ability ability : List.of(Ability.REJECT, Ability.JOURNAL_REJECT)) {
            Game game = game();
            byte[] before = GameSaveFile.encode(game.getSnapshot());
            for (int i = 0; i < 3; i++) game.getAbilityTargets(p("a1"), Ability.CAST);
            assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
            assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,
                    game.useAbility(game.getCurrentPlayer(), p("a1"), null, ability));
            assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
            assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), p("a1"), null, Ability.CAST));
            assertEquals(1, game.getSnapshot().mechanics().get(KEY).value());
        }
    }

    @Test public void nonconvergingReactionIsBoundedAndDoesNotPublishAnyState() throws Exception {
        Game game = game();
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertEquals(MovementRejection.INVALID_BOARD,
                game.useAbility(game.getCurrentPlayer(), p("a1"), null, Ability.UNSTABLE));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
    }

    @Test public void operationFromDeathReactionUsesFullContextAndSurvivesSaveLoad() throws Exception {
        Game game = captureFixture("a2");
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), p("a8"), null, Ability.CAST));
        assertEquals(1, game.getSnapshot().mechanics().get(KEY).value());
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), p("a8"), p("a2")));
        assertEquals(4, game.getSnapshot().mechanics().get(KEY).value());
        assertTrue(game.getEvents().getLast().isEffect(PULSE));
        restored(game);
    }

    private static Game captureFixture(String victimSquare) {
        Field factory = new Field(8);
        Map<Position, Figure> board = new HashMap<>();
        for (Position square : factory.getGameField().keySet())
            board.put(square, factory.createFigure(model.figures.Empty.class, square, FigureColor.NONE));
        board.put(p("h1"), factory.createFigure(King.class, p("h1"), FigureColor.WHITE));
        board.put(p("h8"), factory.createFigure(King.class, p("h8"), FigureColor.BLACK));
        board.put(p(victimSquare), factory.createFigure(Rook.class, p(victimSquare), FigureColor.WHITE));
        board.put(p("a8"), factory.createFigure(Rook.class, p("a8"), FigureColor.BLACK));
        return Game.fromMobaPosition(new GamePosition(board, FigureColor.BLACK, CastlingRights.none(), null, 0),
                Map.of(), rules(), () -> 0L);
    }

    @Test public void failedDeathOperationRejectsTheEntireMoveWithoutPublishingItsEarlierChanges() throws Exception {
        Game game = captureFixture("a3");
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), p("a8"), null, Ability.CAST));
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,
                game.moveFigure(game.getCurrentPlayer(), p("a8"), p("a3")));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
        assertEquals(Integer.valueOf(0), game.getSnapshot().mechanics().get(DERIVED));
    }

    @Test public void newMechanicCaptureUsesCommonRewardsDeathReactionsAndPersistence() throws Exception {
        Game game = game();
        long victim = game.getPieceState(p("a7")).id();
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), p("a1"), p("a7"), Ability.KILL));
        assertEquals(PieceState.Status.CAPTURED, game.getPieceState(victim).status());
        assertEquals(2, game.getSnapshot().mechanics().get(KEY).value());
        assertEquals(GameEvent.Kind.CAPTURE, game.getEvents().getLast().kind());
        assertEquals(1, game.getWhitePlayer().getAttackedFigures().size());
        assertEquals(1, game.getEconomy().history().size());
        restored(game);
    }

    @Test public void invalidUnknownDuplicateOrPrematureStateAndEventDataAreRejected() throws Exception {
        Game game = game();
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), p("a1"), null, Ability.CAST));
        String xml = new String(GameSaveFile.encode(game.getSnapshot()), StandardCharsets.UTF_8);
        for (String invalid : List.of(xml.replace("effectId=\"counter.pulse\"", "effectId=\"unknown\""),
                xml.replace("id=\"counter\"", "id=\"unknown\""),
                xml.replace("formatVersion=\"16\"", "formatVersion=\"15\""),
                xml.replace("name=\"value\" value=\"1\"", "name=\"value\" value=\"-1\""),
                xml.replace("</mechanics>", "<state id=\"counter\"/></mechanics>"))) {
            assertThrows(IOException.class, () -> GameSaveFile.decode(invalid.getBytes(StandardCharsets.UTF_8), rules()));
        }
        assertThrows(IOException.class, () -> GameSaveFile.decode(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
