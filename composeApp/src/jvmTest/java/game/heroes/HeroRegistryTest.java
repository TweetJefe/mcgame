package game.heroes;

import game.heroes.pudge.PudgeAbility;
import game.heroes.lumeshard.LumeshardAbility;

import game.moves.AbilityRejection;
import game.moves.Success;
import game.heroes.pudge.PudgeHero;
import game.heroes.pudge.PudgeRules;
import game.state.PieceState;
import model.Field;
import model.Position;
import model.figures.Bishop;
import model.figures.Rook;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class HeroRegistryTest {
    @Test public void factoryReusesOneStrategyPerTypeAndIncludesPassiveFigures() {
        HeroRegistry heroes = HeroRegistry.fromRules(HeroRules.prototype());
        assertTrue(heroes.get(HeroType.PUDGE) instanceof PudgeHero);
        for (HeroType type : HeroType.values()) {
            assertEquals(type, heroes.get(type).type());
            assertSame(heroes.get(type), heroes.get(type));
        }
        assertTrue(heroes.get(HeroType.PUDGE).supports(Rook.class));
        assertFalse(heroes.get(HeroType.PUDGE).supports(Bishop.class));
        assertTrue(heroes.get(HeroType.NONE).abilities().isEmpty());
        assertSame(heroes.get(HeroType.PUDGE), heroes.ownerOf(PudgeAbility.SWALLOW));
    }

    @Test public void rulesBelongToTheGameAndDoNotLeakBetweenRegistries() {
        var free = HeroRegistry.fromRules(HeroRules.withPudge(PudgeRules.sandbox()));
        var charged = HeroRegistry.fromRules(HeroRules.withPudge(new PudgeRules(4, 3, true, false)));
        Field field = new Field(8);
        PieceState actor = PieceState.initial(1, field.getFigureByPosition(new Position(0, 0)), HeroType.PUDGE);
        assertEquals(Success.SUCCESS, free.get(actor.hero()).availability(actor, PudgeAbility.SWALLOW));
        assertEquals(AbilityRejection.ULTIMATE_NOT_READY, charged.get(actor.hero()).availability(actor, PudgeAbility.SWALLOW));
        assertSame(actor, free.get(actor.hero()).afterCapture(actor));
        assertEquals(1, charged.get(actor.hero()).afterCapture(actor).ultimateCharge());
    }

    @Test public void registrationIsImmutableAndRejectsMissingOrAmbiguousEntries() {
        var empty = new HeroRegistry();
        var pudge = new PudgeHero(PudgeRules.prototype());
        HeroRegistry registered = empty.register(pudge);
        assertThrows(IllegalArgumentException.class, () -> empty.get(HeroType.PUDGE));
        assertSame(pudge, registered.get(HeroType.PUDGE));
        assertThrows(IllegalArgumentException.class, () -> registered.register(pudge));
        assertThrows(IllegalArgumentException.class, () -> registered.get(HeroType.ZEUS));
        assertThrows(IllegalArgumentException.class, () -> registered.ownerOf(LumeshardAbility.LASER));
        assertThrows(NullPointerException.class, () -> registered.register(null));
        Hero conflicting = new AbstractHero(HeroType.ZEUS, List.of(PudgeAbility.HOOK), null, 0) {
            @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                            PieceState target, Position square, Abilities ability) {
                return HeroEffect.empty();
            }
        };
        assertThrows(IllegalArgumentException.class, () -> registered.register(conflicting));
    }
}
