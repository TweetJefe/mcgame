package game.heroes;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.heroes.pudge.PudgeHero;
import game.heroes.pudge.PudgeRules;
import game.state.PieceState;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.Knight;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class HeroTargetingTest {
    @Test public void swallowRejectsIllusionsBeforePotAndBoxChecksWhileHookCanMovePots() {
        Field field = new Field(8);
        PieceState actor = PieceState.initial(1, field.getFigureByPosition(new Position(0, 0)), HeroType.PUDGE_ROOK);
        Position square = new Position(1, 0);
        PieceState illusion = PieceState.initial(2, field.createFigure(Knight.class, square, FigureColor.WHITE),
                HeroType.ILLUSION).asPot().withBox(4);
        HeroActionContext context = new HeroActionContext(field, Map.of(1L, actor, 2L, illusion), 1);
        var hero = new PudgeHero(PudgeRules.sandbox());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, hero.targeting(Ability.SWALLOW)
                .validate(context, actor, new AbilityRequest(actor.position(), Ability.SWALLOW, square)));
        assertEquals(StatusRejection.BOXED, hero.targeting(Ability.HOOK)
                .validate(context, actor, new AbilityRequest(actor.position(), Ability.HOOK, square)));
        PieceState unboxed = illusion.withBox(0);
        context = new HeroActionContext(field, Map.of(1L, actor, 2L, unboxed), 1);
        assertEquals(Success.SUCCESS, hero.targeting(Ability.HOOK)
                .validate(context, actor, new AbilityRequest(actor.position(), Ability.HOOK, square)));
    }

    @Test public void requestShapeValidationKeepsTheExistingErrorPrecedence() {
        Field field = new Field(8);
        PieceState actor = PieceState.initial(1, field.getFigureByPosition(new Position(0, 0)), HeroType.PUDGE_ROOK);
        HeroActionContext context = new HeroActionContext(field, Map.of(1L, actor), 1);
        HeroTargeting targeted = HeroTargeting.square();
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, targeted.validate(context, actor,
                new AbilityRequest(actor.position(), Ability.HOOK, null, new Position(1, 1), false)));
        assertEquals(MovementRejection.OUT_OF_BOUNDS, targeted.validate(context, actor,
                new AbilityRequest(actor.position(), Ability.HOOK, null)));
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, targeted.validate(context, actor,
                new AbilityRequest(actor.position(), Ability.HOOK, actor.position())));
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, HeroTargeting.untargeted().validate(context, actor,
                new AbilityRequest(actor.position(), Ability.HOOK, new Position(1, 1))));
    }
}
