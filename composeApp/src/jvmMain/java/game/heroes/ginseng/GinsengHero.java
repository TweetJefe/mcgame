package game.heroes.ginseng;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityEventDetails;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroDefinitions;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import model.Field;
import model.Position;
import model.figures.King;
import rules.standard.MoveValidator;

/** Сдвиг и получение копии; исполнение сохранённой копии остаётся общим путём реестра. */
public final class GinsengHero extends AbstractHero implements LegacyPreview {
    private final GinsengRules rules;

    public GinsengHero(GinsengRules rules) {
        super(GinsengDefinition.INSTANCE, GinsengAbility.getAbilities(), GinsengAbility.COPY_SPELL,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    /** Совместимость ограниченного статического API; игровой путь использует полный AbilityRequest. */
    @Override
    public HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square,
                                    Abilities ability) {
        return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
    }

    @Override
    protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                           AbilityRequest request) {
        return request.ability() == GinsengAbility.DISPLACE
                ? displaceAvailable(actor, target, request.destination(), square -> context.pieceAt(square) == null)
                : copyAvailable(actor, target, target == null ? null : context.spells().lastUsed().get(target.id()));
    }

    HeroEffect displaceAvailable(PieceState actor, PieceState target, Position destination, Predicate<Position> empty) {
        if (target == null) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
        if (distance(actor.position(), target.position()) > 3)
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        if (destination == null || destination.getRow() < 0 || destination.getRow() > 7
                || destination.getColumn() < 0 || destination.getColumn() > 7)
            return HeroEffect.rejected(MovementRejection.OUT_OF_BOUNDS);
        if (distance(target.position(), destination) != 1 || !empty.test(destination))
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        var updates = new HashMap<Long, PieceState>();
        updates.put(actor.id(), actor);
        updates.put(target.id(), (target.id() == actor.id() ? actor : target).placed(destination, target.type()));
        return HeroEffect.success(updates).withCost(AbilityCost.cooldown(rules.displacementCooldown()));
    }

    HeroEffect copyAvailable(PieceState actor, PieceState source, Abilities last) {
        if (source == null || source.hero() == HeroType.NONE || source.type() == King.class)
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (last == null && source.hero() != HeroType.ILLUSION)
            return HeroEffect.rejected(AbilityRejection.NO_USED_ABILITY);
        return HeroEffect.success(Map.of(actor.id(), actor)).withCost(AbilityCost.ultimate(ultimateCaptures()));
    }

    @Override
    public List<AbilityRequest> requests(HeroActionContext context, PieceState actor, Abilities ability, boolean borrowed) {
        if (ability == GinsengAbility.COPY_SPELL)
            return super.requests(context, actor, ability, borrowed).stream().filter(request -> {
                PieceState source = context.pieceAt(request.target());
                return source != null && (source.hero() == HeroType.ILLUSION
                        || context.spells().lastUsed().containsKey(source.id()));
            }).toList();
        if (ability != GinsengAbility.DISPLACE) return super.requests(context, actor, ability, borrowed);
        var result = new ArrayList<AbilityRequest>();
        for (PieceState target : context.pieces().values()) {
            if (target.position() == null || target.type() == King.class
                    || distance(target.position(), actor.position()) > 3) continue;
            for (int dr = -1; dr <= 1; dr++)
                for (int dc = -1; dc <= 1; dc++) {
                    Position to = new Position(target.position().getRow() + dr, target.position().getColumn() + dc);
                    if ((dr != 0 || dc != 0) && MoveValidator.inside(context.field(), to) && context.pieceAt(to) == null)
                        result.add(new AbilityRequest(actor.position(), ability, target.position(), to, borrowed));
                }
        }
        return List.copyOf(result);
    }

    @Override
    public AbilityEventDetails abilityEventDetails(HeroActionContext before, HeroActionContext after,
                                                    PieceState actor, PieceState target, AbilityRequest request) {
        var details = super.abilityEventDetails(before, after, actor, target, request);
        return new AbilityEventDetails(details.targetId(), details.to(), request.ability() == GinsengAbility.COPY_SPELL
                ? before.spells().lastUsed().get(target.id()) : null);
    }

    @Override
    public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                  PieceState target, AbilityRequest request) {
        if (request.ability() == GinsengAbility.COPY_SPELL)
            return target.hero() == HeroType.ILLUSION ? "Ginseng: потратил ульту на пустышку"
                    : "Ginseng: скопировал " + before.spells().lastUsed().get(target.id()).getDisplayName()
                    + " у " + HeroDefinitions.get(target.hero()).displayName();
        return "Ginseng: сдвиг " + cell(request.target()) + " → " + cell(request.destination());
    }

    private static int distance(Position a, Position b) {
        return Math.max(Math.abs(a.getRow() - b.getRow()), Math.abs(a.getColumn() - b.getColumn()));
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
