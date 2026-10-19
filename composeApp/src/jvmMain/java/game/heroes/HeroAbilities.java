package game.heroes;

import game.heroes.pudge.PudgeAbilities;
import game.heroes.faun.FaunAbilities;
import game.heroes.ares.AresAbilities;
import game.heroes.izba.IzbaAbilities;
import game.heroes.ginseng.GinsengAbilities;
import game.heroes.bolotnik.BolotnikAbilities;
import game.heroes.bountycrow.BountyCrowAbilities;
import game.heroes.pudge.PudgeRules;
import game.heroes.lumeshard.LumeshardAbilities;
import game.moves.MoveResult;
import game.state.PieceState;

import model.Field;
import model.Position;
import java.util.List;
import java.util.Map;

/** Dispatches hero-specific effects without exposing Game's mutable internals. */
public final class HeroAbilities {
    private HeroAbilities() {}
    public static List<Ability> abilities(HeroType hero) {
        return switch (hero) { case KIKIMORA_BISHOP -> game.heroes.kikimora.KikimoraAbilities.abilities(); case CHRONOMLIN_PAWN -> game.heroes.chronomlin.ChronomlinAbilities.abilities(); case ZEUS_ROOK -> game.heroes.zeus.ZeusAbilities.abilities(); case MOLEMINER_PAWN -> game.heroes.moleminer.MoleminerAbilities.abilities(); case CLOWNGANGER_KNIGHT -> game.heroes.clownganger.ClowngangerAbilities.abilities(); case ARES_BISHOP -> AresAbilities.abilities(); case FAUN_KNIGHT -> FaunAbilities.abilities(); case GINSENG_BISHOP -> GinsengAbilities.abilities(); case NONE, ILLUSION, BABA_YAGA_PAWN -> List.of(); case IZBA_ROOK -> IzbaAbilities.abilities(); case PUDGE_ROOK -> PudgeAbilities.abilities(); case LUMESHARD_BISHOP -> LumeshardAbilities.abilities(); case BOUNTY_CROW_PAWN -> BountyCrowAbilities.abilities(); case BOLOTNIK_KNIGHT -> BolotnikAbilities.abilities(); };
    }
    public static MoveResult availability(PieceState actor, Ability ability, HeroRules rules) {
        if (ability == null) return MoveResult.INVALID_ABILITY;
        if(actor.isPotted())return MoveResult.POTTED;
        return switch (actor.hero()) { case KIKIMORA_BISHOP -> game.heroes.kikimora.KikimoraAbilities.availability(actor,ability,rules.kikimora(),false); case CHRONOMLIN_PAWN -> game.heroes.chronomlin.ChronomlinAbilities.availability(actor,ability,rules.chronomlin(),false); case ZEUS_ROOK -> game.heroes.zeus.ZeusAbilities.availability(actor,ability,rules.zeus()); case MOLEMINER_PAWN -> game.heroes.moleminer.MoleminerAbilities.availability(actor,ability,rules.moleminer()); case CLOWNGANGER_KNIGHT -> game.heroes.clownganger.ClowngangerAbilities.availability(actor,ability,rules.clownganger()); case ARES_BISHOP -> AresAbilities.availability(actor,ability,rules.ares()); case FAUN_KNIGHT -> FaunAbilities.availability(actor,ability,rules.faun()); case GINSENG_BISHOP -> GinsengAbilities.availability(actor,ability,rules.ginseng(),false); case NONE, ILLUSION, BABA_YAGA_PAWN -> MoveResult.NO_ABILITY; case IZBA_ROOK -> IzbaAbilities.availability(actor,ability,rules.izba()); case PUDGE_ROOK -> PudgeAbilities.availability(actor, ability, rules.pudge()); case LUMESHARD_BISHOP -> LumeshardAbilities.availability(actor, ability, rules.lumeshard()); case BOUNTY_CROW_PAWN -> BountyCrowAbilities.availability(actor, ability, rules.crow()); case BOLOTNIK_KNIGHT -> BolotnikAbilities.availability(actor, ability, rules.bolotnik()); };
    }
    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square, Ability ability, HeroRules rules) {
        return switch (actor.hero()) {
            case CHRONOMLIN_PAWN -> game.heroes.chronomlin.ChronomlinAbilities.preview(field,actor,target,ability,rules.chronomlin(),false);
            case FAUN_KNIGHT -> FaunAbilities.preview(actor,square,ability,rules.faun());
            case KIKIMORA_BISHOP, ZEUS_ROOK, MOLEMINER_PAWN, CLOWNGANGER_KNIGHT, ARES_BISHOP, GINSENG_BISHOP -> HeroEffect.rejected(MoveResult.INVALID_ABILITY_TARGET);
            case NONE, ILLUSION, BABA_YAGA_PAWN -> HeroEffect.rejected(MoveResult.NO_ABILITY);
            case IZBA_ROOK -> IzbaAbilities.preview(actor,ability,rules.izba());
            case PUDGE_ROOK -> PudgeAbilities.preview(field, actor, target, ability, rules.pudge());
            case LUMESHARD_BISHOP -> LumeshardAbilities.preview(field, actor, target, square, ability, rules.lumeshard());
            case BOUNTY_CROW_PAWN -> BountyCrowAbilities.preview(actor, target, ability, rules.crow());
            case BOLOTNIK_KNIGHT -> BolotnikAbilities.preview(actor, target, square, ability, rules.bolotnik());
        };
    }
    public static HeroEffect preview(Field field, PieceState actor, PieceState target, Ability ability, PudgeRules rules) {
        return preview(field, actor, target, target == null ? null : target.position(), ability, HeroRules.withPudge(rules));
    }
    public static PieceState afterCapture(PieceState actor, HeroRules rules) {
        if(actor.isPotted())return actor;
        return switch (actor.hero()) { case KIKIMORA_BISHOP -> actor.withAbilityState(actor.cooldown(),Math.min(rules.kikimora().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case CHRONOMLIN_PAWN -> actor.withAbilityState(actor.cooldown(),Math.min(rules.chronomlin().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case ZEUS_ROOK -> actor.withAbilityState(actor.cooldown(),Math.min(rules.zeus().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case MOLEMINER_PAWN -> actor.withAbilityState(actor.cooldown(),Math.min(rules.moleminer().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case CLOWNGANGER_KNIGHT -> actor.withAbilityState(actor.cooldown(),Math.min(rules.clownganger().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case ARES_BISHOP -> AresAbilities.afterCapture(actor,rules.ares()); case FAUN_KNIGHT -> FaunAbilities.afterCapture(actor,rules.faun()); case GINSENG_BISHOP -> GinsengAbilities.afterCapture(actor,rules.ginseng()); case NONE, ILLUSION, BABA_YAGA_PAWN -> actor; case IZBA_ROOK -> IzbaAbilities.afterCapture(actor,rules.izba()); case PUDGE_ROOK -> PudgeAbilities.afterCapture(actor, rules.pudge()); case LUMESHARD_BISHOP -> LumeshardAbilities.afterCapture(actor, rules.lumeshard()); case BOUNTY_CROW_PAWN -> BountyCrowAbilities.afterCapture(actor, rules.crow()); case BOLOTNIK_KNIGHT -> BolotnikAbilities.afterCapture(actor, rules.bolotnik()); };
    }
    public static HeroEffect captureReaction(Field field, Map<Long, PieceState> pieces, PieceState victim,
                                            long captorId, Position square, HeroRules rules) {
        return CaptureReactions.resolve(field,pieces,victim,captorId,square,rules);
    }
}
