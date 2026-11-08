package game.draft;

import game.heroes.HeroRules;
import game.heroes.HeroType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import model.FigureColor;
import model.Position;

import static model.FigureColor.WHITE;
import static model.FigureColor.BLACK;

/** Серверная сессия нового драфта. Часы продвигаются командами и вызовом tick планировщика. */
public final class MatchDraft {
    public enum Phase { SELECTION, PREPARATION, COMPLETE }
    public enum Kind { BAN, PICK }
    public enum Result { SUCCESS, WRONG_PHASE, WRONG_SIDE, ACTION_EXPIRED, UNAVAILABLE_HERO, ROSTER_WOULD_BE_IMPOSSIBLE,
        NOT_SELECTED, INVALID_SQUARE, SQUARE_OCCUPIED }
    public record Step(FigureColor side, Kind kind) {}
    public record Action(int index, FigureColor side, Kind kind, HeroType hero, boolean automatic, long elapsedNanos) {}
    public record Snapshot(Phase phase, int step, Step expected, List<HeroType> bans,
                           Map<FigureColor, List<HeroType>> rosters, Map<FigureColor, Long> reserveNanos,
                           long actionNanos, long preparationNanos, Map<Position, HeroType> assignments,
                           List<Action> actions, DraftCatalog catalog) {
        public Snapshot {
            bans = List.copyOf(bans);
            rosters = Map.of(WHITE, List.copyOf(rosters.get(WHITE)), BLACK, List.copyOf(rosters.get(BLACK)));
            reserveNanos = Map.copyOf(reserveNanos);
            assignments = Map.copyOf(assignments);
            actions = List.copyOf(actions);
        }
    }

    public static final Duration ACTION_TIME = Duration.ofSeconds(20);
    public static final Duration RESERVE_TIME = Duration.ofMinutes(1);
    public static final Duration PREPARATION_TIME = Duration.ofSeconds(15);
    private static final List<Step> ORDER = List.of(
            new Step(WHITE, Kind.BAN), new Step(BLACK, Kind.BAN),
            new Step(BLACK, Kind.PICK), new Step(WHITE, Kind.PICK),
            new Step(WHITE, Kind.PICK), new Step(BLACK, Kind.PICK),
            new Step(BLACK, Kind.BAN), new Step(WHITE, Kind.BAN),
            new Step(WHITE, Kind.PICK), new Step(BLACK, Kind.PICK),
            new Step(BLACK, Kind.PICK), new Step(WHITE, Kind.PICK),
            new Step(WHITE, Kind.BAN), new Step(BLACK, Kind.BAN),
            new Step(WHITE, Kind.PICK), new Step(BLACK, Kind.PICK));

    private final HeroRules rules;
    private final DraftCatalog catalog;
    private final LongSupplier clock;
    private final Map<HeroType, DraftRole> roles = new HashMap<>();
    private final List<HeroType> bans = new ArrayList<>();
    private final Map<FigureColor, List<HeroType>> rosters = Map.of(WHITE, new ArrayList<>(), BLACK, new ArrayList<>());
    private final Map<FigureColor, Long> reserve = new HashMap<>();
    private final Map<Position, HeroType> assignments = new HashMap<>();
    private final List<Action> actions = new ArrayList<>();
    private Phase phase = Phase.SELECTION;
    private int step;
    private long started;
    private long lastRead;
    private DraftSnapshot completed;

    public MatchDraft() { this(HeroRules.defaults(), System::nanoTime); }

    public MatchDraft(HeroRules rules, LongSupplier clock) {
        this.rules = Objects.requireNonNull(rules);
        this.clock = Objects.requireNonNull(clock);
        catalog = DraftCatalog.current(rules.definitions());
        for (var hero : catalog.heroes()) roles.put(hero, DraftRole.of(catalog.definition(hero)));
        reserve.put(WHITE, RESERVE_TIME.toNanos());
        reserve.put(BLACK, RESERVE_TIME.toNanos());
        started = lastRead = clock.getAsLong();
        if (!feasible(bans, rosters) || catalog.heroes().size() < ORDER.size())
            throw new IllegalArgumentException("Catalog cannot fill this draft");
    }

