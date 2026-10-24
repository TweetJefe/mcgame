package game.core;

import game.heroes.HeroType;
import game.draft.DraftValidator;
import game.summoning.*;
import game.heroes.ginseng.SpellValidator;
import game.heroes.faun.FaunValidator;
import game.heroes.ares.AresValidator;
import game.economy.EconomyValidator;
import game.terrain.TerrainValidator;
import game.history.GameEvent;
import game.history.MadeMove;
import game.moves.MoveKind;
import game.state.ClockSnapshot;
import game.state.GameMode;
import game.state.GameOutcome;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.ItemSlot;
import game.state.PieceState;
import game.state.Termination;

import model.*;
import model.figures.*;
import rules.standard.MoveValidator;
import rules.moba.AttackRules;
import rules.standard.PositionValidator;
import rules.standard.MaterialRules;

import java.util.*;

/**
 * Cross-record validation before any restored Game is exposed. Not a proof of historical reachability.
 */
public final class SnapshotValidator {
    private SnapshotValidator() {
    }

    public static void validate(GameSnapshot s) {
        require(s != null, "Missing game snapshot");
        require(s.mode() != null && s.field() != null && s.turn() != null && s.heroRules() != null && s.summons() != null && s.spells() != null && s.faun()!=null && s.clown()!=null && s.mines()!=null && s.lightning() != null, "Missing game metadata");
        require(side(s.active()) && s.version() >= 0 && s.version() < Long.MAX_VALUE && s.turn().number() > 0
                && s.turn().number() < Long.MAX_VALUE, "Invalid turn or version");
        PositionValidator.validateBoard(s.field().getGameField(), s.mode() == GameMode.CLASSIC);
        boolean midTurn = s.mode() == GameMode.MOBA && (s.turn().moveUsed() || s.turn().abilityUsed());
        FigureColor epTurn = s.mode() == GameMode.MOBA && s.turn().moveUsed() ? MoveValidator.opposite(s.active()) : s.active();
        PositionValidator.validateState(s.field(), epTurn, s.castlingRights(), s.enPassantTarget(), s.halfmoveClock(), s.mode() == GameMode.CLASSIC);
        require(s.mode() != GameMode.MOBA || midTurn || !AttackRules.isInCheck(s.field().getGameField(), s.pieces(), MoveValidator.opposite(s.active()), s.terrain()), "Inactive king is in check at turn start");
        require(!midTurn || !AttackRules.isInCheck(s.field().getGameField(), s.pieces(), s.active(), s.terrain()), "Acting king is left in check");
        require(s.pieces().size() <= (s.mode() == GameMode.CLASSIC ? 32 : SummonState.MAX_IDENTITIES), "Too many piece identities");
        var occupied = new HashSet<Position>();
        var owners = new HashMap<Long, Long>();
        int white = 0, black = 0;
        for (var entry : s.pieces().entrySet()) {
            PieceState piece = entry.getValue();
            require(entry.getKey() == piece.id() && piece.id() < Long.MAX_VALUE, "Invalid piece identity");
            if (!s.summons().records().containsKey(piece.id()) && !s.clown().illusions().containsKey(piece.id())) {
                if (piece.color() == FigureColor.WHITE) white++;
                else black++;
            }
            if (piece.status() == PieceState.Status.BOARD) {
                require(occupied.add(piece.position()), "Two pieces on one square");
                Figure figure = s.field().getFigureByPosition(piece.position());
                require(figure != null && figure.getClass() == piece.type() && figure.getColor() == piece.color(), "Piece state differs from board");
            } else require(piece.type() != King.class, "King cannot be captured or swallowed");
            if (piece.hero() == HeroType.KIKIMORA)require(piece.cooldown()<=s.heroRules().kikimora().yarnCooldown() && piece.ultimateCharge()<=s.heroRules().kikimora().ultimateCaptures(),"Invalid Kikimora resources");
            else if(piece.hero()==HeroType.CHRONOMLIN)require(piece.cooldown()<=s.heroRules().chronomlin().grappleCooldown() && piece.ultimateCharge()<=s.heroRules().chronomlin().ultimateCaptures(),"Invalid Chronomlin resources");
            else if(piece.hero()==HeroType.ZEUS)require(piece.cooldown()<=s.heroRules().zeus().lightningCooldown() && piece.ultimateCharge()<=s.heroRules().zeus().ultimateCaptures(),"Invalid Zeus resources");
            else if(piece.hero()==HeroType.MOLEMINER)require(piece.cooldown()<=s.heroRules().moleminer().mineCooldown() && piece.ultimateCharge()<=s.heroRules().moleminer().ultimateCaptures(),"Invalid Moleminer resources");
            else if(piece.hero()==HeroType.CLOWNGANGER)require(piece.cooldown()<=s.heroRules().clownganger().boxCooldown() && piece.ultimateCharge()<=s.heroRules().clownganger().ultimateCaptures(),"Invalid Clownganger resources");
            else if (piece.hero() == HeroType.NONE || piece.hero()==HeroType.ILLUSION)
                require(piece.cooldown() == 0 && piece.ultimateCharge() == 0 && piece.storedPieceId() == null, "Nonhero has hero state");
            else if (piece.hero() == HeroType.PUDGE)
                require(piece.cooldown() <= s.pudgeRules().hookCooldown() && piece.ultimateCharge() <= s.pudgeRules().ultimateCaptures(), "Abilities state exceeds configured limits");
            else if (piece.hero() == HeroType.LUMESHARD)
                require(piece.cooldown() <= s.lumeshardRules().laserCooldown() && piece.ultimateCharge() <= s.lumeshardRules().ultimateCaptures(), "Lumeshard state exceeds configured limits");
            else if (piece.hero() == HeroType.BOUNTY_CROW)
                require(piece.cooldown() <= s.crowRules().bountyCooldown() && piece.ultimateCharge() <= s.crowRules().ultimateCaptures(), "Crow state exceeds configured limits");
            else if (piece.hero() == HeroType.ARES)
                require(piece.cooldown() <= s.heroRules().ares().spearCooldown() && piece.ultimateCharge() <= s.heroRules().ares().revengeCaptures(), "Invalid Ares resources");
            else if (piece.hero() == HeroType.FAUN)
                require(piece.cooldown() <= s.heroRules().faun().cloudCooldown() && piece.ultimateCharge() <= s.heroRules().faun().ultimateCaptures(), "Invalid Faun resources");
            else if (piece.hero() == HeroType.GINSENG)
                require(piece.cooldown() <= s.heroRules().ginseng().displacementCooldown() && piece.ultimateCharge() <= s.heroRules().ginseng().ultimateCaptures(), "Invalid Ginseng resources");
            else if (piece.hero() == HeroType.IZBA)
                require(piece.cooldown() == 0 && piece.ultimateCharge() <= s.heroRules().izba().ultimateCaptures(), "Invalid Izba charge");
            else if (piece.hero() == HeroType.BABA_YAGA)
                require(piece.cooldown() == 0 && piece.ultimateCharge() == 0, "Invalid Baba Yaga resources");
            else
                require(piece.cooldown() <= s.bolotnikRules().mudCooldown() && piece.ultimateCharge() <= s.bolotnikRules().ultimateCaptures(), "Bolotnik state exceeds configured limits");
            require(piece.blindTurns() <= s.lumeshardRules().blindnessTurns(), "Blindness exceeds duration");
            require(!piece.isBlinded() || (s.mode() == GameMode.MOBA && piece.type() != King.class && !piece.isRemoved()), "Invalid blind piece");
            require(s.mode() != GameMode.CLASSIC || piece.hero() == HeroType.NONE, "Hero in classic game");
            for (ItemSlot slot : piece.inventory())
                require(slot.isEmpty() && slot.charges() == 0 && slot.cooldown() == 0, "Item catalog is not implemented");
            if (piece.storedPieceId() != null) {
                PieceState passenger = s.pieces().get(piece.storedPieceId());
                require(!piece.isRemoved() && passenger != null
                        && passenger.status() == PieceState.Status.SWALLOWED && passenger.color() == piece.color(), "Invalid swallowed ally");
                require(owners.put(passenger.id(), piece.id()) == null, "Ally is inside two heroes");
            }
        }
        require(white <= 16 && black <= 16, "Too many original pieces without summon origins");
        for (var entry : s.field().getGameField().entrySet())
            require((entry.getValue() instanceof Empty) == !occupied.contains(entry.getKey()), "Board square has no piece identity");
        for (PieceState piece : s.pieces().values()) {
            require(piece.status() != PieceState.Status.SWALLOWED || owners.containsKey(piece.id()), "Orphan swallowed piece");
            var visited = new HashSet<Long>();
            PieceState current = piece;
            while (current != null) {
                require(visited.add(current.id()), "Cycle of swallowed heroes");
                current = current.storedPieceId() == null ? null : s.pieces().get(current.storedPieceId());
            }
        }
        validateHistory(s);
        SpellValidator.validate(s);
        game.heroes.chronomlin.ChronomlinValidator.validate(s);
        game.heroes.kikimora.KikimoraValidator.validate(s);
        FaunValidator.validate(s);
        AresValidator.validate(s);
        game.heroes.clownganger.ClownValidator.validate(s);
        game.heroes.moleminer.MineValidator.validate(s);
        game.heroes.zeus.LightningValidator.validate(s);
        EconomyValidator.validate(s);
        TerrainValidator.validate(s);
        SummonValidator.validate(s);
        DraftValidator.validate(s);
        validateCaptures(s, s.whiteCaptures(), FigureColor.BLACK, s.whitePoints());
        validateCaptures(s, s.blackCaptures(), FigureColor.WHITE, s.blackPoints());
        require(s.repetitions().size() > 0 && s.repetitions().size() <= 100_000, "Invalid repetition map");
        for (var entry : s.repetitions().entrySet())
            require(entry.getKey() != null && !entry.getKey().isBlank()
                    && entry.getKey().length() <= 256 && entry.getValue() > 0, "Invalid repetition count");
        require(s.outcome() != null && s.outcome().reason() != null && s.outcome().winner() != null, "Missing outcome");
        boolean win = EnumSet.of(Termination.CHECKMATE, Termination.RESIGNATION, Termination.TIMEOUT).contains(s.outcome().reason());
        require(win ? side(s.outcome().winner()) : s.outcome().winner() == FigureColor.NONE, "Winner differs from outcome");
        require(s.mode() != GameMode.MOBA || EnumSet.of(Termination.IN_PROGRESS, Termination.CHECKMATE, Termination.STALEMATE,
                Termination.RESIGNATION, Termination.TIMEOUT, Termination.DRAW_AGREEMENT).contains(s.outcome().reason()), "Unsupported Moba outcome");
        require(s.drawOffer() != null && (!s.outcome().isFinished() || s.drawOffer() == FigureColor.NONE), "Invalid draw offer");
        validateClock(s.clock(), s.active(), s.outcome().isFinished());
        if (s.outcome().reason() == Termination.TIMEOUT || s.outcome().reason() == Termination.TIMEOUT_DRAW) {
            require(s.clock().enabled() && (s.active() == FigureColor.WHITE ? s.clock().whiteNanos() : s.clock().blackNanos()) == 0, "Timeout without fallen flag");
            require(!win || s.outcome().winner() == MoveValidator.opposite(s.active()), "Timeout winner is active player");
        }
        if (s.mode() == GameMode.MOBA && (!s.outcome().isFinished() || s.outcome().reason() == Termination.CHECKMATE || s.outcome().reason() == Termination.STALEMATE)) {
            var ctx = new ActionEngine.Context(s.field(), s.pieces(), s.castlingRights(), s.enPassantTarget(), s.economy(), s.turn().number(), s.terrain(), s.summons(), s.spells(), s.faun(),s.clown(),s.mines(),s.lightning(),game.heroes.HeroRegistry.fromRules(s.heroRules()));
            boolean playable = s.turn().moveUsed() ? ActionEngine.canFinishAfterMove(ctx, s.active(), !s.turn().abilityUsed(), s.heroRules()) : (s.turn().abilityUsed() ? ActionEngine.hasMove(ctx, s.active(), s.mode(), s.heroRules()) : ActionEngine.hasTurn(ctx, s.active(), s.heroRules()));
            require(playable == !s.outcome().isFinished(), "Outcome differs from available turn");
            if (!playable)
                require((s.outcome().reason() == Termination.CHECKMATE) == AttackRules.isInCheck(s.field().getGameField(), s.pieces(), s.active(), s.terrain()), "Mate/pat differs from check");
            if (!playable && s.outcome().reason() == Termination.CHECKMATE)
                require(s.outcome().winner() == MoveValidator.opposite(s.active()), "Invalid mate winner");
        }
        if (s.mode() == GameMode.CLASSIC) validateClassicOutcome(s);
    }

