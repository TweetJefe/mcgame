package game.heroes.strix;

import game.heroes.HeroRulesCodec;
import game.heroes.HeroRuleAttributes;
import java.util.Map;

public final class StrixRulesCodec implements HeroRulesCodec<StrixRules> {
    @Override public String section() { return "strixRules"; }
    @Override public int sinceVersion() { return 19; }
    @Override public Map<String, String> encode(StrixRules rules) {
        return Map.of("rewindCooldown", Integer.toString(rules.rewindCooldown()),
                "ultimateCaptures", Integer.toString(rules.ultimateCaptures()));
    }
    @Override public StrixRules decode(HeroRuleAttributes attributes) {
        return new StrixRules(attributes.integer("rewindCooldown"), attributes.integer("ultimateCaptures"));
    }
}