    public static List<Step> order() { return ORDER; }
    public HeroRules rules() { return rules; }

    private long now() {
        long value = clock.getAsLong();
        if (value < lastRead) throw new IllegalStateException("Draft clock moved backwards");
        lastRead = value;
        return value;
    }

    /** Снимок не выполняет автоматических действий и не списывает время повторно. */
    public synchronized Snapshot getSnapshot() {
        long elapsed = Math.max(0, now() - started);
        var remaining = new HashMap<>(reserve);
        long basic = 0, preparation = 0;
        Step expected = phase == Phase.SELECTION ? ORDER.get(step) : null;
        if (expected != null) {
            basic = Math.max(0, ACTION_TIME.toNanos() - elapsed);
            remaining.put(expected.side(), Math.max(0, reserve.get(expected.side()) - Math.max(0, elapsed - ACTION_TIME.toNanos())));
        } else if (phase == Phase.PREPARATION) preparation = Math.max(0, PREPARATION_TIME.toNanos() - elapsed);
        return new Snapshot(phase, step, expected, bans, rosters, remaining, basic, preparation, assignments, actions, catalog);
    }

    public synchronized Snapshot tick() { advance(now()); return getSnapshot(); }

    private void advance(long time) {
        while (phase == Phase.SELECTION) {
            long budget = ACTION_TIME.toNanos() + reserve.get(ORDER.get(step).side());
            if (time - started < budget) return;
            var hero = legalHeroes().stream().findFirst().orElseThrow(() -> new IllegalStateException("No legal automatic action"));
            commit(hero, true, budget, started + budget);
        }
        if (phase == Phase.PREPARATION && time - started >= PREPARATION_TIME.toNanos()) finishPreparation();
    }

    public synchronized List<HeroType> availableHeroes() { return List.copyOf(legalHeroes()); }

    private List<HeroType> legalHeroes() {
        if (phase != Phase.SELECTION) return List.of();
        return catalog.heroes().stream().filter(this::unused).filter(this::legal).toList();
    }

    private boolean unused(HeroType hero) {
        return roles.containsKey(hero) && !bans.contains(hero) && rosters.values().stream().noneMatch(r -> r.contains(hero));
    }

    private boolean legal(HeroType hero) {
        var nextBans = new ArrayList<>(bans);
        var nextRosters = Map.of(WHITE, new ArrayList<>(rosters.get(WHITE)), BLACK, new ArrayList<>(rosters.get(BLACK)));
        var action = ORDER.get(step);
        if (action.kind() == Kind.BAN) nextBans.add(hero);
        else nextRosters.get(action.side()).add(hero);
        return feasible(nextBans, nextRosters);
    }

    private boolean feasible(List<HeroType> excluded, Map<FigureColor, ? extends List<HeroType>> picks) {
        for (var role : DraftRole.values()) {
            int missing = 0;
            for (var side : List.of(WHITE, BLACK)) {
                long selected = picks.get(side).stream().filter(h -> roles.get(h) == role).count();
                if (selected > role.required()) return false;
                missing += role.required() - (int) selected;
            }
            long available = catalog.heroes().stream().filter(h -> roles.get(h) == role && !excluded.contains(h)
                    && picks.values().stream().noneMatch(p -> p.contains(h))).count();
            if (available < missing) return false;
        }
        return true;
    }

    public synchronized Result ban(FigureColor side, HeroType hero) { return act(side, hero, Kind.BAN); }
    public synchronized Result pick(FigureColor side, HeroType hero) { return act(side, hero, Kind.PICK); }