    private static void validateClassicOutcome(GameSnapshot s) {
        Game position = Game.fromPosition(new GamePosition(s.field().getGameField(), s.active(), s.castlingRights(), s.enPassantTarget(), s.halfmoveClock()), () -> 0L);
        String key = position.getPositionKey();
        require(s.repetitions().containsKey(key), "Current classic position missing from repetitions");
        GameOutcome automatic = position.getOutcome();
        if (!automatic.isFinished() && s.repetitions().get(key) >= 5)
            automatic = new GameOutcome(Termination.FIVEFOLD_REPETITION, FigureColor.NONE);
        if (EnumSet.of(Termination.IN_PROGRESS, Termination.CHECKMATE, Termination.STALEMATE, Termination.INSUFFICIENT_MATERIAL,
                Termination.SEVENTY_FIVE_MOVE_RULE, Termination.FIVEFOLD_REPETITION).contains(s.outcome().reason()))
            require(automatic.equals(s.outcome()), "Classic outcome differs from position");
        // A declared intended move can justify a claim before the counters reach the threshold.
        if (s.outcome().reason() == Termination.TIMEOUT_DRAW)
            require(!MaterialRules.canPossiblyMate(s.field().getGameField(), MoveValidator.opposite(s.active())), "Timeout draw has mating material");
    }

