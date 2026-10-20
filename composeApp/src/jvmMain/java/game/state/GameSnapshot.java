package game.state;

import game.heroes.pudge.PudgeRules;
import game.draft.DraftSnapshot;
import game.summoning.SummonState;
import game.heroes.ginseng.SpellState;
import game.heroes.faun.FaunState;
import game.heroes.bolotnik.BolotnikRules;
import game.terrain.TerrainState;
import game.economy.EconomyState;
import game.heroes.bountycrow.BountyCrowRules;
import game.heroes.HeroRules;
import game.heroes.lumeshard.LumeshardRules;
import game.history.GameEvent;
import game.history.MadeMove;

import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;

import java.util.List;
import java.util.Map;

/**
 * Coherent immutable engine snapshot; GameSaveFile persists it, Game.fromSnapshot restores it.
 */
public record GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,
                           TurnState turn, FigureColor active, CastlingRights castlingRights,
                           Position enPassantTarget, int halfmoveClock, Map<String, Integer> repetitions,
                           ClockSnapshot clock, GameOutcome outcome, List<MadeMove> moves, List<GameEvent> events,
                           long whitePoints, long blackPoints, List<Figure> whiteCaptures, List<Figure> blackCaptures,
                           FigureColor drawOffer, HeroRules heroRules, EconomyState economy, TerrainState terrain,
                           DraftSnapshot draft, SummonState summons, SpellState spells, FaunState faun, game.heroes.clownganger.ClownState clown, game.heroes.moleminer.MineState mines, game.heroes.zeus.LightningState lightning) {
    public GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,                            TurnState turn, FigureColor active, CastlingRights castlingRights,                            Position enPassantTarget, int halfmoveClock, Map<String, Integer> repetitions,                            ClockSnapshot clock, GameOutcome outcome, List<MadeMove> moves, List<GameEvent> events,                            long whitePoints, long blackPoints, List<Figure> whiteCaptures, List<Figure> blackCaptures,                            FigureColor drawOffer, HeroRules heroRules, EconomyState economy, TerrainState terrain, DraftSnapshot draft, SummonState summons, SpellState spells, FaunState faun, game.heroes.clownganger.ClownState clown, game.heroes.moleminer.MineState mines){
        this(version,mode,field,pieces,turn,active,castlingRights,enPassantTarget,halfmoveClock,repetitions,clock,outcome,moves,events,whitePoints,blackPoints,whiteCaptures,blackCaptures,drawOffer,heroRules,economy,terrain,draft,summons,spells,faun,clown,mines,game.heroes.zeus.LightningState.fromHistory(events,heroRules.zeus()).clean(pieces));
    }
    public GameSnapshot {
        pieces = Map.copyOf(pieces);
        repetitions = Map.copyOf(repetitions);
        moves = List.copyOf(moves);
        events = List.copyOf(events);
        whiteCaptures = List.copyOf(whiteCaptures);
        blackCaptures = List.copyOf(blackCaptures);
    }
    public GameSnapshot(long version,GameMode mode,Field field,Map<Long,PieceState> pieces,TurnState turn,FigureColor active,CastlingRights castlingRights,Position enPassantTarget,int halfmoveClock,Map<String,Integer> repetitions,ClockSnapshot clock,GameOutcome outcome,List<MadeMove> moves,List<GameEvent> events,long whitePoints,long blackPoints,List<Figure> whiteCaptures,List<Figure> blackCaptures,FigureColor drawOffer,HeroRules heroRules,EconomyState economy,TerrainState terrain,DraftSnapshot draft,SummonState summons,SpellState spells,FaunState faun,game.heroes.clownganger.ClownState clown){
        this(version,mode,field,pieces,turn,active,castlingRights,enPassantTarget,halfmoveClock,repetitions,clock,outcome,moves,events,whitePoints,blackPoints,whiteCaptures,blackCaptures,drawOffer,heroRules,economy,terrain,draft,summons,spells,faun,clown,game.heroes.moleminer.MineState.fromHistory(events));
    }
    public GameSnapshot(long version,GameMode mode,Field field,Map<Long,PieceState> pieces,TurnState turn,FigureColor active,CastlingRights castlingRights,Position enPassantTarget,int halfmoveClock,Map<String,Integer> repetitions,ClockSnapshot clock,GameOutcome outcome,List<MadeMove> moves,List<GameEvent> events,long whitePoints,long blackPoints,List<Figure> whiteCaptures,List<Figure> blackCaptures,FigureColor drawOffer,HeroRules heroRules,EconomyState economy,TerrainState terrain,DraftSnapshot draft,SummonState summons,SpellState spells,FaunState faun){
        this(version,mode,field,pieces,turn,active,castlingRights,enPassantTarget,halfmoveClock,repetitions,clock,outcome,moves,events,whitePoints,blackPoints,whiteCaptures,blackCaptures,drawOffer,heroRules,economy,terrain,draft,summons,spells,faun,game.heroes.clownganger.ClownState.fromHistory(events,heroRules.clownganger()));
    }

    public GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,
                        TurnState turn, FigureColor active, CastlingRights castlingRights, Position enPassantTarget,
                        int halfmoveClock, Map<String, Integer> repetitions, ClockSnapshot clock, GameOutcome outcome,
                        List<MadeMove> moves, List<GameEvent> events, long whitePoints, long blackPoints, List<Figure> whiteCaptures, List<Figure> blackCaptures,
                        FigureColor drawOffer, HeroRules heroRules, EconomyState economy, TerrainState terrain, DraftSnapshot draft, SummonState summons, SpellState spells) {
        this(version, mode, field, pieces, turn, active, castlingRights, enPassantTarget, halfmoveClock, repetitions, clock, outcome, moves, events, whitePoints, blackPoints, whiteCaptures, blackCaptures, drawOffer, heroRules, economy, terrain, draft, summons, spells, FaunState.fromHistory(events, heroRules.faun()));
    }

    public GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,
                        TurnState turn, FigureColor active, CastlingRights castlingRights, Position enPassantTarget,
                        int halfmoveClock, Map<String, Integer> repetitions, ClockSnapshot clock, GameOutcome outcome,
                        List<MadeMove> moves, List<GameEvent> events, long whitePoints, long blackPoints,
                        List<Figure> whiteCaptures, List<Figure> blackCaptures, FigureColor drawOffer, HeroRules heroRules,
                        EconomyState economy, TerrainState terrain, DraftSnapshot draft, SummonState summons) {
        this(version, mode, field, pieces, turn, active, castlingRights, enPassantTarget, halfmoveClock, repetitions, clock, outcome, moves, events, whitePoints, blackPoints, whiteCaptures, blackCaptures, drawOffer, heroRules, economy, terrain, draft, summons, SpellState.fromHistory(events));
    }

    public GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,
                        TurnState turn, FigureColor active, CastlingRights castlingRights, Position enPassantTarget,
                        int halfmoveClock, Map<String, Integer> repetitions, ClockSnapshot clock, GameOutcome outcome,
                        List<MadeMove> moves, List<GameEvent> events, long whitePoints, long blackPoints,
                        List<Figure> whiteCaptures, List<Figure> blackCaptures, FigureColor drawOffer, HeroRules heroRules,
                        EconomyState economy, TerrainState terrain, DraftSnapshot draft) {
        this(version, mode, field, pieces, turn, active, castlingRights, enPassantTarget, halfmoveClock, repetitions, clock, outcome, moves, events, whitePoints, blackPoints, whiteCaptures, blackCaptures, drawOffer, heroRules, economy, terrain, draft, SummonState.empty());
    }

    /**
     * Compatibility constructor for games created without a draft.
     */
    public GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,
                        TurnState turn, FigureColor active, CastlingRights castlingRights, Position enPassantTarget,
                        int halfmoveClock, Map<String, Integer> repetitions, ClockSnapshot clock, GameOutcome outcome,
                        List<MadeMove> moves, List<GameEvent> events, long whitePoints, long blackPoints,
                        List<Figure> whiteCaptures, List<Figure> blackCaptures, FigureColor drawOffer, HeroRules heroRules,
                        EconomyState economy, TerrainState terrain) {
        this(version, mode, field, pieces, turn, active, castlingRights, enPassantTarget, halfmoveClock, repetitions,
                clock, outcome, moves, events, whitePoints, blackPoints, whiteCaptures, blackCaptures, drawOffer, heroRules, economy, terrain, null);
    }

    public GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,
                        TurnState turn, FigureColor active, CastlingRights castlingRights, Position enPassantTarget,
                        int halfmoveClock, Map<String, Integer> repetitions, ClockSnapshot clock, GameOutcome outcome,
                        List<MadeMove> moves, List<GameEvent> events, long whitePoints, long blackPoints,
                        List<Figure> whiteCaptures, List<Figure> blackCaptures, FigureColor drawOffer, HeroRules heroRules, EconomyState economy) {
        this(version, mode, field, pieces, turn, active, castlingRights, enPassantTarget, halfmoveClock, repetitions,
                clock, outcome, moves, events, whitePoints, blackPoints, whiteCaptures, blackCaptures, drawOffer, heroRules, economy, TerrainState.empty());
    }

    public BolotnikRules bolotnikRules() {
        return heroRules.bolotnik();
    }

    /**
     * Compatibility constructor for snapshots from before the economy extension.
     */
    public GameSnapshot(long version, GameMode mode, Field field, Map<Long, PieceState> pieces,
                        TurnState turn, FigureColor active, CastlingRights castlingRights,
                        Position enPassantTarget, int halfmoveClock, Map<String, Integer> repetitions,
                        ClockSnapshot clock, GameOutcome outcome, List<MadeMove> moves, List<GameEvent> events,
                        long whitePoints, long blackPoints, List<Figure> whiteCaptures, List<Figure> blackCaptures,
                        FigureColor drawOffer, HeroRules heroRules) {
        this(version, mode, field, pieces, turn, active, castlingRights, enPassantTarget, halfmoveClock, repetitions,
                clock, outcome, moves, events, whitePoints, blackPoints, whiteCaptures, blackCaptures, drawOffer, heroRules, EconomyState.disabled());
    }

    public BountyCrowRules crowRules() {
        return heroRules.crow();
    }

    public PudgeRules pudgeRules() {
        return heroRules.pudge();
    }

    public LumeshardRules lumeshardRules() {
        return heroRules.lumeshard();
    }

    public PieceState pieceAt(Position square) {
        return pieces.values().stream().filter(piece -> piece.status() == PieceState.Status.BOARD
                && piece.position().equals(square)).findFirst().orElse(null);
    }
}
