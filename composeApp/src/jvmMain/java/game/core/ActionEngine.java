package game.core;

import game.heroes.zeus.*;
import game.heroes.clownganger.*;
import game.heroes.moleminer.*;
import game.heroes.Ability;
import game.heroes.AbilityRequest;
import game.heroes.ginseng.*;
import game.heroes.faun.*;
import game.heroes.ares.AresAbilities;
import game.summoning.*;
import game.economy.*;
import game.terrain.*;
import rules.moba.TerrainGeometry;
import game.heroes.HeroAbilities;
import game.heroes.HeroEffect;
import game.heroes.HeroRules;
import rules.moba.AttackRules;
import java.util.Set;
import game.heroes.pudge.PudgeRules;
import game.moves.AvailableMoves;
import game.moves.Capture;
import game.moves.MoveResult;
import game.moves.Promotion;
import game.state.CastlingRights;
import game.state.GameMode;
import game.state.PieceState;

import model.*;
import model.figures.*;
import rules.standard.MoveValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure previews. No live Game, Player, clock, or history is modified here. */
final class ActionEngine {
    record Context(Field field, Map<Long, PieceState> pieces, CastlingRights rights, Position ep, EconomyState economy, long turn, TerrainState terrain, SummonState summons, SpellState spells, FaunState faun, ClownState clown, MineState mines, LightningState lightning) {
        Context(Field field,Map<Long,PieceState> pieces,CastlingRights rights,Position ep,EconomyState economy,long turn,TerrainState terrain,SummonState summons,SpellState spells,FaunState faun,ClownState clown,MineState mines){this(field,pieces,rights,ep,economy,turn,terrain,summons,spells,faun,clown,mines,LightningState.empty());}
        Context(Field field,Map<Long,PieceState> pieces,CastlingRights rights,Position ep,EconomyState economy,long turn,TerrainState terrain,SummonState summons,SpellState spells,FaunState faun,ClownState clown){this(field,pieces,rights,ep,economy,turn,terrain,summons,spells,faun,clown,MineState.empty());}
        Context(Field field,Map<Long,PieceState> pieces,CastlingRights rights,Position ep,EconomyState economy,long turn,TerrainState terrain,SummonState summons,SpellState spells,FaunState faun){this(field,pieces,rights,ep,economy,turn,terrain,summons,spells,faun,ClownState.empty());}
        Context(Field field,Map<Long,PieceState> pieces,CastlingRights rights,Position ep,EconomyState economy,long turn,TerrainState terrain,SummonState summons,SpellState spells) {
            this(field,pieces,rights,ep,economy,turn,terrain,summons,spells,FaunState.empty());
        }
        Context(Field field, Map<Long, PieceState> pieces, CastlingRights rights, Position ep, EconomyState economy, long turn, TerrainState terrain, SummonState summons) {
            this(field,pieces,rights,ep,economy,turn,terrain,summons,SpellState.empty());
        }
        Context(Field field, Map<Long, PieceState> pieces, CastlingRights rights, Position ep, EconomyState economy, long turn, TerrainState terrain) {
            this(field,pieces,rights,ep,economy,turn,terrain,SummonState.empty());
        }
        Context(Field field, Map<Long, PieceState> pieces, CastlingRights rights, Position ep) {
            this(field, pieces, rights, ep, EconomyState.disabled(), 1, TerrainState.empty());
        }
        Context { pieces = Map.copyOf(pieces); }
        PieceState at(Position square) {
            return pieces.values().stream().filter(piece -> piece.status() == PieceState.Status.BOARD
                    && piece.position().equals(square)).findFirst().orElse(null);
        }
    }
    record Preview(MoveResult result, Context next, List<Capture> captures) {
        Preview { captures = List.copyOf(captures); }
        static Preview rejected(MoveResult result) { return new Preview(result, null, List.of()); }
    }
    private ActionEngine() {}