    private static void validateHistory(GameSnapshot s) {
        require(s.events().size() <= 100_000 && s.moves().size() <= 100_000, "History too large");
        long previousTurn = 1;
        int moveIndex = 0, currentMoves = 0, currentAbilities = 0;
        Set<Long> closed = new HashSet<>(), moved = new HashSet<>(), cast = new HashSet<>();
        for (int i = 0; i < s.events().size(); i++) {
            GameEvent event = s.events().get(i);
            require(event.sequence() == i + 1L && event.turn() >= previousTurn && event.turn() <= s.turn().number()
                    && event.kind() != null && side(event.color()) && event.message() != null && event.message().length() <= 4096, "Invalid event sequence");
            previousTurn = event.turn();
            if (event.actorId() != null) require(s.pieces().containsKey(event.actorId()), "Unknown event actor");
            if (event.targetId() != null) require(s.pieces().containsKey(event.targetId()), "Unknown event target");
            if (event.kind() != GameEvent.Kind.END_TURN)
                require(event.actorId() != null && inside(event.from()) && inside(event.to()), "Incomplete action event");
            if (event.kind() != GameEvent.Kind.COUNTER_CAPTURE && event.kind() != GameEvent.Kind.BOG_CAPTURE && event.kind() != GameEvent.Kind.YAGA_BIRTH && event.kind() != GameEvent.Kind.ARES_REVENGE && event.kind()!=GameEvent.Kind.ILLUSION_HIT && event.kind()!=GameEvent.Kind.MINE_TRIGGER && event.kind()!=GameEvent.Kind.MINE_CAPTURE && event.kind()!=GameEvent.Kind.BLAST_CAPTURE && event.kind()!=GameEvent.Kind.BLAST_DEATH && event.kind()!=GameEvent.Kind.LIGHTNING_CAPTURE) {
                FigureColor expected = (s.turn().number() - event.turn()) % 2 == 0 ? s.active() : MoveValidator.opposite(s.active());
                require(event.color() == expected && !closed.contains(event.turn()), "Action outside its active turn");
            }
            if (event.kind() == GameEvent.Kind.MOVE) {
                moved.add(event.turn());
                require(moveIndex < s.moves().size(), "Duplicate or missing move");
                MadeMove move = s.moves().get(moveIndex++);
                require(move.getPly() == moveIndex && move.getColor() == event.color() && move.getFrom().equals(event.from())
                        && move.getTo().equals(event.to()) && move.getKind() != null && move.getFigure() != null, "Move differs from action history");
                require(move.getPromotion() == null || move.getKind() == MoveKind.PROMOTION, "Unexpected promotion");
                require((move.getCapturedFigure() == null) == (move.getCaptureSquare() == null), "Incomplete capture description");
                require(move.getClock() != null, "Missing move clock snapshot");
                validateClock(move.getClock(), move.getClock().activeColor(), !move.getClock().running());
                if (move.getCaptureSquare() != null) require(inside(move.getCaptureSquare()), "Invalid capture square");
                if (event.turn() == s.turn().number()) currentMoves++;
            } else if (event.kind() == GameEvent.Kind.ABILITY_USED) {
                require(s.mode() == GameMode.MOBA && cast.add(event.turn())
                        && event.ability().eventTarget().accepts(event.targetId()), "Invalid ability event");
                if (event.turn() == s.turn().number()) currentAbilities++;
            } else if (event.kind() == GameEvent.Kind.END_TURN) {
                require(s.mode() == GameMode.MOBA && moved.contains(event.turn()) && event.turn() < s.turn().number()
                        && closed.add(event.turn()) && event.actorId() == null && event.targetId() == null, "Invalid turn end");
            } else require(s.mode() == GameMode.MOBA && event.targetId() != null, "Invalid reaction event");
        }
        require(moveIndex == s.moves().size(), "Unmatched move history");
        if (s.mode() == GameMode.MOBA) {
            require(closed.size() == s.turn().number() - 1, "Missing turn boundaries");
            require(s.turn().moveUsed() == (currentMoves > 0) && s.turn().abilityUsed() == (currentAbilities == 1), "Budget differs from history");
        } else
            require(!s.turn().moveUsed() && !s.turn().abilityUsed() && s.turn().number() == s.moves().size() + 1L, "Invalid classic turn budget");
    }

