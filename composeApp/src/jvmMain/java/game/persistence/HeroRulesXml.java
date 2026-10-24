package game.persistence;

import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRules;
import game.heroes.HeroRulesCodec;
import game.state.PieceState;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/** XML orchestration over registered codecs; contains no hero-specific schemas. */
public final class HeroRulesXml {
    private HeroRulesXml() {}

    public static int latestVersion() {
        return HeroDefinitions.all().stream().flatMap(d -> d.rulesCodec().stream())
                .mapToInt(HeroRulesCodec::sinceVersion).max().orElse(1);
    }

    public static int requiredVersion(HeroRules rules, Collection<PieceState> pieces) {
        int version = 1;
        for (HeroDefinition<?> definition : rules.definitions())
            version = Math.max(version, requiredVersion(definition, rules, pieces));
        return version;
    }

    private static <R> int requiredVersion(HeroDefinition<R> definition, HeroRules rules,
                                           Collection<PieceState> pieces) {
        return definition.rulesCodec().filter(codec -> !rules.get(definition).equals(definition.defaultRules())
                || pieces.stream().anyMatch(piece -> piece.hero() == definition.type()))
                .map(HeroRulesCodec::sinceVersion).orElse(1);
    }

    public static void write(Element root, HeroRules rules, int version) {
        for (HeroDefinition<?> definition : rules.definitions()) write(root, rules, version, definition);
    }

    private static <R> void write(Element root, HeroRules rules, int version, HeroDefinition<R> definition) {
        definition.rulesCodec().filter(codec -> version >= codec.sinceVersion()).ifPresent(codec -> {
            Element section = root.getOwnerDocument().createElement(codec.section());
            for (var attribute : codec.encode(rules.get(definition)).entrySet())
                section.setAttribute(attribute.getKey(), attribute.getValue());
            Element anchor = find(root, codec.beforeSection());
            if (anchor == null) anchor = find(root, "castling");
            root.insertBefore(section, anchor);
        });
    }

    public static HeroRules read(Element root, int version, HeroRules defaults) {
        HeroRules rules = defaults;
        for (HeroDefinition<?> definition : defaults.definitions()) rules = read(root, version, rules, definition);
        return rules;
    }

    private static <R> HeroRules read(Element root, int version, HeroRules rules, HeroDefinition<R> definition) {
        if (definition.rulesCodec().isEmpty()) return rules;
        HeroRulesCodec<R> codec = definition.rulesCodec().orElseThrow();
        Element section = find(root, codec.section());
        if (version < codec.sinceVersion()) {
            if (section != null) throw new IllegalArgumentException("Rules section requires format " + codec.sinceVersion());
            return rules;
        }
        if (section == null) throw new IllegalArgumentException("Missing " + codec.section());
        var attributes = new HashMap<String, String>();
        for (int i = 0; i < section.getAttributes().getLength(); i++) {
            Node attribute = section.getAttributes().item(i);
            attributes.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        return rules.with(definition, codec.decode(new HeroRuleAttributes(attributes)));
    }

    public static Set<String> sections(HeroRules rules) {
        return rules.definitions().stream().flatMap(d -> d.rulesCodec().stream())
                .map(HeroRulesCodec::section).collect(Collectors.toUnmodifiableSet());
    }

    private static Element find(Element root, String name) {
        Element result = null;
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element element && element.getTagName().equals(name)) {
                if (result != null) throw new IllegalArgumentException("Duplicate " + name);
                result = element;
            }
        return result;
    }
}