    static Preview move(Context context, FigureColor side, Position from, Position to, Promotion promotion,
                        GameMode mode, HeroRules rules) {
        PieceState source=context.at(from);
        if(source!=null && source.color()==side && source.isPotted())return Preview.rejected(MoveResult.POTTED);
        if(mode==GameMode.MOBA && source!=null && context.clown().movementTurn()==context.turn() && !context.clown().canMove(source.id(),context.turn(),true,context.pieces()))return Preview.rejected(MoveResult.MOVE_ALREADY_USED);
        if(source!=null && source.color()==side && source.isBoxed())return Preview.rejected(MoveResult.BOXED);
        if(source!=null && source.color()==side && source.isStunned())return Preview.rejected(MoveResult.STUNNED);
        MoveResult validation = MoveValidator.validate(context.field(), side, from, to, context.rights(), context.ep(), promotion, mode == GameMode.MOBA ? AttackRules.blindSquares(context.pieces()) : Set.of(),
                context.terrain().hostileSquares(context.pieces(), side), context.terrain().hostileSquares(context.pieces(), MoveValidator.opposite(side)), mode != GameMode.MOBA);
        if (!validation.isSuccess()) return Preview.rejected(validation);
        Field field = context.field(); var oldBoard = field.getGameField();
        Figure figure = oldBoard.get(from);
        PieceState moving = context.at(from);
        boolean castle = MoveValidator.isCastling(figure, from, to);
        if(castle && context.at(MoveValidator.rookSource(from,to)).isStunned())return Preview.rejected(MoveResult.STUNNED);
        if(figure instanceof King && (context.mines().hostile(to,context.pieces(),side) || castle && context.mines().hostile(new Position(from.getRow(),from.getColumn()+Integer.signum(to.getColumn()-from.getColumn())),context.pieces(),side)))return Preview.rejected(MoveResult.KING_IMMUNE);
        boolean ep = MoveValidator.isEnPassant(oldBoard, side, from, to, context.ep());
        Position captureSquare = ep ? new Position(from.getRow(), to.getColumn()) : to;
        Figure victim = oldBoard.get(captureSquare);
        PieceState victimState = context.at(captureSquare);
        if(victimState!=null && victimState.isPotted())return Preview.rejected(MoveResult.POTTED);
        if(castle && context.at(MoveValidator.rookSource(from,to)).isPotted())return Preview.rejected(MoveResult.POTTED);
        if(victimState!=null && victimState.isBoxed())return Preview.rejected(MoveResult.BOXED);
        if(moving.hero()==game.heroes.HeroType.ILLUSION && victimState!=null)return Preview.rejected(MoveResult.ILLUSION_CANNOT_CAPTURE);
        if(castle && context.at(MoveValidator.rookSource(from,to)).isBoxed())return Preview.rejected(MoveResult.BOXED);
        if(victimState!=null && victimState.hero()==game.heroes.HeroType.ILLUSION){
            if(moving.isBlinded())return Preview.rejected(MoveResult.BLINDED_CANNOT_CAPTURE);
            Context hit=new Context(context.field(),context.pieces(),context.rights(),null,context.economy(),context.turn(),context.terrain(),context.summons(),context.spells(),context.faun(),context.clown().hit(victimState.id()),context.mines(),context.lightning());
            if(AttackRules.isInCheck(hit.field().getGameField(),hit.pieces(),side,hit.terrain()))return Preview.rejected(MoveResult.KING_IN_CHECK);
            return recordMove(new Preview(MoveResult.SUCCESS,hit,List.of()),moving.id());
        }
        if (mode == GameMode.MOBA && moving.isBlinded() && victimState != null)
            return Preview.rejected(MoveResult.BLINDED_CANNOT_CAPTURE);
        var board = new HashMap<>(oldBoard);
        var states = new HashMap<>(context.pieces());
        var captures = new ArrayList<Capture>();
        CastlingRights rights = rightsAfter(context.rights(), figure, from,
                victimState == null ? null : victim, captureSquare);
        Class<? extends Figure> newType = promotion == null ? figure.getClass() : promotion.getFigureType();
        board.put(from, field.createFigure(Empty.class, from, FigureColor.NONE));
        board.put(to, field.createFigure(newType, to, side));
        states.put(moving.id(), moving.placed(to, newType));
        if (victimState != null) {
            states.put(victimState.id(), victimState.hidden(PieceState.Status.CAPTURED));
            captures.add(new Capture(moving.id(), victimState.id(), side, victim, captureSquare, false));
            states.put(moving.id(), HeroAbilities.afterCapture(states.get(moving.id()), rules));
            if (ep) board.put(captureSquare, field.createFigure(Empty.class, captureSquare, FigureColor.NONE));
            if (mode == GameMode.MOBA) {
                HeroEffect reaction = HeroAbilities.captureReaction(field, states, victimState, moving.id(), to, rules);
                if (!reaction.result().isSuccess()) return Preview.rejected(reaction.result());
                rights = applyEffect(field, board, states, rights, reaction);
                captures.addAll(reaction.captures());
            }
        }
        if (castle) {
            Position rookFrom = MoveValidator.rookSource(from, to), rookTo = MoveValidator.rookDestination(from, to);
            PieceState rook = context.at(rookFrom);
            board.put(rookFrom, field.createFigure(Empty.class, rookFrom, FigureColor.NONE));
            board.put(rookTo, field.createFigure(Rook.class, rookTo, side));
            states.put(rook.id(), rook.placed(rookTo, Rook.class));
        }
        Position nextEp = figure instanceof Pawn && Math.abs(to.getRow() - from.getRow()) == 2
                ? new Position((from.getRow() + to.getRow()) / 2, from.getColumn()) : null;
        var preview=finish(context, states, rights, nextEp, captures, context.terrain(), List.of(), side, mode, rules);
        return mode==GameMode.MOBA?recordMove(preview,moving.id()):preview;
    }

