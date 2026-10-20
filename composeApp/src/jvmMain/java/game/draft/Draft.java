package game.draft;

import game.heroes.HeroType;
import model.FigureColor;
import model.Position;

import java.util.*;

import static model.FigureColor.*;

/**
 * Authoritative local draft commands. Rejections preserve the entire immutable snapshot.
 */
public final class Draft {
    private DraftSnapshot state;

    public Draft() {
        this(DraftRules.prototype());
    }

    public Draft(DraftRules rules) {
        state = new DraftSnapshot(0, rules, rules.bansPerSide() == 0 ? DraftPhase.PICKS : DraftPhase.BANS, WHITE,
                List.of(), Map.of(WHITE, List.of(), BLACK, List.of()), Set.of(), List.of());
    }

    public synchronized DraftSnapshot getSnapshot() {
        return state;
    }

    public synchronized List<HeroType> getAvailableHeroes(FigureColor side) {
        if (side != WHITE && side != BLACK || state.isComplete() || state.finished().contains(side)) return List.of();
        return HeroCatalog.heroes(state.rules().catalogVersion()).stream().filter(h -> !state.bans().contains(h) && !state.isPicked(h))
                .filter(h -> state.phase() == DraftPhase.BANS || !getPlacementTargets(side, h).isEmpty()).toList();
    }

    public synchronized List<Position> getPlacementTargets(FigureColor side, HeroType hero) {
        if ((side != WHITE && side != BLACK) || hero == null || state.isComplete() || state.finished().contains(side) || state.bans().contains(hero) || state.isPicked(hero))
            return List.of();
        Set<Position> occupied = new HashSet<>();
        state.picksFor(side).forEach(p -> occupied.add(p.square()));
        return HeroCatalog.startingSquares(hero, side).stream().filter(p -> !occupied.contains(p)).toList();
    }

    public synchronized DraftActionResult ban(FigureColor side, HeroType hero) {
        DraftActionResult guard = guard(side, DraftPhase.BANS);
        if (!guard.isSuccess()) return guard;
        DraftActionResult target = heroGuard(hero);
        if (!target.isSuccess()) return target;
        return apply(new DraftEvent(state.events().size() + 1L, DraftEvent.Kind.BAN, side, hero, null));
    }

    public synchronized DraftActionResult pick(FigureColor side, HeroType hero, Position square) {
        DraftActionResult guard = guard(side, DraftPhase.PICKS);
        if (!guard.isSuccess()) return guard;
        DraftActionResult target = heroGuard(hero);
        if (!target.isSuccess()) return target;
        if (state.picksFor(side).size() >= state.rules().maxPicksPerSide()) return DraftActionResult.PICK_LIMIT;
        if (square == null || !HeroCatalog.startingSquares(hero, side).contains(square))
            return DraftActionResult.INVALID_SQUARE;
        if (state.picksFor(side).stream().anyMatch(p -> p.square().equals(square)))
            return DraftActionResult.SQUARE_OCCUPIED;
        return apply(new DraftEvent(state.events().size() + 1L, DraftEvent.Kind.PICK, side, hero, square));
    }

    public synchronized DraftActionResult finishPicks(FigureColor side) {
        DraftActionResult guard = guard(side, DraftPhase.PICKS);
        if (!guard.isSuccess()) return guard;
        return apply(new DraftEvent(state.events().size() + 1L, DraftEvent.Kind.FINISH_PICKS, side, null, null));
    }

    private DraftActionResult guard(FigureColor side, DraftPhase phase) {
        if (state.isComplete()) return DraftActionResult.DRAFT_COMPLETE;
        if (side != state.active()) return DraftActionResult.WRONG_SIDE;
        return state.phase() == phase ? DraftActionResult.SUCCESS : DraftActionResult.WRONG_PHASE;
    }

