package game.heroes.phoenix;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;
import java.util.Map;

public final class PhoenixRulesCodec implements HeroRulesCodec<PhoenixRules> {
    @Override public String section() { return "phoenixRules"; }
    @Override public int sinceVersion() { return 20; }
    @Override public Map<String, String> encode(PhoenixRules rules) {
        return Map.of("ultimateCaptures", Integer.toString(rules.ultimateCaptures()),
                "eggTurns", Integer.toString(rules.eggTurns()));
    }
    @Override public PhoenixRules decode(HeroRuleAttributes attributes) {
        return new PhoenixRules(attributes.integer("ultimateCaptures"), attributes.integer("eggTurns"));
    }
}
