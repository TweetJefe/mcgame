package game.heroes;

import java.util.Map;

/** Bounded, immutable attributes supplied to a registered rules decoder. */
public record HeroRuleAttributes(Map<String, String> values) {
    public HeroRuleAttributes { values = Map.copyOf(values); }

    public String text(String name) {
        String value = values.get(name);
        if (value == null) throw new IllegalArgumentException("Missing rule attribute: " + name);
        if (value.length() > 4096) throw new IllegalArgumentException("Attribute too long");
        return value;
    }

    public int integer(String name) { return Math.toIntExact(Long.parseLong(text(name))); }

    public boolean bool(String name) {
        String value = text(name);
        if (!value.equals("true") && !value.equals("false"))
            throw new IllegalArgumentException("Invalid boolean");
        return Boolean.parseBoolean(value);
    }
}