    private DraftActionResult heroGuard(HeroType hero) {
        if (hero == null || !HeroCatalog.heroes(state.rules().catalogVersion()).contains(hero))
            return DraftActionResult.INVALID_HERO;
        if (state.bans().contains(hero)) return DraftActionResult.HERO_BANNED;
        return state.isPicked(hero) ? DraftActionResult.HERO_PICKED : DraftActionResult.SUCCESS;
    }

    private DraftActionResult apply(DraftEvent event) {
        var bans = new ArrayList<>(state.bans());
        var events = new ArrayList<>(state.events());
        events.add(event);
        var white = new ArrayList<>(state.picksFor(WHITE));
        var black = new ArrayList<>(state.picksFor(BLACK));
        var finished = new HashSet<>(state.finished());
        DraftPhase phase = state.phase();
        FigureColor next = opposite(event.side());
        if (event.kind() == DraftEvent.Kind.BAN) bans.add(event.hero());
        else if (event.kind() == DraftEvent.Kind.PICK)
            (event.side() == WHITE ? white : black).add(new HeroPick(event.hero(), event.square()));
        else finished.add(event.side());
        if (phase == DraftPhase.BANS) {
            long whiteBans = events.stream().filter(e -> e.kind() == DraftEvent.Kind.BAN && e.side() == WHITE).count();
            long blackBans = events.stream().filter(e -> e.kind() == DraftEvent.Kind.BAN && e.side() == BLACK).count();
            if (whiteBans == state.rules().bansPerSide() && blackBans == state.rules().bansPerSide()) {
                phase = DraftPhase.PICKS;
                next = WHITE;
            }
        }
        var rosters = Map.<FigureColor, List<HeroPick>>of(WHITE, white, BLACK, black);
        if (phase == DraftPhase.PICKS) {
            for (FigureColor side : List.of(WHITE, BLACK)) {
                var roster = rosters.get(side);
                boolean noCandidate = HeroCatalog.heroes(state.rules().catalogVersion()).stream().filter(h -> !bans.contains(h))
                        .filter(h -> rosters.values().stream().flatMap(List::stream).noneMatch(p -> p.hero() == h))
                        .noneMatch(h -> HeroCatalog.startingSquares(h, side).stream().anyMatch(p -> roster.stream().noneMatch(pick -> pick.square().equals(p))));
                if (roster.size() >= state.rules().maxPicksPerSide() || noCandidate) finished.add(side);
            }
            if (finished.contains(WHITE) && finished.contains(BLACK)) {
                phase = DraftPhase.COMPLETE;
                next = NONE;
            } else if (finished.contains(next)) next = opposite(next);
        }
        state = new DraftSnapshot(state.version() + 1, state.rules(), phase, next, bans, rosters, finished, events);
        return DraftActionResult.SUCCESS;
    }

    private static FigureColor opposite(FigureColor side) {
        return side == WHITE ? BLACK : WHITE;
    }

    public static Draft fromSnapshot(DraftSnapshot saved) {
        Objects.requireNonNull(saved, "Missing draft");
        Draft restored = replay(saved.rules(), saved.events());
        if (!restored.getSnapshot().equals(saved))
            throw new IllegalArgumentException("Draft snapshot differs from its action history");
        return restored;
    }

    public static Draft replay(DraftRules rules, List<DraftEvent> events) {
        if (events == null || events.size() > 100) throw new IllegalArgumentException("Invalid draft history");
        Draft restored = new Draft(rules);
        for (int i = 0; i < events.size(); i++) {
            DraftEvent e = events.get(i);
            if (e == null || e.sequence() != i + 1L) throw new IllegalArgumentException("Invalid draft event sequence");
            DraftActionResult result = switch (e.kind()) {
                case BAN -> restored.ban(e.side(), e.hero());
                case PICK -> restored.pick(e.side(), e.hero(), e.square());
                case FINISH_PICKS -> restored.finishPicks(e.side());
            };
            if (!result.isSuccess()) throw new IllegalArgumentException("Invalid draft history: " + result);
        }
        return restored;
    }
}
