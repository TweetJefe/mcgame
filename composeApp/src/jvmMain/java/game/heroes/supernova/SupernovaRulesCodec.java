package game.heroes.supernova;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;
import java.util.Map;

public final class SupernovaRulesCodec implements HeroRulesCodec<SupernovaRules> {
    @Override public String section() { return "supernovaRules"; }
    @Override public int sinceVersion() { return 20; }
    @Override public Map<String, String> encode(SupernovaRules rules) {
        return Map.of("pullCooldown", Integer.toString(rules.pullCooldown()),
                "ultimateCaptures", Integer.toString(rules.ultimateCaptures()),
                "castRadius", Integer.toString(rules.castRadius()),
                "effectRadius", Integer.toString(rules.effectRadius()));
    }
    @Override public SupernovaRules decode(HeroRuleAttributes attributes) {
        return new SupernovaRules(attributes.integer("pullCooldown"), attributes.integer("ultimateCaptures"), attributes.integer("castRadius"), attributes.integer("effectRadius"));
    }
}
