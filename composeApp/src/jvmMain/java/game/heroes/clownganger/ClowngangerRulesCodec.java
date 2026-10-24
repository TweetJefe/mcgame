package game.heroes.clownganger;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Clownganger's balance parameters. */
public final class ClowngangerRulesCodec implements HeroRulesCodec<ClowngangerRules> {
    @Override public String section() { return "clowngangerRules"; }
    @Override public int sinceVersion() { return 10; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(ClowngangerRules rules) {
        return Map.of("boxCooldown", String.valueOf(rules.boxCooldown()),
                "boxTurns", String.valueOf(rules.boxTurns()),
                "illusionTurns", String.valueOf(rules.illusionTurns()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public ClowngangerRules decode(HeroRuleAttributes attributes) {
        return new ClowngangerRules(attributes.integer("boxCooldown"),
                attributes.integer("boxTurns"),
                attributes.integer("illusionTurns"),
                attributes.integer("ultimateCaptures"));
    }
}
