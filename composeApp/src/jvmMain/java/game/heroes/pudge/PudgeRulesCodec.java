package game.heroes.pudge;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Pudge's balance parameters. */
public final class PudgeRulesCodec implements HeroRulesCodec<PudgeRules> {
    @Override public String section() { return "rules"; }
    @Override public int sinceVersion() { return 1; }
    @Override public String beforeSection() { return "goldRules"; }

    @Override public Map<String, String> encode(PudgeRules rules) {
        return Map.of("hookCooldown", String.valueOf(rules.hookCooldown()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()),
                "hookDiagonals", String.valueOf(rules.hookDiagonals()),
                "swallowDiagonals", String.valueOf(rules.swallowDiagonals()));
    }

    @Override public PudgeRules decode(HeroRuleAttributes attributes) {
        return new PudgeRules(attributes.integer("hookCooldown"),
                attributes.integer("ultimateCaptures"),
                attributes.bool("hookDiagonals"),
                attributes.bool("swallowDiagonals"));
    }
}
