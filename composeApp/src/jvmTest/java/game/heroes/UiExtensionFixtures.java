package game.heroes;

import game.core.Game;
import game.history.AbilityEventTarget;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.PieceState;
import model.Field;
import model.FigureColor;
import model.figures.Rook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static game.core.Positions.pos;

public final class UiExtensionFixtures {
    private UiExtensionFixtures() {}
    public enum Spell implements Abilities {
        CAST, SECOND, THIRD;
        @Override public String id() { return "ui.extension." + name(); }
        @Override public String getDisplayName() { return "Новая " + name(); }
        @Override public HeroTargeting targeting() { return HeroTargeting.untargeted(); }
        @Override public AbilityEventTarget eventTarget() { return AbilityEventTarget.NONE; }
    }
    public static final HeroDefinition<NoHeroRules> DEFINITION = new HeroDefinition<>(HeroType.PUDGE,
            "Новый герой", Set.of(Rook.class), NoHeroRules.class, NoHeroRules.INSTANCE, null, Extension::new)
            .withDraftProfile(DraftProfile.pickable(1));
    private static final class Extension extends AbstractHero {
        Extension(NoHeroRules rules) { super(DEFINITION, Abilities.getAbilities(Spell.values()), null, 0); }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                        PieceState target, AbilityRequest request) {
            return HeroEffect.success(Map.of(actor.id(), actor.withBlindness(1)))
                    .withCost(AbilityCost.cooldown(2));
        }
    }
    public static Game game() {
        var definitions = new ArrayList<>(HeroDefinitions.all());
        definitions.removeIf(d -> d.type() == HeroType.PUDGE);
        definitions.add(DEFINITION);
        return Game.fromMobaPosition(new GamePosition(new Field(8).getGameField(), FigureColor.WHITE,
                CastlingRights.initial(), null, 0), Map.of(pos("a1"), HeroType.PUDGE),
                HeroRules.defaults(definitions), () -> 0L);
    }
}
