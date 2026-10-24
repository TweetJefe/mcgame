package game.heroes.faun;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Faun's balance parameters. */
public final class FaunRulesCodec implements HeroRulesCodec<FaunRules> {
    @Override public String section() { return "faunRules"; }
    @Override public int sinceVersion() { return 8; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(FaunRules rules) {
        return Map.of("cloudCooldown", String.valueOf(rules.cloudCooldown()),
                "cloudTurns", String.valueOf(rules.cloudTurns()),
                "invisibilityTurns", String.valueOf(rules.invisibilityTurns()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public FaunRules decode(HeroRuleAttributes attributes) {
        return new FaunRules(attributes.integer("cloudCooldown"),
                attributes.integer("cloudTurns"),
                attributes.integer("invisibilityTurns"),
                attributes.integer("ultimateCaptures"));
    }
}
