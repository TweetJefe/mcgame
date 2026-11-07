package game.heroes.zeus;

import game.moves.StatusRejection;

import game.mechanics.PieceRestriction;

import game.heroes.Abilities;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.AbstractHero;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.mechanics.MechanicSession;
import game.moves.AbilityRejection;
import game.moves.Capture;
import game.moves.MoveResult;
import game.moves.Success;
import game.state.PieceState;
import game.summoning.SummonState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import model.Field;
import model.Position;
import model.figures.King;

/** Геометрия, цепочка и эффекты молнии; состояние и применение обслуживает LightningMechanic. */
public final class ZeusHero extends AbstractHero {
    private final ZeusRules rules;

    public ZeusHero(ZeusRules rules) {
        super(ZeusDefinition.INSTANCE, ZeusAbility.getAbilities(), ZeusAbility.THUNDERSTORM,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }

    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target,
                                                    AbilityRequest request) {
        if (target != null && context.blocks(target, PieceRestriction.CAPTURE))
            return HeroEffect.rejected(StatusRejection.PROTECTED_OBSTACLE);
        MoveResult result = validateFirst(actor, target, request.ability());
        if (!result.isSuccess()) return HeroEffect.rejected(result);
        return HeroEffect.empty().withCost(cost(request.ability()))
                .withOperations(new LightningMechanic.Cast(actor, target, request.ability(), request.borrowed()));
    }

    private AbilityCost cost(Abilities ability) {
        return ability == ZeusAbility.CHAIN_LIGHTNING ? AbilityCost.cooldown(rules.lightningCooldown())
                : AbilityCost.ultimate(ultimateCaptures());
    }

    private static MoveResult validateFirst(PieceState actor, PieceState target, Abilities ability) {
        if (ability == ZeusAbility.CHAIN_LIGHTNING) {
            if (target == null || !eligible(target, actor)) return AbilityRejection.INVALID_ABILITY_TARGET;
            if (distance(actor.position(), target.position()) > 3) return AbilityRejection.ABILITY_OUT_OF_RANGE;
            return Success.SUCCESS;
        }
        return ability == ZeusAbility.THUNDERSTORM ? Success.SUCCESS : AbilityRejection.NO_ABILITY;
    }

    public static List<PieceState> charged(Map<Long, PieceState> pieces, LightningState lightning, PieceState actor) {
        return pieces.values().stream().filter(piece -> eligible(piece, actor) && lightning.count(actor.id(), piece.id()) > 0)
                .sorted(Comparator.comparingLong(PieceState::id)).toList();
    }

    private static boolean eligible(PieceState piece, PieceState actor) {
        return piece.position() != null && piece.color() != actor.color() && piece.type() != King.class
                && !piece.isBoxed() && !piece.isPotted();
    }

    record CastResult(HeroEffect effect, LightningState lightning, SummonState summons, List<Long> illusionHits) {}

