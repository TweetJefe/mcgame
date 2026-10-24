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
            case KIKIMORA -> game.heroes.kikimora.KikimoraAbilities.abilities(); case CHRONOMLIN -> game.heroes.chronomlin.ChronomlinAbilities.abilities(); case ZEUS -> game.heroes.zeus.ZeusAbilities.abilities(); case MOLEMINER -> game.heroes.moleminer.MoleminerAbilities.abilities(); case CLOWNGANGER -> game.heroes.clownganger.ClowngangerAbilities.abilities(); case ARES -> AresAbilities.abilities();
            case FAUN -> FaunAbilities.abilities();
            case GINSENG -> GinsengAbilities.abilities();
            case NONE, ILLUSION, BABA_YAGA -> List.of();
            case IZBA -> IzbaAbilities.abilities();
            default -> throw new IllegalArgumentException("Hero requires a registered strategy");
            case LUMESHARD -> LumeshardAbilities.abilities();
            case BOUNTY_CROW -> BountyCrowAbilities.abilities();
            case BOLOTNIK -> BolotnikAbilities.abilities();
        };
    }

    static MoveResult availability(PieceState actor, Abilities ability, HeroRules rules) {
        if (ability == null) return AbilityRejection.INVALID_ABILITY;
        if(actor.isPotted())return StatusRejection.POTTED;
        return switch (actor.hero()) { case KIKIMORA -> game.heroes.kikimora.KikimoraAbilities.availability(actor,ability,rules.kikimora(),false); case CHRONOMLIN -> game.heroes.chronomlin.ChronomlinAbilities.availability(actor,ability,rules.chronomlin(),false); case ZEUS -> game.heroes.zeus.ZeusAbilities.availability(actor,ability,rules.zeus()); case MOLEMINER -> game.heroes.moleminer.MoleminerAbilities.availability(actor,ability,rules.moleminer()); case CLOWNGANGER -> game.heroes.clownganger.ClowngangerAbilities.availability(actor,ability,rules.clownganger()); case ARES -> AresAbilities.availability(actor, ability, rules.ares());
            case FAUN -> FaunAbilities.availability(actor, ability, rules.faun());
            case GINSENG -> GinsengAbilities.availability(actor, ability, rules.ginseng(), false);
            case NONE, ILLUSION, BABA_YAGA -> AbilityRejection.NO_ABILITY;
            case IZBA -> IzbaAbilities.availability(actor, ability, rules.izba());
            default -> throw new IllegalArgumentException("Hero requires a registered strategy");
            case LUMESHARD -> LumeshardAbilities.availability(actor, ability, rules.lumeshard());
            case BOUNTY_CROW -> BountyCrowAbilities.availability(actor, ability, rules.crow());
            case BOLOTNIK -> BolotnikAbilities.availability(actor, ability, rules.bolotnik());
        };
    }

    static HeroEffect preview(Field field, PieceState actor, PieceState target, Position square, Abilities ability, HeroRules rules) {
        return switch (actor.hero()) {
            case CHRONOMLIN -> game.heroes.chronomlin.ChronomlinAbilities.preview(field,actor,target,ability,rules.chronomlin(),false);
            case FAUN -> FaunAbilities.preview(actor, square, ability, rules.faun());
            case KIKIMORA, ZEUS, MOLEMINER, CLOWNGANGER, ARES, GINSENG -> HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            case NONE, ILLUSION, BABA_YAGA -> HeroEffect.rejected(AbilityRejection.NO_ABILITY);
            case IZBA -> IzbaAbilities.preview(actor, ability, rules.izba());
            default -> throw new IllegalArgumentException("Hero requires a registered strategy");
            case LUMESHARD ->
                    LumeshardAbilities.preview(field, actor, target, square, ability, rules.lumeshard());
            case BOUNTY_CROW -> BountyCrowAbilities.preview(actor, target, ability, rules.crow());
            case BOLOTNIK -> BolotnikAbilities.preview(actor, target, square, ability, rules.bolotnik());
        };
    }

    static PieceState afterCapture(PieceState actor, HeroRules rules) {
        if(actor.isPotted())return actor;
        return switch (actor.hero()) { case KIKIMORA -> actor.withAbilityState(actor.cooldown(),Math.min(rules.kikimora().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case CHRONOMLIN -> actor.withAbilityState(actor.cooldown(),Math.min(rules.chronomlin().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case ZEUS -> actor.withAbilityState(actor.cooldown(),Math.min(rules.zeus().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case MOLEMINER -> actor.withAbilityState(actor.cooldown(),Math.min(rules.moleminer().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case CLOWNGANGER -> actor.withAbilityState(actor.cooldown(),Math.min(rules.clownganger().ultimateCaptures(),actor.ultimateCharge()+1),actor.storedPieceId()); case ARES -> AresAbilities.afterCapture(actor, rules.ares());
            case FAUN -> FaunAbilities.afterCapture(actor, rules.faun());
            case GINSENG -> GinsengAbilities.afterCapture(actor, rules.ginseng());
            case NONE, ILLUSION, BABA_YAGA -> actor;
            case IZBA -> IzbaAbilities.afterCapture(actor, rules.izba());
            default -> throw new IllegalArgumentException("Hero requires a registered strategy");
            case LUMESHARD -> LumeshardAbilities.afterCapture(actor, rules.lumeshard());
            case BOUNTY_CROW -> BountyCrowAbilities.afterCapture(actor, rules.crow());
            case BOLOTNIK -> BolotnikAbilities.afterCapture(actor, rules.bolotnik());
        };
    }

}
