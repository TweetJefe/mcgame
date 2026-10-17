package game.economy;

import model.FigureColor;
import java.util.ArrayList;
import java.util.List;

/** Immutable wallets, active bounties and their financial history. */
public record EconomyState(boolean enabled, long whiteHalves, long blackHalves,
                           List<BountyMark> marks, List<GoldChange> history) {
    public EconomyState {
        marks = List.copyOf(marks); history = List.copyOf(history);
        if (whiteHalves < 0 || blackHalves < 0 || marks.size() > 32 || history.size() > 100_000
                || (!enabled && (whiteHalves != 0 || blackHalves != 0 || !marks.isEmpty() || !history.isEmpty())))
            throw new IllegalArgumentException("Invalid economy state");
        if (marks.stream().map(BountyMark::targetId).distinct().count() != marks.size())
            throw new IllegalArgumentException("Duplicate bounty target");
    }
    public static EconomyState initial() { return new EconomyState(true, 0, 0, List.of(), List.of()); }
    public static EconomyState disabled() { return new EconomyState(false, 0, 0, List.of(), List.of()); }
    public long balanceHalves(FigureColor side) {
        if (side != FigureColor.WHITE && side != FigureColor.BLACK) throw new IllegalArgumentException("Not a player");
        return side == FigureColor.WHITE ? whiteHalves : blackHalves;
    }
    public BountyMark mark(long targetId) { return marks.stream().filter(m -> m.targetId() == targetId).findFirst().orElse(null); }
    public EconomyState withMark(BountyMark mark) {
        if (!enabled) throw new IllegalStateException("Economy is disabled");
        var next = new ArrayList<>(marks); next.removeIf(m -> m.targetId() == mark.targetId()); next.add(mark);
        return new EconomyState(true, whiteHalves, blackHalves, next, history);
    }
    public EconomyState removeMark(long targetId) {
        return new EconomyState(enabled, whiteHalves, blackHalves, marks.stream().filter(m -> m.targetId() != targetId).toList(), history);
    }
    public EconomyState expire(long endedTurn) {
        return new EconomyState(enabled, whiteHalves, blackHalves, marks.stream().filter(m -> m.expiresAfterTurn() > endedTurn).toList(), history);
    }
    public EconomyState apply(GoldChange change) {
        if (!enabled) throw new IllegalStateException("Economy is disabled");
        long delta = change.totalHalves();
        long white = whiteHalves, black = blackHalves;
        if (change.side() == FigureColor.WHITE) {
            white = Math.addExact(white, delta);
            if (change.kind() == GoldChange.Kind.STEAL) black = Math.subtractExact(black, delta);
        } else {
            black = Math.addExact(black, delta);
            if (change.kind() == GoldChange.Kind.STEAL) white = Math.subtractExact(white, delta);
        }
        var entries = new ArrayList<>(history); entries.add(change);
        return new EconomyState(true, white, black, marks, entries);
    }
}
