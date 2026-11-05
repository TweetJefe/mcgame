package game.heroes.kikimora;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.compat.LegacyPreview;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.state.PieceState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.King;

/** Геометрия полёта пряжи и превращение цели в горшок. */
public final class KikimoraHero extends AbstractHero implements LegacyPreview {
    private final KikimoraRules rules;

    public KikimoraHero(KikimoraRules rules) {
        super(KikimoraDefinition.INSTANCE, KikimoraAbility.getAbilities(), KikimoraAbility.CLAY_POT,
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
        if (target == null || target.position() == null || target.id() == actor.id())
            return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        if (target.type() == King.class) return HeroEffect.rejected(StatusRejection.KING_IMMUNE);
        if (target.isPotted()) return HeroEffect.rejected(StatusRejection.POTTED);
        if (target.isBoxed()) return HeroEffect.rejected(StatusRejection.BOXED);
        int distance = Math.max(Math.abs(target.position().getRow() - actor.position().getRow()),
                Math.abs(target.position().getColumn() - actor.position().getColumn()));
        if (distance > (request.ability() == KikimoraAbility.YARN ? 2 : 1))
            return HeroEffect.rejected(AbilityRejection.ABILITY_OUT_OF_RANGE);
        var updates = new HashMap<Long, PieceState>();
        updates.put(actor.id(), actor);
        if (request.ability() == KikimoraAbility.CLAY_POT) {
            updates.put(target.id(), target.asPot());
            return HeroEffect.success(updates).withCost(AbilityCost.ultimate(ultimateCaptures()));
        }
        if (target.color() == actor.color()) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        List<PieceState> targets = yarnTargets(context.pieces(), actor, target);
        int duration = targets.size() == 2 ? 2 : 1;
        for (PieceState piece : targets)
            updates.put(piece.id(), piece.withStun(Math.max(piece.stunnedThroughTurn(),
                    stunDeadline(context.currentTurn(), actor.color(), piece.color(), duration))));
        return HeroEffect.success(updates).withCost(AbilityCost.cooldown(rules.yarnCooldown()));
    }

    /** Точный решётчатый луч продолжает полёт, включая направления с наклоном (1, 2). */
    static List<PieceState> yarnTargets(Map<Long, PieceState> pieces, PieceState actor, PieceState first) {
        var result = new ArrayList<PieceState>();
        result.add(first);
        int dr = first.position().getRow() - actor.position().getRow();
        int dc = first.position().getColumn() - actor.position().getColumn();
        int divisor = gcd(Math.abs(dr), Math.abs(dc));
        int rowStep = dr / divisor, columnStep = dc / divisor;
        for (int n = 1; n * Math.max(Math.abs(rowStep), Math.abs(columnStep)) <= 2; n++) {
            Position square = new Position(first.position().getRow() + rowStep * n,
                    first.position().getColumn() + columnStep * n);
            if (square.getRow() < 0 || square.getRow() > 7 || square.getColumn() < 0 || square.getColumn() > 7) break;
            PieceState next = pieces.values().stream().filter(piece -> square.equals(piece.position())).findFirst().orElse(null);
            if (next != null) {
                if (next.type() != King.class && !next.isBoxed() && !next.isPotted()) result.add(next);
                break;
            }
        }
        return List.copyOf(result);
    }

    static long stunDeadline(long turn, FigureColor caster, FigureColor owner, int turns) {
        return Math.addExact(turn, 2L * turns - (caster == owner ? 0 : 1));
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    @Override
    public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                  PieceState target, AbilityRequest request) {
        return request.ability() == KikimoraAbility.YARN
                ? "Kikimora: пряжа на " + cell(request.target()) : "Kikimora: глиняный горшок на " + cell(request.target());
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
