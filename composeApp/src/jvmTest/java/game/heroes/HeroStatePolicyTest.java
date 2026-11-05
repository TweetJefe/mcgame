package game.heroes;

import game.core.Game;
import game.core.SnapshotValidator;
import game.heroes.pudge.PudgeDefinition;
import game.heroes.pudge.PudgeRules;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicPhase;
import game.mechanics.MechanicStep;
import game.mechanics.Mechanics;
import game.moves.Success;
import game.persistence.GameSaveFile;
import game.scenarios.PudgeScenario;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.ItemSlot;
import game.state.PieceState;
import game.state.SquareColor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Queen;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class HeroStatePolicyTest {
    private static final Field FIELD = new Field(8);

    @Test public void initializationUsesLocalStartingColorAndLeavesOtherHeroesWithoutIt() {
        var factory = new HeroPieceFactory(HeroRegistry.standard());
        for (var hero : List.of(HeroType.KIKIMORA, HeroType.LUMESHARD, HeroType.ARES, HeroType.GINSENG)) {
            PieceState piece = factory.initial(1, FIELD.createFigure(Bishop.class, pos("c2"), FigureColor.WHITE), hero);
            assertEquals(SquareColor.at(pos("c2")), piece.startingColor());
            assertEquals(piece, PieceState.initial(1, FIELD.createFigure(Bishop.class, pos("c2"), FigureColor.WHITE), hero));
        }
        assertNull(factory.initial(1, FIELD.createFigure(Rook.class, pos("a1"), FigureColor.WHITE), HeroType.PUDGE).startingColor());
    }

    @Test public void runtimeFormsRemainDifferentFromInitialDraftCompatibility() {
        var heroes = HeroRegistry.standard();
        assertFalse(heroes.get(HeroType.CHRONOMLIN).supports(Queen.class));
        heroes.validateState(piece(HeroType.CHRONOMLIN, Queen.class, null));
        heroes.validateState(piece(HeroType.BABA_YAGA, Rook.class, null));
        heroes.validateState(piece(HeroType.GINSENG, Rook.class, SquareColor.DARK));
        assertThrows(IllegalArgumentException.class,
                () -> heroes.validateState(piece(HeroType.GINSENG, Queen.class, SquareColor.DARK)));
        assertThrows(IllegalArgumentException.class,
                () -> heroes.validateState(piece(HeroType.ILLUSION, Queen.class, null)));
        assertThrows(IllegalArgumentException.class,
                () -> heroes.validateState(piece(HeroType.PUDGE, Knight.class, null)));
    }

    @Test public void specialFieldsAreLocalPermissionsAndDoNotChangePieceIdentity() {
        var heroes = HeroRegistry.standard();
        PieceState pudge = piece(HeroType.PUDGE, Rook.class, null).withAbilityState(0, 0, 2L);
        heroes.validateState(pudge);
        heroes.validateState(piece(HeroType.GINSENG, Bishop.class, SquareColor.DARK).hidden(PieceState.Status.HIDDEN_BOG));
        heroes.validateState(piece(HeroType.ILLUSION, Knight.class, null).hidden(PieceState.Status.DISSOLVED));
        assertThrows(IllegalArgumentException.class,
                () -> heroes.validateState(piece(HeroType.NONE, Rook.class, null).withAbilityState(0, 0, 2L)));
        assertThrows(IllegalArgumentException.class,
                () -> heroes.validateState(piece(HeroType.PUDGE, Rook.class, null).hidden(PieceState.Status.HIDDEN_BOG)));
        assertThrows(IllegalArgumentException.class,
                () -> heroes.validateState(piece(HeroType.NONE, Knight.class, null).hidden(PieceState.Status.DISSOLVED)));
        assertThrows(IllegalArgumentException.class,
                () -> pudge.withAbilityState(0, 0, pudge.id()));
    }

    @Test public void limitsComeFromThePartyRulesRatherThanTheStandardRegistry() {
        HeroRules high = HeroRules.defaults().with(PudgeDefinition.INSTANCE, new PudgeRules(7, 5, true, true));
        var configured = HeroRegistry.fromRules(high);
        PieceState piece = piece(HeroType.PUDGE, Rook.class, null).withAbilityState(6, 4, null);
        configured.validateState(piece);
        assertThrows(IllegalArgumentException.class, () -> HeroRegistry.standard().validateState(piece));
        assertThrows(IllegalArgumentException.class, () -> configured.validateState(piece.withAbilityState(8, 4, null)));
        assertThrows(IllegalArgumentException.class, () -> configured.validateState(piece.withAbilityState(6, 6, null)));
    }

    @Test public void originalSquareIsValidatedWithoutReinitializingMovedPieces() {
        var heroes = HeroRegistry.standard();
        PieceState piece = new HeroPieceFactory(heroes).initial(1,
                FIELD.createFigure(Bishop.class, pos("c2"), FigureColor.WHITE), HeroType.KIKIMORA);
        PieceState moved = piece.placed(pos("c3"), Bishop.class);
        heroes.validateState(moved);
        heroes.validateOrigin(moved, pos("c2"));
        assertEquals(piece.startingColor(), moved.startingColor());
        assertThrows(IllegalArgumentException.class, () -> heroes.validateOrigin(moved, pos("c3")));
    }

    @Test public void anExtensionInitializesFromItsSourceAndUsesItsOwnRuntimePolicy() {
        var heroes = HeroRegistry.fromRules(extensionRules());
        var factory = new HeroPieceFactory(heroes);
        PieceState original = factory.initial(1, FIELD.createFigure(Rook.class, pos("a1"), FigureColor.WHITE), HeroType.PUDGE);
        assertEquals(1, original.ultimateCharge());
        PieceState source = original.withAbilityState(0, 0, null).withBlindness(1).withStun(8).withBox(7);
        PieceState base = new PieceState(2, Queen.class, source.color(), pos("a3"), PieceState.Status.BOARD,
                source.hero(), 0, 0, source.inventory(), null, source.blindTurns(), null,
                source.stunnedThroughTurn(), source.boxedThroughTurn());
        PieceState created = factory.create(base, new PieceCreationContext(pos("a3"), source));
        assertEquals(base, created);
        assertEquals(1, original.ultimateCharge());
        assertEquals(0, source.ultimateCharge());
        assertEquals(8, created.stunnedThroughTurn());
        assertEquals(7, created.boxedThroughTurn());
        assertEquals(1, created.blindTurns());
        heroes.validateState(created);
    }

    @Test public void malformedStrategyOutputIsNeverPublished() throws Exception {
        Game game = Game.fromMobaPosition(new GamePosition(new Field(8).getGameField(), FigureColor.WHITE,
                CastlingRights.initial(), null, 0), Map.of(pos("a1"), HeroType.PUDGE), extensionRules(), () -> 0L);
        GameSnapshot before = game.getSnapshot();
        byte[] save = GameSaveFile.encode(before);
        assertThrows(IllegalArgumentException.class,
                () -> game.useAbility(game.getCurrentPlayer(), pos("a1"), null, TestAbility.INVALID_STATE));
        assertEquals(before, game.getSnapshot());
        assertArrayEquals(save, GameSaveFile.encode(game.getSnapshot()));
    }

    @Test public void malformedEndTurnOutputIsNeverPublished() {
        Game game = Game.fromMobaPosition(new GamePosition(new Field(8).getGameField(), FigureColor.WHITE,
                CastlingRights.initial(), null, 0), Map.of(pos("a1"), HeroType.PUDGE), extensionRules(), () -> 0L);
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("a2"), pos("a3")));
        GameSnapshot before = game.getSnapshot();
        assertThrows(IllegalArgumentException.class, () -> game.endTurn(game.getCurrentPlayer()));
        assertEquals(before, game.getSnapshot());
    }

    @Test public void initializerCannotChangeTheRequestedIdentityOrPlacement() {
        HeroDefinition<NoHeroRules> definition = new HeroDefinition<>(HeroType.PUDGE, "Initializer",
                Set.of(Rook.class), NoHeroRules.class, NoHeroRules.INSTANCE, null,
                rules -> { throw new AssertionError("Manual registry supplies the strategy"); })
                .withStatePolicy(new HeroStatePolicy<NoHeroRules>() {
                    @Override public PieceState initialize(PieceState base, PieceCreationContext origin, NoHeroRules rules) {
                        return base.placed(pos("b1"), base.type());
                    }
                    @Override public void validate(PieceState piece, NoHeroRules rules) {}
                });
        Hero strategy = new AbstractHero(definition, List.of(), null, 0) {
            @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                           PieceState target, AbilityRequest request) {
                return HeroEffect.empty();
            }
        };
        var heroes = new HeroRegistry(HeroRules.defaults(List.of(definition))).register(strategy);
        assertThrows(IllegalArgumentException.class, () -> new HeroPieceFactory(heroes).initial(1,
                FIELD.createFigure(Rook.class, pos("a1"), FigureColor.WHITE), HeroType.PUDGE));
    }

    @Test public void restoreAndXmlDecodeRejectHeroSpecificState() throws Exception {
        Game game = PudgeScenario.DIAGONAL_HOOK.createGame(() -> 0L);
        GameSnapshot original = game.getSnapshot();
        PieceState actor = original.pieceAt(pos("e2"));
        var pieces = new HashMap<>(original.pieces());
        pieces.put(actor.id(), actor.withStartingColor(SquareColor.DARK));
        GameSnapshot malformed = withPieces(original, pieces);
        assertThrows(IllegalArgumentException.class, () -> SnapshotValidator.validate(malformed));
        assertThrows(IllegalArgumentException.class, () -> Game.fromSnapshot(malformed, () -> 0L));
        assertThrows(IOException.class, () -> GameSaveFile.encode(malformed));
        String xml = new String(GameSaveFile.encode(original), StandardCharsets.UTF_8);
        var tag = Pattern.compile("<piece\\b[^>]*\\bid=\"" + actor.id() + "\"[^>]*>").matcher(xml);
        assertTrue(tag.find());
        String changed = xml.substring(0, tag.start()) + tag.group().replaceFirst("<piece", "<piece startingColor=\"DARK\"")
                + xml.substring(tag.end());
        assertThrows(IOException.class, () -> GameSaveFile.decode(changed.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(Game.fromSnapshot(original, () -> 0L));
    }

    private static PieceState piece(HeroType hero, Class<? extends Figure> body, SquareColor color) {
        return new PieceState(1, body, FigureColor.WHITE, pos("c2"), PieceState.Status.BOARD, hero,
                0, 0, List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), null, 0, color);
    }

    private static GameSnapshot withPieces(GameSnapshot s, Map<Long, PieceState> pieces) {
        return new GameSnapshot(s.version(), s.mode(), s.field(), pieces, s.turn(), s.active(), s.castlingRights(),
                s.enPassantTarget(), s.halfmoveClock(), s.repetitions(), s.clock(), s.outcome(), s.moves(), s.events(),
                s.whitePoints(), s.blackPoints(), s.whiteCaptures(), s.blackCaptures(), s.drawOffer(), s.heroRules(), s.draft(), s.mechanics());
    }

    private enum TestAbility implements Abilities {
        INVALID_STATE;
        @Override public String id() { return "test.policy.invalid"; }
        @Override public String getDisplayName() { return "Invalid state"; }
        @Override public HeroTargeting targeting() { return HeroTargeting.untargeted(); }
    }

    private static final MechanicDefinition<Integer> INVALID_END = new MechanicDefinition<>() {
        private final MechanicKey<Integer> key = new MechanicKey<>("test.policy.end", Integer.class, 0);
        @Override public MechanicKey<Integer> key() { return key; }
        @Override public List<MechanicStep> steps() {
            return List.of(Mechanics.update("test.policy.end", MechanicPhase.END_TURN, null, session -> {
                for (PieceState piece : session.pieces().values())
                    if (piece.hero() == HeroType.PUDGE)
                        session.update(Map.of(piece.id(), piece.withAbilityState(0, 2, null)));
            }));
        }
    };

    private static final HeroDefinition<NoHeroRules> EXTENSION = new HeroDefinition<>(HeroType.PUDGE,
            "Extension", Set.of(Rook.class), NoHeroRules.class, NoHeroRules.INSTANCE, null,
            ExtensionHero::new, List.of(INVALID_END))
            .withStatePolicy(new HeroStatePolicy<>() {
                @Override public PieceState initialize(PieceState base, PieceCreationContext origin, NoHeroRules rules) {
                    return base.withAbilityState(0, origin.source() == null ? 1 : origin.source().ultimateCharge(), null);
                }
                @Override public void validate(PieceState piece, NoHeroRules rules) {
                    HeroStateConstraints.basic().validate(piece);
                    HeroStateConstraints.validateResources(piece, 0, 1);
                }
            });

    private static final class ExtensionHero extends AbstractHero {
        ExtensionHero(NoHeroRules rules) { super(EXTENSION, List.of(TestAbility.INVALID_STATE), null, 0); }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                       PieceState target, AbilityRequest request) {
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(0, 2, null)));
        }
    }

    private static HeroRules extensionRules() {
        var definitions = new ArrayList<>(HeroDefinitions.all());
        definitions.removeIf(d -> d.type() == EXTENSION.type());
        definitions.add(EXTENSION);
        return HeroRules.defaults(definitions);
    }
}
