package game.summoning;

import game.heroes.izba.IzbaAbility;

import game.heroes.HeroType;
import game.history.GameEvent;
import game.state.*;
import model.*;
import model.figures.Rook;

import java.util.*;

/**
 * Checks generated identities separately from initial draft picks and ordinary capture accounting.
 */
public final class SummonValidator {
    private SummonValidator() {
    }

    public static void validate(GameSnapshot s) {
        SummonState state = Objects.requireNonNull(s.summons());
        boolean containsHero = s.pieces().values().stream().anyMatch(p -> p.hero() == HeroType.IZBA || p.hero() == HeroType.BABA_YAGA);
        require(!containsHero || (s.mode() == GameMode.MOBA && state.enabled()), "Izba requires summoning state");
        require(!state.enabled() || s.mode() == GameMode.MOBA, "Summoning in Classic");
        var births = new HashMap<Long, GameEvent>();
        var matures = new HashMap<Long, GameEvent>();
        var replaced = new HashSet<Long>();
        for (GameEvent e : s.events()) {
            if (e.isAbility(IzbaAbility.SUMMON_YAGA)) {
                PieceState source = s.pieces().get(e.actorId());
                require((source.hero() == HeroType.IZBA || source.hero() == HeroType.GINSENG) && source.color() == e.color(), "Invalid summoning source");
                require(state.records().values().stream().filter(r -> r.sourceId() == source.id() && r.bornTurn() == e.turn() && r.cause() == SummonRecord.Cause.ULTIMATE).count() == 1, "Ultimate without one birth");
            }
            if (e.kind() == GameEvent.Kind.YAGA_BIRTH)
                require(births.put(e.targetId(), e) == null, "Duplicate summon birth");
            if (e.kind() == GameEvent.Kind.YAGA_MATURE)
                require(matures.put(e.actorId(), e) == null, "Duplicate summon transformation");
            if (e.kind() == GameEvent.Kind.REPLACE) {
                PieceState source = s.pieces().get(e.actorId()), target = s.pieces().get(e.targetId());
                require((source.hero() == HeroType.IZBA || source.hero() == HeroType.GINSENG) && source.color() == e.color() && target.status() == PieceState.Status.REPLACED && replaced.add(target.id()), "Invalid replacement event");
                require(s.events().stream().anyMatch(a -> a.turn() == e.turn() && a.actorId() != null && a.actorId().equals(e.actorId()) && a.isAbility(IzbaAbility.SUMMON_YAGA)), "Replacement without ultimate");
            }
        }
        require(births.size() == state.records().size(), "Missing or orphan summon origin");
        for (var r : state.records().values()) {
            PieceState p = s.pieces().get(r.id()), source = s.pieces().get(r.sourceId());
            GameEvent birth = births.get(r.id());
            require(p != null && source != null && (source.hero() == HeroType.IZBA || source.hero() == HeroType.GINSENG) && p.color() == source.color(), "Invalid summon lineage");
            require(p.hero() == (r.transformed() ? HeroType.IZBA : HeroType.BABA_YAGA), "Summon form differs from lifetime");
            require(birth != null && birth.actorId() == r.sourceId() && birth.color() == p.color() && birth.turn() == r.bornTurn() && birth.to().equals(r.bornAt()), "Birth differs from origin");
            FigureColor activeAtBirth = (s.turn().number() - r.bornTurn()) % 2 == 0 ? s.active() : (s.active() == FigureColor.WHITE ? FigureColor.BLACK : FigureColor.WHITE);
            long expected = 2L * s.heroRules().izba().lifetimeTurns() - (activeAtBirth == p.color() ? 0 : 1);
            require(r.transformsAfterTurn() - r.bornTurn() == expected, "Invalid summon lifetime");
            if (r.cause() == SummonRecord.Cause.ULTIMATE)
                require(s.events().stream().anyMatch(e -> e.isAbility(IzbaAbility.SUMMON_YAGA) && e.turn() == r.bornTurn() && e.actorId() == r.sourceId()), "Birth without ultimate");
            else
                require(source.isRemoved() && s.events().stream().anyMatch(e -> e.targetId() != null && e.targetId() == r.sourceId() && e.turn() == r.bornTurn()
                        && Set.of(GameEvent.Kind.MOVE, GameEvent.Kind.COUNTER_CAPTURE, GameEvent.Kind.BOG_CAPTURE, GameEvent.Kind.ARES_REVENGE,GameEvent.Kind.MINE_CAPTURE,GameEvent.Kind.LIGHTNING_CAPTURE,GameEvent.Kind.BLAST_CAPTURE,GameEvent.Kind.BLAST_DEATH).contains(e.kind())
                        && Math.max(Math.abs(e.to().getRow() - r.bornAt().getRow()), Math.abs(e.to().getColumn() - r.bornAt().getColumn())) == 1), "Birth without nearby death");
            if (r.cause() == SummonRecord.Cause.ULTIMATE)
                require(p.color() == FigureColor.WHITE ? r.bornAt().getRow() < 2 : r.bornAt().getRow() > 5, "Ultimate birth outside home ranks");
            GameEvent mature = matures.get(r.id());
            require(r.transformed() == (mature != null), "Missing or unexpected transformation event");
            if (r.transformed())
                require(mature.turn() == r.transformsAfterTurn() && mature.color() == p.color() && mature.targetId() == p.id() && r.transformsAfterTurn() < s.turn().number() && p.type() == Rook.class, "Invalid transformation");
            else require(p.isRemoved() || p.isPotted() || r.transformsAfterTurn() >= s.turn().number(), "Overdue living Baba Yaga");
        }
        for (PieceState p : s.pieces().values()) {
            require(p.hero() != HeroType.BABA_YAGA || state.records().containsKey(p.id()), "Baba Yaga was not summoned");
            require((p.status() == PieceState.Status.REPLACED) == replaced.contains(p.id()), "Removal without replacement history");
        }
        require(matures.size() == state.records().values().stream().filter(SummonRecord::transformed).count(), "Orphan transformation");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalArgumentException(message);
    }
}
