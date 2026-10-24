package game.heroes.lumeshard;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Lumeshard's balance parameters. */
public final class LumeshardRulesCodec implements HeroRulesCodec<LumeshardRules> {
    @Override public String section() { return "lumeshardRules"; }
    @Override public int sinceVersion() { return 2; }
    @Override public String beforeSection() { return "goldRules"; }

    @Override public Map<String, String> encode(LumeshardRules rules) {
        return Map.of("laserCooldown", String.valueOf(rules.laserCooldown()),
                "blindnessTurns", String.valueOf(rules.blindnessTurns()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public LumeshardRules decode(HeroRuleAttributes attributes) {
        return new LumeshardRules(attributes.integer("laserCooldown"),
                attributes.integer("blindnessTurns"),
                attributes.integer("ultimateCaptures"));
    }
}
