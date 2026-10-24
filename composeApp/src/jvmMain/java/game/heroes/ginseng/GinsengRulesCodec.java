package game.heroes.ginseng;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Ginseng's balance parameters. */
public final class GinsengRulesCodec implements HeroRulesCodec<GinsengRules> {
    @Override public String section() { return "ginsengRules"; }
    @Override public int sinceVersion() { return 7; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(GinsengRules rules) {
        return Map.of("displacementCooldown", String.valueOf(rules.displacementCooldown()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public GinsengRules decode(HeroRuleAttributes attributes) {
        return new GinsengRules(attributes.integer("displacementCooldown"),
                attributes.integer("ultimateCaptures"));
    }
}
