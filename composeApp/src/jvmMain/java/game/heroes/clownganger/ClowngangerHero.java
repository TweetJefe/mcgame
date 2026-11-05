package game.heroes.clownganger;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityEventDetails;
import game.heroes.AbilityRequest;
import game.heroes.AbilityUse;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Predicate;
import model.Field;
import model.Position;
import model.figures.Empty;

/** Геометрия коробки и иллюзии; жизненным циклом двойника управляет ClownMechanic. */
public final class ClowngangerHero extends AbstractHero implements LegacyPreview {
    private final ClowngangerRules rules;

    public ClowngangerHero(ClowngangerRules rules) {
        super(ClowngangerDefinition.INSTANCE, ClowngangerAbility.getAbilities(), ClowngangerAbility.ILLUSION,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                                    AbilityRequest request) {
        HeroEffect effect = calculate(square -> context.pieceAt(square) == null, actor, target, request.target(),
                request.ability(), context.currentTurn());
        return effect.result().isSuccess() && request.ability() == ClowngangerAbility.ILLUSION
                ? effect.withOperations(new ClownMechanic.Create(actor, request.target())) : effect;
    }

    HeroEffect detachedPreview(Field field, PieceState actor, PieceState target, Position square,
                               Abilities ability, long turn, boolean borrowed) {
        return calculate(position -> field.getFigureByPosition(position) instanceof Empty, actor, target, square, ability, turn)
                .applyCost(actor, AbilityUse.of(HeroType.CLOWNGANGER, borrowed));
    }

    private HeroEffect calculate(Predicate<Position> vacant, PieceState actor, PieceState target, Position square,
                                 Abilities ability, long turn) {
        var updates = new HashMap<Long, PieceState>();
        updates.put(actor.id(), actor);
        if (ability == ClowngangerAbility.BOX) {
            if (target == null || target.color() != actor.color())
                return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            if (distance(actor.position(), square) > 3)
                return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
            updates.put(target.id(), target.withBox(Math.addExact(turn, 2L * rules.boxTurns())));
            return HeroEffect.success(updates).withCost(AbilityCost.cooldown(rules.boxCooldown()));
        }
        if (ability != ClowngangerAbility.ILLUSION) return HeroEffect.rejected(AbilityRejection.NO_ABILITY);
        if (distance(actor.position(), square) != 1 || !vacant.test(square))
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        return HeroEffect.success(updates).withCost(AbilityCost.ultimate(ultimateCaptures()));
    }

    @Override public AbilityEventDetails abilityEventDetails(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        if (request.ability() != ClowngangerAbility.ILLUSION)
            return super.abilityEventDetails(before, after, actor, target, request);
        var born = after.clown().illusions().values().stream()
                .filter(illusion -> illusion.sourceId() == actor.id() && illusion.bornTurn() == after.currentTurn())
                .findFirst().orElseThrow();
        return new AbilityEventDetails(born.id(), born.bornAt(), null);
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                          PieceState target, AbilityRequest request) {
        return request.ability() == ClowngangerAbility.BOX
                ? "Clownganger: коробка на " + cell(request.target())
                : "Clownganger: двойник на " + cell(request.target());
    }

    private static int distance(Position a, Position b) {
        return Math.max(Math.abs(a.getRow() - b.getRow()), Math.abs(a.getColumn() - b.getColumn()));
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
