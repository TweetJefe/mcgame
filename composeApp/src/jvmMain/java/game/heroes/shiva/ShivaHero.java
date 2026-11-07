package game.heroes.shiva;

import game.heroes.AbstractHero;
import game.heroes.AbilityCost;
import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.Objects;
import model.Position;

public final class ShivaHero extends AbstractHero {
    private final ShivaRules rules;
    public ShivaHero(ShivaRules rules) {
        super(ShivaDefinition.INSTANCE, ShivaAbility.getAbilities(), ShivaAbility.REBIRTH,
                Objects.requireNonNull(rules, "rules").ultimateCaptures());
        this.rules = rules;
    }
    @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor, PieceState target, AbilityRequest request) {
        if (request.ability() == ShivaAbility.LOTUS) {
            if (target != null || distance(actor.position(), request.target()) > rules.castRadius())
                return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            return HeroEffect.empty().withOperations(new ShivaMechanic.Plant(actor.id(), request.target()))
                    .withCost(AbilityCost.cooldown(rules.lotusCooldown()));
        }
        if (ShivaMechanic.orientations(context).isEmpty()) return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
        return HeroEffect.empty().withOperations(new ShivaMechanic.Rebirth(actor.id()))
                .withCost(AbilityCost.ultimate(ultimateCaptures()));
    }
    static int distance(Position a, Position b) {
        return Math.max(Math.abs(a.getRow()-b.getRow()), Math.abs(a.getColumn()-b.getColumn()));
    }
    @Override public String abilityMessage(HeroActionContext before, HeroActionContext after,
            PieceState actor, PieceState target, AbilityRequest request) {
        return request.ability() == ShivaAbility.LOTUS ? "Shiva: установлен лотус" : "Shiva: воскрешение";
    }
}
