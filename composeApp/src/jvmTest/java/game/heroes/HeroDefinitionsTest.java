package game.heroes;

import game.heroes.bolotnik.BolotnikDefinition;
import game.heroes.bolotnik.BolotnikRules;
import game.heroes.bountycrow.BountyCrowDefinition;
import game.heroes.bountycrow.BountyCrowRules;
import game.heroes.lumeshard.LumeshardDefinition;
import game.heroes.lumeshard.LumeshardRules;
import game.heroes.pudge.PudgeDefinition;
import game.heroes.pudge.PudgeRules;
import game.persistence.HeroRulesXml;
import game.state.PieceState;
import model.Field;
import model.Position;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class HeroDefinitionsTest {
    private record TestRules(int cooldown) {}
    private enum TestAbility implements Abilities {
        CAST;
        @Override public String id() { return "TEST_ONLY"; }
        @Override public String getDisplayName() { return "Test cast"; }
    }
    private static final HeroRulesCodec<TestRules> CODEC = new HeroRulesCodec<>() {
        @Override public String section() { return "testRules"; }
        @Override public int sinceVersion() { return 1; }
        @Override public Map<String, String> encode(TestRules rules) {
            return Map.of("cooldown", String.valueOf(rules.cooldown()));
        }
        @Override public TestRules decode(HeroRuleAttributes attributes) {
            return new TestRules(attributes.integer("cooldown"));
        }
    };
    private static final HeroDefinition<TestRules> TEST = new HeroDefinition<>(
            HeroType.NONE, "Independent test hero", Set.of(Bishop.class),
            TestRules.class, new TestRules(2), CODEC, TestHero::new);

    private static final class TestHero extends AbstractHero {
        private final TestRules rules;
        TestHero(TestRules rules) {
            super(TEST, List.of(TestAbility.CAST), null, 0);
            this.rules = rules;
        }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                        PieceState target, Position square, Abilities ability) {
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(rules.cooldown(), 0, null)));
        }
    }

    @Test public void descriptionsCoverAllIdentitiesAndPreserveInitialBodies() {
        Set<HeroType> registered = new HashSet<>();
        for (HeroDefinition<?> definition : HeroDefinitions.all()) {
            assertTrue(registered.add(definition.type()));
            assertSame(definition, HeroDefinitions.get(definition.type()));
            assertFalse(definition.displayName().isBlank());
            assertFalse(definition.supports(null));
        }
        assertEquals(Set.of(HeroType.values()), registered);
        assertEquals("Пудж-Ладья", PudgeDefinition.INSTANCE.displayName());
        assertEquals(Set.of(Rook.class), PudgeDefinition.INSTANCE.supportedBodies());
        assertEquals(Set.of(Pawn.class), HeroDefinitions.get(HeroType.BABA_YAGA).supportedBodies());
        assertEquals(Set.of(Bishop.class, Knight.class, Rook.class),
                HeroDefinitions.get(HeroType.ILLUSION).supportedBodies());
        assertTrue(HeroDefinitions.get(HeroType.NONE).supportedBodies().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> PudgeDefinition.INSTANCE.supportedBodies().clear());
    }

    @Test public void defaultsAndOverridesDoNotInstantiateOrMutateStrategies() {
        AtomicInteger creations = new AtomicInteger();
        HeroDefinition<TestRules> definition = new HeroDefinition<>(HeroType.ZEUS,
                "Metadata only", Set.of(Rook.class), TestRules.class, new TestRules(5), CODEC,
                parameters -> { creations.incrementAndGet(); return new TestHero(parameters); });
        var input = new ArrayList<HeroDefinition<?>>(List.of(definition));
        HeroRules defaults = HeroRules.defaults(input);
        input.clear();
        HeroRules changed = defaults.with(definition, new TestRules(9));
        assertEquals(0, creations.get());
        assertEquals(new TestRules(5), defaults.get(definition));
        assertEquals(new TestRules(9), changed.get(definition));
        assertEquals(List.of(definition), defaults.definitions());
        assertThrows(UnsupportedOperationException.class, () -> defaults.definitions().clear());
    }

    @Test public void independentDefinitionDrivesFactoryBodiesRulesAndWireSchema() throws Exception {
        HeroRules defaults = HeroRules.defaults(List.of(TEST));
        HeroRules configuration = defaults.with(TEST, new TestRules(7));
        HeroRegistry registry = HeroRegistry.fromRules(configuration);
        Hero strategy = registry.get(TEST.type());
        assertSame(TEST, strategy.definition());
        assertTrue(strategy.supports(Bishop.class));
        assertFalse(strategy.supports(Rook.class));
        assertSame(strategy, registry.ownerOf(TestAbility.CAST));
        Field field = new Field(8);
        Position square = new Position(0, 2);
        PieceState actor = PieceState.initial(1, field.getFigureByPosition(square), TEST.type());
        var context = new HeroActionContext(field, Map.of(actor.id(), actor), 1);
        assertEquals(7, strategy.preview(context, actor, null, null, TestAbility.CAST)
                .updates().get(actor.id()).cooldown());
        assertEquals(0, actor.cooldown());
        assertEquals(2, ((TestHero) HeroRegistry.fromRules(defaults).get(TEST.type())).rules.cooldown());

        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        var root = document.createElement("moba-chess");
        document.appendChild(root);
        root.appendChild(document.createElement("castling"));
        HeroRulesXml.write(root, configuration, 1);
        assertEquals("testRules", root.getFirstChild().getNodeName());
        assertEquals(configuration, HeroRulesXml.read(root, 1, defaults));
        assertEquals(Set.of("testRules"), HeroRulesXml.sections(configuration));
    }

    @Test @SuppressWarnings({"rawtypes", "unchecked"})
    public void invalidConfigurationsFailBeforeAFactoryCanUseThem() {
        HeroRules configuration = HeroRules.defaults(List.of(TEST));
        assertThrows(IllegalArgumentException.class, () -> configuration.with((HeroDefinition) TEST, "wrong type"));
        assertThrows(NullPointerException.class, () -> configuration.with(TEST, null));
        assertThrows(IllegalArgumentException.class, () -> configuration.get(PudgeDefinition.INSTANCE));
        assertThrows(IllegalArgumentException.class, () -> HeroRules.defaults(List.of(TEST, TEST)));
        HeroDefinition<TestRules> duplicateSection = new HeroDefinition<>(HeroType.PUDGE,
                "Duplicate schema", Set.of(Rook.class), TestRules.class, new TestRules(1), CODEC, TestHero::new);
        assertThrows(IllegalArgumentException.class,
                () -> HeroRules.defaults(List.of(TEST, duplicateSection)));
        HeroDefinition<TestRules> wrongFactory = new HeroDefinition<>(HeroType.PUDGE,
                "Wrong factory", Set.of(Rook.class), TestRules.class, new TestRules(1), CODEC, TestHero::new);
        assertThrows(IllegalArgumentException.class,
                () -> HeroRegistry.fromRules(HeroRules.defaults(List.of(wrongFactory))));
    }

    @Test public void existingSandboxProfileAndLegacyConstructorsRemainEquivalent() {
        HeroRules defaults = HeroRules.prototype();
        HeroRules sandbox = HeroRules.sandbox();
        assertEquals(PudgeRules.sandbox(), sandbox.get(PudgeDefinition.INSTANCE));
        assertEquals(LumeshardRules.sandbox(), sandbox.get(LumeshardDefinition.INSTANCE));
        assertEquals(BountyCrowRules.sandbox(), sandbox.get(BountyCrowDefinition.INSTANCE));
        assertEquals(BolotnikRules.sandbox(), sandbox.get(BolotnikDefinition.INSTANCE));
        for (HeroDefinition<?> definition : defaults.definitions())
            if (!Set.of(HeroType.PUDGE, HeroType.LUMESHARD,
                    HeroType.BOUNTY_CROW, HeroType.BOLOTNIK).contains(definition.type()))
                assertEquals(get(defaults, definition), get(sandbox, definition));
        assertEquals(defaults, new HeroRules(PudgeRules.prototype(), LumeshardRules.prototype()));
        assertEquals(defaults.hashCode(), new HeroRules(PudgeRules.prototype(), LumeshardRules.prototype()).hashCode());
    }

    private static <R> R get(HeroRules rules, HeroDefinition<R> definition) { return rules.get(definition); }
}
