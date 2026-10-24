package game.heroes.zeus;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Zeus's balance parameters. */
public final class ZeusRulesCodec implements HeroRulesCodec<ZeusRules> {
    @Override public String section() { return "zeusRules"; }
    @Override public int sinceVersion() { return 12; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(ZeusRules rules) {
        return Map.of("lightningCooldown", String.valueOf(rules.lightningCooldown()),
                "chargeTurns", String.valueOf(rules.chargeTurns()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public ZeusRules decode(HeroRuleAttributes attributes) {
        return new ZeusRules(attributes.integer("lightningCooldown"),
                attributes.integer("chargeTurns"),
                attributes.integer("ultimateCaptures"));
    }
}
