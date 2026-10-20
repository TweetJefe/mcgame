package game.economy;

import model.FigureColor;

/**
 * An audited wallet change; STEAL transfers the total from the opposite side.
 */
public record GoldChange(long turn, Kind kind, FigureColor side, long actorId, Long victimId,
                         long baseHalves, long bountyHalves, long passiveHalves) {
    public enum Kind {CAPTURE, STEAL}

    public GoldChange {
        if (turn <= 0 || kind == null || (side != FigureColor.WHITE && side != FigureColor.BLACK)
                || actorId <= 0 || baseHalves < 0 || bountyHalves < 0 || passiveHalves < 0
                || baseHalves > 2000 || bountyHalves > 3 || passiveHalves > 1
                || (kind == Kind.CAPTURE ? victimId == null || victimId <= 0 : victimId != null || baseHalves < 1 || baseHalves > 4 || bountyHalves != 0 || passiveHalves != 0))
            throw new IllegalArgumentException("Invalid gold transaction");
    }

    public long totalHalves() {
        return baseHalves + bountyHalves + passiveHalves;
    }
}
