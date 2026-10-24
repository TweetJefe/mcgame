package game.heroes.kikimora;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Kikimora's balance parameters. */
public final class KikimoraRulesCodec implements HeroRulesCodec<KikimoraRules> {
    @Override public String section() { return "kikimoraRules"; }
    @Override public int sinceVersion() { return 14; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(KikimoraRules rules) {
        return Map.of("yarnCooldown", String.valueOf(rules.yarnCooldown()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public KikimoraRules decode(HeroRuleAttributes attributes) {
        return new KikimoraRules(attributes.integer("yarnCooldown"),
                attributes.integer("ultimateCaptures"));
    }
}
