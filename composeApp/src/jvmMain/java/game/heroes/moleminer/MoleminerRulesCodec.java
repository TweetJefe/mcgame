package game.heroes.moleminer;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Moleminer's balance parameters. */
public final class MoleminerRulesCodec implements HeroRulesCodec<MoleminerRules> {
    @Override public String section() { return "moleminerRules"; }
    @Override public int sinceVersion() { return 11; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(MoleminerRules rules) {
        return Map.of("mineCooldown", String.valueOf(rules.mineCooldown()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public MoleminerRules decode(HeroRuleAttributes attributes) {
        return new MoleminerRules(attributes.integer("mineCooldown"),
                attributes.integer("ultimateCaptures"));
    }
}
