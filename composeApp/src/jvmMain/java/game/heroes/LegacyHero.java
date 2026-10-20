package game.heroes;

import game.moves.MovementRejection;
import game.moves.Success;
import game.heroes.ares.AresAbilities;
import game.moves.Capture;
import game.economy.GoldRules;
import game.moves.MoveResult;
import game.state.PieceState;
import model.Position;
import model.figures.King;
import rules.standard.MoveValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static game.heroes.HeroTargeting.Permission.*;

/** Temporary bridge for heroes whose active mechanics have not been migrated yet. */
final class LegacyHero implements Hero {
    private final HeroType type;
    private final HeroRules rules;
    private final Hero storedAllyBehavior;

    LegacyHero(HeroType type, HeroRules rules, Hero storedAllyBehavior) {
        this.type = type;
        this.rules = rules;
        this.storedAllyBehavior = storedAllyBehavior;
    }

    @Override public HeroType type() { return type; }
    @Override public List<Ability> abilities() { return LegacyHeroAbilities.abilities(type); }
    @Override public MoveResult availability(PieceState actor, Ability ability) {
        return LegacyHeroAbilities.availability(actor, ability, rules);
    }
    @Override public HeroEffect preview(HeroActionContext context, PieceState actor, PieceState target,
                                       Position square, Ability ability) {
        return LegacyHeroAbilities.preview(context.field(), actor, target, square, ability, rules);
    }
    @Override public PieceState afterCapture(PieceState actor) {
        return LegacyHeroAbilities.afterCapture(actor, rules);
    }

    @Override public HeroTargeting targeting(Ability ability) {
        return switch (ability) {
            case STEAL, AMBUSH, SUMMON_YAGA, INVISIBILITY, PLANT_MINE, THUNDERSTORM, EVOLVE ->
                    HeroTargeting.untargeted();
            case DISPLACE -> HeroTargeting.square(ILLUSION, SELF, POTTED, DESTINATION);
            case COPY_SPELL -> HeroTargeting.square(ILLUSION, SELF);
            case MUD -> HeroTargeting.square(ILLUSION, SELF, POTTED);
            case CLOUD -> HeroTargeting.square(ILLUSION, SELF, KING, INVISIBLE, POTTED, BOXED);
            case SPEAR -> HeroTargeting.square(ILLUSION, KING, INVISIBLE, POTTED, BOXED);
            case BOX -> HeroTargeting.square(ILLUSION, SELF, KING);
            case GRAPPLE -> HeroTargeting.square(ILLUSION, KING, POTTED);
            case DETONATE -> HeroTargeting.square(ILLUSION, SELF, INVISIBLE);
            default -> Hero.super.targeting(ability);
        };
    }

    @Override public List<AbilityRequest> requests(HeroActionContext context, PieceState actor,
                                                  Ability ability, boolean borrowed) {
        Position square = actor.position();
        if (ability == Ability.SPEAR)
            return AresAbilities.directions(square).stream()
                    .map(target -> new AbilityRequest(square, ability, target, null, borrowed)).toList();
        if (ability == Ability.COPY_SPELL)
            return Hero.super.requests(context, actor, ability, borrowed).stream().filter(request -> {
                PieceState source = context.pieceAt(request.target());
                return source != null && (source.hero() == HeroType.ILLUSION
                        || context.spells().lastUsed().containsKey(source.id()));
            }).toList();
        if (ability != Ability.DISPLACE) return Hero.super.requests(context, actor, ability, borrowed);
        var result = new ArrayList<AbilityRequest>();
        for (PieceState target : context.pieces().values()) {
            if (target.position() == null || target.type() == King.class
                    || Math.max(Math.abs(target.position().getRow() - square.getRow()),
                    Math.abs(target.position().getColumn() - square.getColumn())) > 3) continue;
            for (int dr = -1; dr <= 1; dr++)
                for (int dc = -1; dc <= 1; dc++) {
                    Position to = new Position(target.position().getRow() + dr, target.position().getColumn() + dc);
                    if ((dr != 0 || dc != 0) && MoveValidator.inside(context.field(), to) && context.pieceAt(to) == null)
                        result.add(new AbilityRequest(square, ability, target.position(), to, borrowed));
                }
        }
        return List.copyOf(result);
    }