    private static void validateCaptures(GameSnapshot s, List<Figure> captures, FigureColor victimSide, long points) {
        var expected = new HashMap<Class<? extends Figure>, Integer>();
        for (PieceState piece : s.pieces().values())
            if (piece.status() == PieceState.Status.CAPTURED && piece.color() == victimSide)
                expected.merge(piece.type(), 1, Integer::sum);
        var actual = new HashMap<Class<? extends Figure>, Integer>();
        long sum = 0;
        for (Figure figure : captures) {
            require(figure != null && Field.supports(figure.getClass()) && figure.getColor() == victimSide && !(figure instanceof Empty) && !(figure instanceof King)
                    && inside(figure.getPosition()), "Invalid captured figure");
            actual.merge(figure.getClass(), 1, Integer::sum);
            sum += figure.getPoints();
        }
        require(expected.equals(actual) && points == sum, "Capture list or points differs from piece state");
    }

    static void validateClock(ClockSnapshot clock, FigureColor active, boolean finished) {
        require(clock != null && side(clock.activeColor()) && clock.whiteNanos() >= 0 && clock.blackNanos() >= 0
                && clock.whiteNanos() <= Integer.MAX_VALUE * 1_000_000_000L && clock.blackNanos() <= Integer.MAX_VALUE * 1_000_000_000L, "Invalid clock");
        require(clock.enabled() ? clock.running() == !finished && clock.activeColor() == active
                : !clock.running() && clock.whiteNanos() == 0 && clock.blackNanos() == 0, "Invalid clock state");
        if (clock.running()) require(clock.whiteNanos() > 0 && clock.blackNanos() > 0, "Running clock has zero time");
    }

    private static boolean side(FigureColor color) {
        return color == FigureColor.WHITE || color == FigureColor.BLACK;
    }

    private static boolean inside(Position p) {
        return p != null && p.getRow() >= 0 && p.getRow() < 8 && p.getColumn() >= 0 && p.getColumn() < 8;
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
