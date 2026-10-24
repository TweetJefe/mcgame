package game.heroes.ares;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Ares's balance parameters. */
public final class AresRulesCodec implements HeroRulesCodec<AresRules> {
    @Override public String section() { return "aresRules"; }
    @Override public int sinceVersion() { return 9; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(AresRules rules) {
        return Map.of("spearCooldown", String.valueOf(rules.spearCooldown()),
                "revengeCaptures", String.valueOf(rules.revengeCaptures()));
    }

    @Override public AresRules decode(HeroRuleAttributes attributes) {
        return new AresRules(attributes.integer("spearCooldown"),
                attributes.integer("revengeCaptures"));
    }
}
