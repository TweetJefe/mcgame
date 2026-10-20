package game.heroes;

import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.heroes.faun.FaunAbilities;
import game.heroes.ares.AresAbilities;
import game.heroes.izba.IzbaAbilities;
import game.heroes.ginseng.GinsengAbilities;
import game.heroes.bolotnik.BolotnikAbilities;
import game.heroes.bountycrow.BountyCrowAbilities;
import game.heroes.lumeshard.LumeshardAbilities;
import game.moves.MoveResult;
import game.state.PieceState;

import model.Field;
import model.Position;

import java.util.List;

/**
 * Existing dispatch kept temporarily for unmigrated strategies. Pudge is intentionally absent.
 */
final class LegacyHeroAbilities {
    private LegacyHeroAbilities() {
    }

    static List<Abilities> abilities(HeroType hero) {
        return switch (hero) {
            case KIKIMORA_BISHOP -> game.heroes.kikimora.KikimoraAbilities.abilities(); case CHRONOMLIN_PAWN -> game.heroes.chronomlin.ChronomlinAbilities.abilities(); case ZEUS_ROOK -> game.heroes.zeus.ZeusAbilities.abilities(); case MOLEMINER_PAWN -> game.heroes.moleminer.MoleminerAbilities.abilities(); case CLOWNGANGER_KNIGHT -> game.heroes.clownganger.ClowngangerAbilities.abilities(); case ARES_BISHOP -> AresAbilities.abilities();
            case FAUN_KNIGHT -> FaunAbilities.abilities();
            case GINSENG_BISHOP -> GinsengAbilities.abilities();
            case NONE, ILLUSION, BABA_YAGA_PAWN -> List.of();
            case IZBA_ROOK -> IzbaAbilities.abilities();
            default -> throw new IllegalArgumentException("Hero requires a registered strategy");
            case LUMESHARD_BISHOP -> LumeshardAbilities.abilities();
            case BOUNTY_CROW_PAWN -> BountyCrowAbilities.abilities();
            case BOLOTNIK_KNIGHT -> BolotnikAbilities.abilities();
        };
    }

    static MoveResult availability(PieceState actor, Abilities ability, HeroRules rules) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if(actor.isPotted())return StatusRejection.POTTED;
        return switch (actor.hero()) { case KIKIMORA_BISHOP -> game.heroes.kikimora.KikimoraAbilities.availability(actor,ability,rules.kikimora(),false); case CHRONOMLIN_PAWN -> game.heroes.chronomlin.ChronomlinAbilities.availability(actor,ability,rules.chronomlin(),false); case ZEUS_ROOK -> game.heroes.zeus.ZeusAbilities.availability(actor,ability,rules.zeus()); case MOLEMINER_PAWN -> game.heroes.moleminer.MoleminerAbilities.availability(actor,ability,rules.moleminer()); case CLOWNGANGER_KNIGHT -> game.heroes.clownganger.ClowngangerAbilities.availability(actor,ability,rules.clownganger()); case ARES_BISHOP -> AresAbilities.availability(actor, ability, rules.ares());
            case FAUN_KNIGHT -> FaunAbilities.availability(actor, ability, rules.faun());
            case GINSENG_BISHOP -> GinsengAbilities.availability(actor, ability, rules.ginseng(), false);
            case NONE, ILLUSION, BABA_YAGA_PAWN -> AbilityRejection.NO_ABILITY;
            case IZBA_ROOK -> IzbaAbilities.availability(actor, ability, rules.izba());
            default -> throw new IllegalArgumentException("Hero requires a registered strategy");
            case LUMESHARD_BISHOP -> LumeshardAbilities.availability(actor, ability, rules.lumeshard());
            case BOUNTY_CROW_PAWN -> BountyCrowAbilities.availability(actor, ability, rules.crow());
            case BOLOTNIK_KNIGHT -> BolotnikAbilities.availability(actor, ability, rules.bolotnik());
        };
    }

    static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square, Abilities ability, HeroRules rules) {
        return switch (actor.hero()) {
            case CHRONOMLIN_PAWN -> game.heroes.chronomlin.ChronomlinAbilities.preview(field,actor,target,ability,rules.chronomlin(),false);
            case FAUN_KNIGHT -> FaunAbilities.preview(actor, square, ability, rules.faun());
            case KIKIMORA_BISHOP, ZEUS_ROOK, MOLEMINER_PAWN, CLOWNGANGER_KNIGHT, ARES_BISHOP, GINSENG_BISHOP -> HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            case NONE, ILLUSION, BABA_YAGA_PAWN -> HeroEffect.rejected(AbilityRejection.NO_ABILITY);
            case IZBA_ROOK -> IzbaAbilities.preview(actor, ability, rules.izba());
            default -> throw new IllegalArgumentException("Hero requires a registered strategy");
            case LUMESHARD_BISHOP ->
                    LumeshardAbilities.preview(field, actor, target, square, ability, rules.lumeshard());
            case BOUNTY_CROW_PAWN -> BountyCrowAbilities.preview(actor, target, ability, rules.crow());
            case BOLOTNIK_KNIGHT -> BolotnikAbilities.preview(actor, target, square, ability, rules.bolotnik());
        };
    }

    static PieceState afterCapture(PieceState actor, HeroRules rules) {
        if(actor.isPotted())return actor;
        return switch (actor.hero()) { case KIKIMORA_BISHOP -> actor.withAbilityState(actor.cooldown(),Math.min(rules.kikimora().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case CHRONOMLIN_PAWN -> actor.withAbilityState(actor.cooldown(),Math.min(rules.chronomlin().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case ZEUS_ROOK -> actor.withAbilityState(actor.cooldown(),Math.min(rules.zeus().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case MOLEMINER_PAWN -> actor.withAbilityState(actor.cooldown(),Math.min(rules.moleminer().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case CLOWNGANGER_KNIGHT -> actor.withAbilityState(actor.cooldown(),Math.min(rules.clownganger().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case ARES_BISHOP -> AresAbilities.afterCapture(actor, rules.ares());
            case FAUN_KNIGHT -> FaunAbilities.afterCapture(actor, rules.faun());
            case GINSENG_BISHOP -> GinsengAbilities.afterCapture(actor, rules.ginseng());
            case NONE, ILLUSION, BABA_YAGA_PAWN -> actor;
            case IZBA_ROOK -> IzbaAbilities.afterCapture(actor, rules.izba());
            default -> throw new IllegalArgumentException("Hero requires a registered strategy");
            case LUMESHARD_BISHOP -> LumeshardAbilities.afterCapture(actor, rules.lumeshard());
            case BOUNTY_CROW_PAWN -> BountyCrowAbilities.afterCapture(actor, rules.crow());
            case BOLOTNIK_KNIGHT -> BolotnikAbilities.afterCapture(actor, rules.bolotnik());
        };
    }

}