    @Override public HeroEffect onCaptured(HeroActionContext context, PieceState victim,
                                           long captorId, Position square) {
        if (!AresAbilities.armed(victim, rules.ares()))
            // Ginseng may be carrying an ally through a copied Swallow.
            return storedAllyBehavior.onCaptured(context, victim, captorId, square);
        PieceState spent = context.pieceById(victim.id()).withAbilityState(victim.cooldown(), 0, null);
        PieceState killer = context.pieceById(captorId);
        if (killer == null || killer.isRemoved() || killer.isPotted())
            return HeroEffect.success(Map.of(spent.id(), spent));
        if (killer.type() == King.class) return HeroEffect.rejected(MovementRejection.KING_IN_CHECK);
        PieceState killed = killer.hidden(killer.color() == victim.color()
                ? PieceState.Status.DESTROYED : PieceState.Status.CAPTURED);
        Position capturedAt = killer.position() == null ? square : killer.position();
        Capture capture = new Capture(victim.id(), killer.id(), victim.color(),
                context.field().createFigure(killer.type(), capturedAt, killer.color()),
                capturedAt, true, Capture.Cause.ARES);
        return new HeroEffect(Success.SUCCESS, Map.of(spent.id(), spent, killed.id(), killed), List.of(capture));
    }

    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after, PieceState actor,
                                           PieceState target, AbilityRequest request) {
        return switch (request.ability()) {
            case YARN -> "Kikimora: пряжа на "+cell(request.target());
            case CLAY_POT -> "Kikimora: глиняный горшок на "+cell(request.target());
            case GRAPPLE -> "Chronomlin: притяжение к "+cell(request.target());
            case EVOLVE -> "Chronomlin: улучшение фигуры";
            case CHAIN_LIGHTNING -> "Zeus: цепная молния на "+cell(request.target());
            case THUNDERSTORM -> "Zeus: разряд по заряженной цели";
            case PLANT_MINE -> "Moleminer: заложил мину";
            case DETONATE -> "Moleminer: прыжок и взрыв на "+cell(request.target());
            case BOX -> "Clownganger: коробка на "+cell(request.target());
            case ILLUSION -> "Clownganger: двойник на "+cell(request.target());
            case SPEAR -> "Ares: копьё в направлении " + cell(request.target());
            case CLOUD -> "Faun: туча с центром " + cell(request.target());
            case INVISIBILITY -> "Faun: стал невидимым";
            case COPY_SPELL -> target.hero()==HeroType.ILLUSION?"Ginseng: потратил ульту на пустышку":"Ginseng: скопировал " + before.spells().lastUsed().get(target.id()).getDisplayName() + " у " + target.hero().getName();
            case DISPLACE -> "Ginseng: сдвиг " + cell(request.target()) + " → " + cell(request.destination());
            case SUMMON_YAGA -> "Изба: призвала Бабу-ягу";
            case LASER -> "Lumeshard: ослепил " + target.type().getSimpleName() + " на " + cell(request.target());
            case TELEPORT -> "Lumeshard: телепорт " + cell(actor.position()) + " → " + cell(request.target());
            case BOUNTY ->
                    "Bounty Crow: награда +1,5 золота за " + target.type().getSimpleName() + " на " + cell(request.target());
            case MUD -> "Bolotnik: грязь на " + cell(request.target());
            case AMBUSH -> "Bolotnik: спрятался в болоте у " + cell(actor.position());
            case STEAL ->
                    "Bounty Crow: украл " + GoldRules.format(after.economy().history().getLast().totalHalves()) + " золота";
            default -> Hero.super.abilityMessage(before, after, actor, target, request);
        };
    }

    private static String cell(Position square) {
        return square == null ? "—" : String.valueOf((char) ('a' + square.getColumn())) + (square.getRow() + 1);
    }
}
