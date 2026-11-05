package game.draft;

import game.heroes.DraftProfile;
import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.AbilityRequest;
import game.state.PieceState;
import game.heroes.NoHeroRules;
import model.figures.Queen;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static game.core.Positions.pos;
import static model.FigureColor.BLACK;
import static model.FigureColor.WHITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DraftCatalogTest {
    /** A new test description, not present in any historical catalog or placement dispatcher. */
    public static final HeroDefinition<NoHeroRules> EXTENSION = new HeroDefinition<>(HeroType.ILLUSION,
            "Independent draft entrant", Set.of(Queen.class), NoHeroRules.class, NoHeroRules.INSTANCE, null,
            ExtraFigure::new)
            .withDraftProfile(DraftProfile.pickable(50));

    private static final class ExtraFigure extends AbstractHero {
        ExtraFigure(NoHeroRules rules) { super(EXTENSION, List.of(), null, 0); }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                        PieceState target, AbilityRequest request) {
            return HeroEffect.empty();
        }
    }

    public static HeroRules extendedRules() {
        var definitions = new ArrayList<>(HeroDefinitions.all());
        definitions.removeIf(d -> d.type() == HeroType.ILLUSION);
        definitions.add(EXTENSION);
        return HeroRules.defaults(definitions);
    }

    @Test public void registeredProfilesDriveRosterOrderAndStartingBodiesWithoutNewBranches() {
        var draft = new Draft(DraftRules.prototype(), extendedRules());
        assertEquals(14, draft.getSnapshot().catalog().heroes().size());
        assertEquals(HeroType.ILLUSION, draft.getSnapshot().catalog().heroes().getLast());
        assertEquals(List.of(pos("d1")), draft.getSnapshot().catalog().startingSquares(HeroType.ILLUSION, WHITE));
        assertEquals(List.of(pos("d8")), draft.getSnapshot().catalog().startingSquares(HeroType.ILLUSION, BLACK));
        assertSame(EXTENSION, draft.getSnapshot().catalog().definition(HeroType.ILLUSION));
        assertEquals(DraftActionResult.SUCCESS, draft.ban(WHITE, HeroType.PUDGE));
        assertEquals(DraftActionResult.SUCCESS, draft.ban(BLACK, HeroType.LUMESHARD));
        assertEquals(List.of(pos("d1")), draft.getPlacementTargets(WHITE, HeroType.ILLUSION));
        assertEquals(DraftActionResult.SUCCESS, draft.pick(WHITE, HeroType.ILLUSION, pos("d1")));
        assertEquals(draft.getSnapshot(), Draft.fromSnapshot(draft.getSnapshot()).getSnapshot());
        var configuration = extendedRules();
        assertSame(configuration, Draft.fromSnapshot(draft.getSnapshot(), configuration).getHeroRules());
        assertThrows(IllegalArgumentException.class, () -> Draft.fromSnapshot(draft.getSnapshot(), HeroRules.prototype()));
    }

    @Test public void anExistingDraftKeepsItsRosterWhenTheRegisteredPoolGrows() {
        var draft = new Draft();
        var saved = draft.getSnapshot();
        var expanded = new Draft(DraftRules.prototype(), extendedRules());
        assertTrue(expanded.getSnapshot().catalog().heroes().contains(HeroType.ILLUSION));
        assertFalse(saved.catalog().heroes().contains(HeroType.ILLUSION));
        assertEquals(saved, Draft.fromSnapshot(saved).getSnapshot());
        assertThrows(UnsupportedOperationException.class, () -> saved.catalog().heroes().clear());
        assertThrows(UnsupportedOperationException.class, () -> saved.catalog().definitions().clear());
        assertThrows(IllegalArgumentException.class, () -> DraftCatalog.restore(
                List.of(HeroType.PUDGE, HeroType.PUDGE), HeroDefinitions.all()));
    }

    @Test public void historicalCatalogsStayFrozenAndDefaultNewCatalogPreservesTheCurrentRoster() {
        assertEquals(List.of(HeroType.PUDGE, HeroType.LUMESHARD, HeroType.BOUNTY_CROW, HeroType.BOLOTNIK),
                HeroCatalog.heroes(1));
        assertEquals(List.of(HeroType.PUDGE, HeroType.LUMESHARD, HeroType.BOUNTY_CROW, HeroType.BOLOTNIK,
                HeroType.IZBA, HeroType.GINSENG, HeroType.FAUN, HeroType.ARES, HeroType.CLOWNGANGER,
                HeroType.MOLEMINER, HeroType.ZEUS, HeroType.CHRONOMLIN, HeroType.KIKIMORA), HeroCatalog.heroes(10));
        assertEquals(HeroCatalog.heroes(10), new Draft().getSnapshot().catalog().heroes());
        assertFalse(HeroCatalog.heroes(10).contains(HeroType.NONE));
        assertFalse(HeroCatalog.heroes().contains(HeroType.BABA_YAGA));
        assertFalse(HeroCatalog.heroes().contains(HeroType.ILLUSION));
    }

    @Test public void currentCatalogLimitsAreCheckedAgainstItsActualFrozenComposition() {
        var rules = new DraftRules(1, 6);
        var small = new DraftCatalog(HeroDefinitions.all().subList(0, 2));
        assertThrows(IllegalArgumentException.class, () -> new Draft(rules, small));
        var noBans = new Draft(new DraftRules(0, 1), small);
        assertEquals(DraftPhase.PICKS, noBans.getSnapshot().phase());
    }
}