    private Result act(FigureColor side, HeroType hero, Kind kind) {
        long time = now();
        int requestedStep = step;
        advance(time);
        if (step != requestedStep) return Result.ACTION_EXPIRED;
        if (phase != Phase.SELECTION || ORDER.get(step).kind() != kind) return Result.WRONG_PHASE;
        if (ORDER.get(step).side() != side) return Result.WRONG_SIDE;
        if (!unused(hero)) return Result.UNAVAILABLE_HERO;
        if (!legal(hero)) return Result.ROSTER_WOULD_BE_IMPOSSIBLE;
        commit(hero, false, time - started, time);
        return Result.SUCCESS;
    }

    private void commit(HeroType hero, boolean automatic, long elapsed, long time) {
        var action = ORDER.get(step);
        reserve.put(action.side(), reserve.get(action.side()) - Math.max(0, elapsed - ACTION_TIME.toNanos()));
        if (action.kind() == Kind.BAN) bans.add(hero);
        else rosters.get(action.side()).add(hero);
        actions.add(new Action(step, action.side(), action.kind(), hero, automatic, elapsed));
        step++;
        started = time;
        if (step == ORDER.size()) phase = Phase.PREPARATION;
    }

    public synchronized List<Position> placementTargets(FigureColor side, HeroType hero) {
        if (phase != Phase.PREPARATION || !rosters.containsKey(side) || !rosters.get(side).contains(hero)) return List.of();
        return catalog.startingSquares(hero, side).stream()
                .filter(p -> !assignments.containsKey(p) || assignments.get(p) == hero).toList();
    }

    public synchronized Result place(FigureColor side, HeroType hero, Position square) {
        advance(now());
        if (phase != Phase.PREPARATION) return Result.WRONG_PHASE;
        if (!rosters.containsKey(side) || !rosters.get(side).contains(hero)) return Result.NOT_SELECTED;
        if (!catalog.startingSquares(hero, side).contains(square)) return Result.INVALID_SQUARE;
        if (assignments.containsKey(square) && assignments.get(square) != hero) return Result.SQUARE_OCCUPIED;
        assignments.values().removeIf(h -> h == hero);
        assignments.put(square, hero);
        return Result.SUCCESS;
    }

    public synchronized Result clearPlacement(FigureColor side, HeroType hero) {
        advance(now());
        if (phase != Phase.PREPARATION) return Result.WRONG_PHASE;
        if (!rosters.containsKey(side) || !rosters.get(side).contains(hero)) return Result.NOT_SELECTED;
        assignments.values().removeIf(h -> h == hero);
        return Result.SUCCESS;
    }

    private void finishPreparation() {
        for (var side : List.of(WHITE, BLACK)) for (var hero : rosters.get(side))
            if (!assignments.containsValue(hero)) {
                Position square = placementTargets(side, hero).stream().findFirst().orElseThrow();
                assignments.put(square, hero);
            }
        // Старый снимок — только совместимое представление конечного состава для Game/XML.
        // Настоящая чередующаяся последовательность и время остаются в actions этой сессии.
        var legacy = new Draft(new DraftRules(3, 5), catalog, rules);
        var whiteBans = actions.stream().filter(a -> a.side() == WHITE && a.kind() == Kind.BAN).toList();
        var blackBans = actions.stream().filter(a -> a.side() == BLACK && a.kind() == Kind.BAN).toList();
        for (int i = 0; i < 3; i++) {
            require(legacy.ban(WHITE, whiteBans.get(i).hero()));
            require(legacy.ban(BLACK, blackBans.get(i).hero()));
        }
        for (int i = 0; i < 5; i++) for (var side : List.of(WHITE, BLACK)) {
            var hero = rosters.get(side).get(i);
            Position square = assignments.entrySet().stream().filter(e -> e.getValue() == hero).findFirst().orElseThrow().getKey();
            require(legacy.pick(side, hero, square));
        }
        completed = legacy.getSnapshot();
        phase = Phase.COMPLETE;
    }

    private static void require(DraftActionResult result) {
        if (!result.isSuccess()) throw new IllegalStateException("Cannot export completed draft: " + result);
    }

    public synchronized DraftSnapshot completedDraft() {
        advance(now());
        if (phase != Phase.COMPLETE) throw new IllegalStateException("Preparation has not finished");
        return completed;
    }
}