    /** Исполнитель уже оплатил активацию в AbstractHero; здесь выполняется только эффект. */
    CastResult resolve(Field factory, Map<Long, PieceState> pieces, PieceState actor, PieceState first,
            Abilities ability, LightningState lightning, SummonState summons, long turn, MechanicSession session) {
        var states = new HashMap<>(pieces);
        var updates = new HashMap<Long, PieceState>();
        var captures = new ArrayList<Capture>();
        var illusions = new ArrayList<Long>();
        states.put(actor.id(), actor);
        updates.put(actor.id(), actor);
        PieceState target = first;
        if (ability == ZeusAbility.THUNDERSTORM) {
            var candidates = charged(states, lightning, actor).stream()
                    .filter(p -> !session.context().blocks(p, PieceRestriction.CAPTURE)).toList();
            if (candidates.isEmpty()) return rejected(AbilityRejection.NO_CHARGED_TARGETS, lightning, summons);
            int pick = 0;
            if (candidates.size() > 1) {
                var choice = summons.choose(candidates.size());
                pick = choice.index();
                summons = new SummonState(true, choice.nextState(), summons.records());
            }
            target = candidates.get(pick);
        }
        var seen = new HashSet<Long>();
        for (int jump = 0; jump < (ability == ZeusAbility.CHAIN_LIGHTNING ? 3 : 1); jump++) {
            seen.add(target.id());
            Position square = target.position();
            if (ability == ZeusAbility.CHAIN_LIGHTNING)
                lightning = lightning.pulse(new LightningState.Pulse(actor.id(), target.id(), turn, square,
                        LightningState.Kind.HIT), this.rules);
            if (ability == ZeusAbility.THUNDERSTORM || lightning.count(actor.id(), target.id()) >= 3) {
                if (target.hero() == HeroType.ILLUSION) {
                    illusions.add(target.id());
                    lightning = lightning.pulse(new LightningState.Pulse(actor.id(), target.id(), turn, square,
                            LightningState.Kind.DISCHARGE), this.rules);
                } else {
                    PieceState dead = target.hidden(PieceState.Status.CAPTURED);
                    states.put(dead.id(), dead);
                    updates.put(dead.id(), dead);
                    PieceState charged = session.heroes().get(actor.hero()).afterCapture(states.get(actor.id()));
                    states.put(actor.id(), charged);
                    updates.put(actor.id(), charged);
                    captures.add(new Capture(actor.id(), dead.id(), actor.color(),
                            factory.createFigure(target.type(), square, target.color()), square, true,
                            Capture.Cause.LIGHTNING));
                    lightning = lightning.pulse(new LightningState.Pulse(actor.id(), actor.id(), turn, square,
                            LightningState.Kind.RESET), this.rules);
                    session.state(game.mechanics.BuiltInStates.LIGHTNING, lightning);
                    session.state(game.mechanics.BuiltInStates.SUMMONS, summons);
                    HeroEffect reaction = session.react(states, target, actor.id(), square);
                    lightning = session.state(game.mechanics.BuiltInStates.LIGHTNING);
                    summons = session.state(game.mechanics.BuiltInStates.SUMMONS);
                    if (!reaction.result().isSuccess()) return rejected(reaction.result(), lightning, summons);
                    states.putAll(reaction.updates());
                    updates.putAll(reaction.updates());
                    captures.addAll(reaction.captures());
                    break; // Первое настоящее взятие заканчивает цепочку.
                }
            }
            if (ability == ZeusAbility.THUNDERSTORM || jump == 2) break;
            Position previous = square;
            var candidates = states.values().stream().filter(piece -> eligible(piece, actor) && !session.context().blocks(piece, PieceRestriction.CAPTURE)
                    && !seen.contains(piece.id()) && distance(previous, piece.position()) <= 2).toList();
            if (candidates.isEmpty()) break;
            int nearest = candidates.stream().mapToInt(piece -> distance(previous, piece.position())).min().orElseThrow();
            var tied = candidates.stream().filter(piece -> distance(previous, piece.position()) == nearest)
                    .sorted(Comparator.comparingLong(PieceState::id)).toList();
            int pick = 0;
            if (tied.size() > 1) {
                var choice = summons.choose(tied.size());
                pick = choice.index();
                summons = new SummonState(true, choice.nextState(), summons.records());
            }
            target = tied.get(pick);
        }
        return new CastResult(new HeroEffect(Success.SUCCESS, updates, captures), lightning.clean(states),
                summons, List.copyOf(illusions));
    }

    private static int distance(Position a, Position b) {
        return Math.max(Math.abs(a.getRow() - b.getRow()), Math.abs(a.getColumn() - b.getColumn()));
    }

    private static CastResult rejected(MoveResult result, LightningState state, SummonState summons) {
        return new CastResult(HeroEffect.rejected(result), state, summons, List.of());
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                          PieceState target, AbilityRequest request) {
        return request.ability() == ZeusAbility.CHAIN_LIGHTNING
                ? "Zeus: цепная молния на " + cell(request.target()) : "Zeus: разряд по заряженной цели";
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
