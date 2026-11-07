package game.heroes.sunwukong;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;
import java.util.Map;

public final class SunWukongRulesCodec implements HeroRulesCodec<SunWukongRules> {
    @Override public String section() { return "sunWukongRules"; }
    @Override public int sinceVersion() { return 18; }
    @Override public Map<String, String> encode(SunWukongRules rules) {
        return Map.of("shapeshiftCooldown", Integer.toString(rules.shapeshiftCooldown()),
                "shapeshiftTurns", Integer.toString(rules.shapeshiftTurns()),
                "ultimateCaptures", Integer.toString(rules.ultimateCaptures()));
    }
    @Override public SunWukongRules decode(HeroRuleAttributes attributes) {
        return new SunWukongRules(attributes.integer("shapeshiftCooldown"),
                attributes.integer("shapeshiftTurns"), attributes.integer("ultimateCaptures"));
    }
}