    private static Preview recordMove(Preview preview,long actor){
        if(!preview.result().isSuccess())return preview;
        var n=preview.next();
        return new Preview(preview.result(),new Context(n.field(),n.pieces(),n.rights(),n.ep(),n.economy(),n.turn(),n.terrain(),n.summons(),n.spells(),n.faun(),n.clown().move(actor,n.turn()),n.mines(),n.lightning()),preview.captures());
    }
    static MoveResult abilityReady(Context context, FigureColor side, Position actorSquare, Ability ability, HeroRules rules) {
        return abilityReady(context,side,actorSquare,ability,rules,false);
    }
    static MoveResult abilityReady(Context context, FigureColor side, Position actorSquare, Ability ability, HeroRules rules,boolean borrowed) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if (!MoveValidator.inside(context.field(), actorSquare)) return MoveResult.OUT_OF_BOUNDS;
        PieceState actor = context.at(actorSquare);
        if (actor == null) return MoveResult.EMPTY_SOURCE;
        if (actor.color() != side) return MoveResult.WRONG_FIGURE_COLOR;
        if(actor.isPotted())return MoveResult.POTTED;
        if(actor.isBoxed())return MoveResult.BOXED;
        if(actor.isStunned())return MoveResult.STUNNED;
        if(context.faun().silenced(actorSquare))return MoveResult.SILENCED;
        MoveResult ready;
        if(borrowed) {
            var stored=context.spells().borrowed().get(actor.id());
            if(actor.hero()!=game.heroes.HeroType.GINSENG_BISHOP || stored==null || stored.ability()!=ability)return MoveResult.NO_BORROWED_ABILITY;
            if(!stored.isReady(context.turn()))return MoveResult.BORROWED_NOT_READY;
            ready=BorrowedEffects.availability(actor,ability,rules);
        } else ready=HeroAbilities.availability(actor,ability,rules);
        if (!ready.isSuccess()) return ready;
        if(ability==Ability.THUNDERSTORM && ZeusAbilities.charged(context.pieces(),context.lightning(),actor).isEmpty())return MoveResult.NO_CHARGED_TARGETS;
        if ((ability == Ability.BOUNTY || ability == Ability.STEAL) && !context.economy().enabled()) return MoveResult.ECONOMY_DISABLED;
        if (ability == Ability.STEAL && context.economy().balanceHalves(MoveValidator.opposite(side)) == 0) return MoveResult.NO_GOLD_TO_STEAL;
        return MoveResult.SUCCESS;
    }

    static Preview ability(Context context, FigureColor side, Position actorSquare, Position targetSquare,
                           Ability ability, HeroRules rules) {
        return ability(context,side,new AbilityRequest(actorSquare,ability,targetSquare),rules);
    }
    static Preview ability(Context context,FigureColor side,AbilityRequest request,HeroRules rules) {
        if(request==null)return Preview.rejected(MoveResult.INVALID_ABILITY);
        Position actorSquare=request.actor(),targetSquare=request.target();Ability ability=request.ability();
        MoveResult ready = abilityReady(context, side, actorSquare, ability, rules,request.borrowed());
        if (!ready.isSuccess()) return Preview.rejected(ready);
        if(ability!=Ability.DISPLACE && request.destination()!=null)return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if (ability == Ability.STEAL || ability == Ability.AMBUSH || ability == Ability.SUMMON_YAGA || ability==Ability.INVISIBILITY || ability==Ability.PLANT_MINE || ability==Ability.THUNDERSTORM || ability==Ability.EVOLVE) {
            if (targetSquare != null) return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
        } else {
            if (!MoveValidator.inside(context.field(), targetSquare)) return Preview.rejected(MoveResult.OUT_OF_BOUNDS);
            if (actorSquare.equals(targetSquare) && ability != Ability.MUD && ability!=Ability.DISPLACE && ability!=Ability.COPY_SPELL && ability!=Ability.CLOUD && ability!=Ability.BOX && ability!=Ability.DETONATE) return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
        }
        PieceState actor = context.at(actorSquare), target = context.at(targetSquare);
        if(ability!=Ability.CLOUD && ability!=Ability.SPEAR && ability!=Ability.DETONATE && context.faun().hidden(target,side))return Preview.rejected(MoveResult.TARGET_INVISIBLE);
        if (ability!=Ability.CLOUD && ability!=Ability.SPEAR && ability!=Ability.BOX && ability!=Ability.GRAPPLE && target != null && target.type() == King.class) return Preview.rejected(MoveResult.KING_IMMUNE);
        if(ability==Ability.SWALLOW && target!=null && target.hero()==game.heroes.HeroType.ILLUSION)return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
        if(target!=null && target.isPotted() && !List.of(Ability.HOOK,Ability.DISPLACE,Ability.SPEAR,Ability.GRAPPLE,Ability.CLOUD,Ability.MUD).contains(ability))return Preview.rejected(MoveResult.POTTED);
        if(target!=null && target.isBoxed() && ability!=Ability.SPEAR && ability!=Ability.CLOUD)return Preview.rejected(MoveResult.BOXED);
        HeroEffect effect; ZeusAbilities.Result zeus=null;
        Ability copied=ability==Ability.COPY_SPELL && target!=null?context.spells().lastUsed().get(target.id()):null;
        if(ability==Ability.PLANT_MINE && context.mines().cells().containsKey(actorSquare))return Preview.rejected(MoveResult.TERRAIN_OCCUPIED);
        if(ability==Ability.YARN || ability==Ability.CLAY_POT)effect=game.heroes.kikimora.KikimoraAbilities.preview(context.pieces(),actor,target,ability,context.turn(),rules.kikimora(),request.borrowed());
        else if(ability==Ability.CHAIN_LIGHTNING || ability==Ability.THUNDERSTORM){zeus=ZeusAbilities.cast(context.field(),context.pieces(),actor,target,ability,context.lightning(),context.summons(),context.turn(),rules,request.borrowed());effect=zeus.effect();}
        else if(ability==Ability.PLANT_MINE || ability==Ability.DETONATE)effect=MoleminerAbilities.preview(actor,targetSquare,ability,rules.moleminer(),request.borrowed());
        else if(ability==Ability.BOX || ability==Ability.ILLUSION)effect=ClowngangerAbilities.preview(context.field(),actor,target,targetSquare,ability,context.turn(),rules.clownganger(),request.borrowed());
        else if(ability==Ability.SPEAR)effect=AresAbilities.spear(context.field(),context.pieces(),actor,targetSquare,context.turn(),rules.ares(),context.terrain(),request.borrowed());
        else if(ability==Ability.DISPLACE)effect=GinsengAbilities.displace(context.field(),actor,target,request.destination(),rules.ginseng(),request.borrowed());
        else if(ability==Ability.COPY_SPELL)effect=GinsengAbilities.copy(actor,target,copied,rules.ginseng(),request.borrowed());
        else effect=request.borrowed()?BorrowedEffects.preview(context.field(),actor,target,targetSquare,ability,rules):HeroAbilities.preview(context.field(), actor, target, targetSquare, ability, rules);
        if (!effect.result().isSuccess()) return Preview.rejected(effect.result());
        Field field = context.field(); var board = new HashMap<>(field.getGameField());
        var states = new HashMap<>(context.pieces());
        CastlingRights rights = applyEffect(field, board, states, context.rights(), effect);
        if (ability == Ability.HOOK && TerrainGeometry.blocksRay(target.position(), states.get(target.id()).position(),
                context.terrain().hostileSquares(context.pieces(), target.color()))) return Preview.rejected(MoveResult.MUD_BLOCKED);
        if(ability==Ability.GRAPPLE && TerrainGeometry.blocksRay(actor.position(),effect.updates().get(actor.id()).position(),context.terrain().hostileSquares(context.pieces(),actor.color())))return Preview.rejected(MoveResult.MUD_BLOCKED);
        TerrainResolver.Placement placement = TerrainResolver.place(context.terrain(), context.pieces(), actor, targetSquare, ability, context.turn(), rules);
        if (!placement.result().isSuccess()) return Preview.rejected(placement.result());
        SummonState summons=zeus==null?context.summons():zeus.summons();
        if(ability==Ability.SUMMON_YAGA) {
            var summoned=SummonResolver.cast(states,summons,actor,context.turn(),side,rules,placement.terrain());
            if(!summoned.result().isSuccess())return Preview.rejected(summoned.result());
            states=new HashMap<>(summoned.pieces());summons=summoned.summons();
        }
        ClownState clown=context.clown();
        if(zeus!=null)for(long id:zeus.illusionHits())clown=clown.hit(id);
        if(ability==Ability.ILLUSION){
            if(states.size()>=SummonState.MAX_IDENTITIES)return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
            for(var i:clown.illusions().values())if(i.sourceId()==actor.id() && !states.get(i.id()).isRemoved())return Preview.rejected(MoveResult.INVALID_ABILITY_TARGET);
            long id=Math.addExact(states.keySet().stream().mapToLong(Long::longValue).max().orElse(0),1);
            var choice=summons.choose(2);summons=new SummonState(true,choice.nextState(),summons.records());
            Position bornAt=choice.index()==0?actor.position():targetSquare;
            if(choice.index()==0)states.put(actor.id(),states.get(actor.id()).placed(targetSquare,actor.type()));
            states.put(id,new PieceState(id,actor.type(),actor.color(),bornAt,PieceState.Status.BOARD,game.heroes.HeroType.ILLUSION,0,0,java.util.List.of(game.state.ItemSlot.empty(),game.state.ItemSlot.empty(),game.state.ItemSlot.empty()),null,actor.blindTurns(),null));
            clown=clown.create(id,actor.id(),bornAt,context.turn(),rules.clownganger());
        }
        SpellState spells=context.spells().cast(actor.id(),ability,request.borrowed(),context.turn(),target==null?null:target.id(),copied);
        MineState mines=context.mines();if(ability==Ability.PLANT_MINE)mines=mines.plant(actor.id(),actorSquare,context.turn());
        Context preparedContext=new Context(context.field(),context.pieces(),context.rights(),context.ep(),context.economy(),context.turn(),context.terrain(),summons,spells,context.faun().cast(actor.id(),ability,targetSquare,context.turn(),rules.faun()),clown,mines,zeus==null?context.lightning():zeus.lightning());
        Preview preview = finish(preparedContext, states, rights, context.ep(), effect.captures(), placement.terrain(), placement.immediateEntries(), side, GameMode.MOBA, rules,ability==Ability.DETONATE?actor.id():null,targetSquare);
        if (!preview.result().isSuccess()) return preview;
        EconomyState economy = preview.next().economy();
        if (ability == Ability.BOUNTY) economy = assignBounty(context, actor, target, rules);
        if (ability == Ability.STEAL) economy = economy.apply(new GoldChange(context.turn(), GoldChange.Kind.STEAL, side, actor.id(), null,
                Math.min(4, economy.balanceHalves(MoveValidator.opposite(side))), 0, 0));
        Context next = preview.next();
        return new Preview(MoveResult.SUCCESS, new Context(next.field(), next.pieces(), next.rights(), next.ep(), economy, context.turn(), next.terrain(), next.summons(),next.spells(),next.faun(),next.clown(),next.mines(),next.lightning()), preview.captures());
    }

    private static EconomyState assignBounty(Context context, PieceState actor, PieceState target, HeroRules rules) {
        // Exclude the casting turn; each following owner turn is two individual player turns later.
        return context.economy().withMark(new BountyMark(actor.id(), target.id(),
                Math.addExact(context.turn(), 2L * rules.crow().bountyTurns())));
    }

    private static Preview finish(Context before, Map<Long, PieceState> states, CastlingRights rights, Position ep,
                                  List<Capture> captures, TerrainState terrain, List<Long> immediate, FigureColor side, GameMode mode, HeroRules rules) {
        return finish(before,states,rights,ep,captures,terrain,immediate,side,mode,rules,null,null);
    }
    private static Preview finish(Context before,Map<Long,PieceState> states,CastlingRights rights,Position ep,List<Capture> captures,TerrainState terrain,List<Long> immediate,FigureColor side,GameMode mode,HeroRules rules,Long bomber,Position landing){
        var resolved=EffectResolver.resolve(before.field(),before.pieces(),states,terrain,before.summons(),before.mines(),captures,immediate,before.turn(),side,rules,bomber,landing);
        if(!resolved.result().isSuccess())return Preview.rejected(resolved.result());
        ClownState clown=before.clown();
        for(long id:resolved.illusionHits())clown=clown.hit(id);
        var cleaned=clown.clean(resolved.pieces(),0);
        var board = new HashMap<Position, Figure>();
        for (Position square : before.field().getGameField().keySet())
            board.put(square, before.field().createFigure(Empty.class, square, FigureColor.NONE));
        for (PieceState piece : cleaned.values()) if (piece.status() == PieceState.Status.BOARD)
            board.put(piece.position(), before.field().createFigure(piece.type(), piece.position(), piece.color()));
        for (PieceState old : before.pieces().values()) {
            PieceState next = resolved.pieces().get(old.id());
            if (old.position() != null && (next.position() == null || !old.position().equals(next.position()) || next.isPotted()))
                rights = removedRookRights(rights, old, old.position());
        }
        var kings=cleaned.values().stream().filter(p->p.position()!=null && p.type()==King.class).toList();
        if(kings.size()==2 && Math.max(Math.abs(kings.get(0).position().getRow()-kings.get(1).position().getRow()),Math.abs(kings.get(0).position().getColumn()-kings.get(1).position().getColumn()))<=1)return Preview.rejected(MoveResult.KING_IN_CHECK);
        if (AttackRules.isInCheck(board, cleaned, side, resolved.terrain())) return Preview.rejected(MoveResult.KING_IN_CHECK);
        Field next = mode == GameMode.MOBA ? Field.fromMobaPosition(board) : Field.fromPosition(board);
        EconomyState economy = GoldEngine.captures(before.economy(), resolved.captures(), before.pieces(), resolved.pieces(), rules.gold(), before.turn());
        for(PieceState p:cleaned.values())if(p.isRemoved())economy=economy.removeMark(p.id());
        return new Preview(MoveResult.SUCCESS, new Context(next, cleaned, rights, validEp(board, ep,cleaned), economy, before.turn(), resolved.terrain(), resolved.summons(),before.spells(),before.faun().afterCaptures(resolved.captures(),cleaned),clown,resolved.mines(),before.lightning().clean(cleaned)), resolved.captures());
    }

    static boolean canEnd(Context context, FigureColor side) {
        var pieces=AttackRules.expireStuns(context.clown().clean(context.pieces(),context.turn()),context.turn());
        var board=new HashMap<>(context.field().getGameField());
        for(var old:context.pieces().values())if(old.position()!=null && pieces.get(old.id()).position()==null)board.put(old.position(),context.field().createFigure(Empty.class,old.position(),FigureColor.NONE));
        return !AttackRules.isInCheck(board,pieces,side,context.terrain().expire(context.turn()));
    }
    static boolean canFinishAfterMove(Context context, FigureColor side, boolean abilityRemaining, HeroRules rules) {
        if (canEnd(context, side)) return true;
        if(abilityRemaining && anyAbility(context,side,rules,next->canFinishAfterMove(next,side,false,rules)))return true;
        if(context.clown().movementTurn()==context.turn())for(var piece:context.pieces().values()){
            if(piece.position()==null || piece.color()!=side || !context.clown().canMove(piece.id(),context.turn(),true,context.pieces()))continue;
            var candidates=MoveValidator.candidates(context.field(),context.field().getFigureByPosition(piece.position()),context.rights(),context.ep());
            for(var targets:List.of(candidates.getCanMove(),candidates.getCanAttack()))for(var to:targets){
                var preview=move(context,side,piece.position(),to,null,GameMode.MOBA,rules);
                if(preview.result().isSuccess() && canFinishAfterMove(preview.next(),side,abilityRemaining,rules))return true;
            }
        }
        return false;
    }
    static boolean hasMove(Context context, FigureColor side, GameMode mode, HeroRules rules) {
        return anyMove(context, side, mode, rules, next -> canFinishAfterMove(next,side,false,rules));
    }
    private static boolean anyMove(Context context, FigureColor side, GameMode mode, HeroRules rules, java.util.function.Predicate<Context> continuation) {
        for (var entry : context.field().getGameField().entrySet()) {
            Figure piece = entry.getValue();
            if (piece.getColor() != side) continue;
            AvailableMoves candidates = MoveValidator.candidates(context.field(), piece, context.rights(), context.ep());
            for (var targets : List.of(candidates.getCanMove(), candidates.getCanAttack())) for (Position target : targets) {
                Promotion promotion = MoveValidator.isPromotion(piece, target, 8) ? Promotion.QUEEN : null;
                Preview preview = move(context, side, entry.getKey(), target, promotion, mode, rules);
                if (preview.result().isSuccess() && continuation.test(preview.next())) return true;
            }
        }
        return false;
    }
    static List<AbilityRequest> requests(Context context,FigureColor side,Position square,Ability ability,boolean borrowed,HeroRules rules) {
        if(!abilityReady(context,side,square,ability,rules,borrowed).isSuccess())return List.of();
        var result=new ArrayList<AbilityRequest>();
        if(ability==Ability.STEAL || ability==Ability.AMBUSH || ability==Ability.SUMMON_YAGA || ability==Ability.INVISIBILITY || ability==Ability.PLANT_MINE || ability==Ability.THUNDERSTORM || ability==Ability.EVOLVE) {
            result.add(new AbilityRequest(square,ability,null,null,borrowed));return result;
        }
        if(ability==Ability.SPEAR) {
            for(Position target:AresAbilities.directions(square))result.add(new AbilityRequest(square,ability,target,null,borrowed));return result;
        }
        if(ability==Ability.DISPLACE) {
            for(PieceState target:context.pieces().values())if(target.position()!=null && target.type()!=King.class
                    && Math.max(Math.abs(target.position().getRow()-square.getRow()),Math.abs(target.position().getColumn()-square.getColumn()))<=3) {
                for(int dr=-1;dr<=1;dr++)for(int dc=-1;dc<=1;dc++) {
                    Position to=new Position(target.position().getRow()+dr,target.position().getColumn()+dc);
                    if((dr!=0 || dc!=0) && MoveValidator.inside(context.field(),to) && context.at(to)==null)
                        result.add(new AbilityRequest(square,ability,target.position(),to,borrowed));
                }
            }
        } else for(Position target:context.field().getGameField().keySet()) {
            if(ability==Ability.COPY_SPELL) {
                PieceState source=context.at(target);
                if(source==null || (source.hero()!=game.heroes.HeroType.ILLUSION && !context.spells().lastUsed().containsKey(source.id())))continue;
            }
            result.add(new AbilityRequest(square,ability,target,null,borrowed));
        }
        return result;
    }
    private static boolean anyAbility(Context context, FigureColor side, HeroRules rules, java.util.function.Predicate<Context> continuation) {
        for(PieceState piece:context.pieces().values()) {
            if(piece.position()==null || piece.color()!=side)continue;
            for(boolean borrowed:List.of(false,true)) {
                var copy=context.spells().borrowed().get(piece.id());
                var abilities=borrowed?(copy==null?List.<Ability>of():List.of(copy.ability())):HeroAbilities.abilities(piece.hero());
                for(Ability ability:abilities)for(var request:requests(context,side,piece.position(),ability,borrowed,rules)) {
                    Preview preview=ability(context,side,request,rules);
                    if(preview.result().isSuccess() && continuation.test(preview.next()))return true;
                }
            }
        }
        return false;
    }
    /** A turn must still be finishable when temporary terrain expires. */
    static boolean hasTurn(Context context, FigureColor side, HeroRules rules) {
        if (anyMove(context, side, GameMode.MOBA, rules, next -> canFinishAfterMove(next, side, true, rules))) return true;
        return anyAbility(context, side, rules, next -> hasMove(next, side, GameMode.MOBA, rules));
    }

    /** Apply a prepared hero patch to this action's private copies, never to the live Game. */
    private static CastlingRights applyEffect(Field factory, Map<Position, Figure> board, Map<Long, PieceState> states,
                                              CastlingRights rights, HeroEffect effect) {
        for (var entry : effect.updates().entrySet()) {
            PieceState old = states.get(entry.getKey()), next = entry.getValue();
            if (old.position() != null) {
                board.put(old.position(), factory.createFigure(Empty.class, old.position(), FigureColor.NONE));
                if (!old.position().equals(next.position()) || next.isPotted()) rights = removedRookRights(rights, old, old.position());
            }
        }
        states.putAll(effect.updates());
        for (PieceState next : effect.updates().values()) if (next.position() != null)
            board.put(next.position(), factory.createFigure(next.type(), next.position(), next.color()));
        return rights;
    }

    private static CastlingRights rightsAfter(CastlingRights rights, Figure moving, Position from, Figure captured, Position captureSquare) {
        if (moving instanceof King) rights = rights.withoutKing(moving.getColor());
        if (moving instanceof Rook) rights = withoutHomeRook(rights, moving.getColor(), from);
        if (captured instanceof Rook) rights = withoutHomeRook(rights, captured.getColor(), captureSquare);
        return rights;
    }
    private static CastlingRights removedRookRights(CastlingRights rights, PieceState piece, Position square) {
        return piece.type() == Rook.class ? withoutHomeRook(rights, piece.color(), square) : rights;
    }
    private static CastlingRights withoutHomeRook(CastlingRights rights, FigureColor color, Position square) {
        if (square.getRow() == MoveValidator.homeRow(color)) {
            if (square.getColumn() == 0) return rights.without(color, false);
            if (square.getColumn() == 7) return rights.without(color, true);
        }
        return rights;
    }
    private static Position validEp(Map<Position, Figure> board, Position ep,Map<Long,PieceState> pieces) {
        if (ep == null) return null;
        FigureColor pawnColor = ep.getRow() == 2 ? FigureColor.WHITE : FigureColor.BLACK;
        int direction = pawnColor == FigureColor.WHITE ? 1 : -1;
        Position square = new Position(ep.getRow() + direction, ep.getColumn());
        Position origin = new Position(ep.getRow() - direction, ep.getColumn());
        Figure pawn = board.get(square);
        return board.get(ep) instanceof Empty && board.get(origin) instanceof Empty
                && pawn instanceof Pawn && pawn.getColor() == pawnColor && pieces.values().stream().noneMatch(p->p.isPotted() && square.equals(p.position())) ? ep : null;
    }
}
