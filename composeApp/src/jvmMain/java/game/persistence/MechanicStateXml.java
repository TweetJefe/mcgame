package game.persistence;

import game.heroes.HeroRegistry;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicStates;
import game.state.GameSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Общая секция новых механик; исторические секции обслуживает прежний адаптер XML. */
public final class MechanicStateXml {
    public static final int FORMAT_VERSION = 16;
    private MechanicStateXml() {}

    public static int requiredVersion(GameSnapshot snapshot) {
        int version = 1;
        for (var module : HeroRegistry.fromRules(snapshot.heroRules()).mechanics().definitions())
            if (!module.legacyWireState() && needsVersion(module, snapshot))
                version = FORMAT_VERSION;
        return version;
    }

    private static <S> boolean needsVersion(MechanicDefinition<S> module, GameSnapshot snapshot) {
        S state = snapshot.mechanics().get(module.key());
        return !state.equals(module.key().initial()) || module.codec() != null && !state.equals(module.restore(snapshot));
    }

    public static void write(Element root, GameSnapshot snapshot) {
        Element section = null;
        for (var module : HeroRegistry.fromRules(snapshot.heroRules()).mechanics().definitions()) {
            if (module.legacyWireState()) continue;
            var data = encode(module, snapshot);
            if (data == null) continue;
            if (section == null) {
                section = root.getOwnerDocument().createElement("mechanics"); root.appendChild(section);
            }
            Element entry = root.getOwnerDocument().createElement("state"); section.appendChild(entry);
            entry.setAttribute("id", module.key().id());
            writeFields(entry, data);
        }
    }

    private static <S> Map<String, String> encode(MechanicDefinition<S> module, GameSnapshot snapshot) {
        S state = snapshot.mechanics().get(module.key());
        if (module.codec() == null) {
            if (!state.equals(module.restore(snapshot))) throw new IllegalArgumentException("Mechanic state has no codec: " + module.key().id());
            return null;
        }
        if (state.equals(module.restore(snapshot))) return null;
        return Map.copyOf(module.codec().encode(state));
    }

    public static GameSnapshot read(Element root, int version, GameSnapshot base, HeroRegistry heroes) {
        Element section = null;
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element entry && entry.getTagName().equals("mechanics")) {
                if (section != null || version < FORMAT_VERSION) throw new IllegalArgumentException("Duplicate or premature mechanics section");
                section = entry;
            }
        var encoded = new HashMap<String, Element>();
        if (section != null) for (Node node = section.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element entry) {
                if (!entry.getTagName().equals("state") || !entry.hasAttribute("id") || encoded.putIfAbsent(entry.getAttribute("id"), entry) != null)
                    throw new IllegalArgumentException("Invalid mechanic state entry");
            }
        MechanicStates states = base.mechanics();
        for (var module : heroes.mechanics().definitions()) {
            var entry = encoded.remove(module.key().id());
            if (entry != null && (module.codec() == null || module.legacyWireState()))
                throw new IllegalArgumentException("Unexpected mechanic state: " + module.key().id());
            if (!module.legacyWireState()) states = decode(module, states, entry, base.withMechanics(states));
        }
        if (!encoded.isEmpty()) throw new IllegalArgumentException("Unregistered mechanic state");
        return base.withMechanics(states);
    }

    private static <S> MechanicStates decode(MechanicDefinition<S> module, MechanicStates states, Element entry, GameSnapshot base) {
        S value = entry == null ? module.restore(base) : module.codec().decode(readFields(entry));
        return states.with(module.key(), value);
    }

    static void writeFields(Element parent, Map<String, String> data) {
        if (data.size() > 256) throw new IllegalArgumentException("Too many mechanic fields");
        for (var field : new TreeMap<>(data).entrySet()) {
            checkField(field.getKey(), field.getValue());
            Element entry = parent.getOwnerDocument().createElement("attribute"); parent.appendChild(entry);
            entry.setAttribute("name", field.getKey()); entry.setAttribute("value", field.getValue());
        }
    }

    static Map<String, String> readFields(Element parent) {
        var result = new HashMap<String, String>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element entry) {
                if (!entry.getTagName().equals("attribute") || !entry.hasAttribute("name") || !entry.hasAttribute("value"))
                    throw new IllegalArgumentException("Invalid mechanic field");
                String name = entry.getAttribute("name"), value = entry.getAttribute("value");
                checkField(name, value);
                if (result.putIfAbsent(name, value) != null || result.size() > 256)
                    throw new IllegalArgumentException("Duplicate or excessive mechanic fields");
                for (Node child = entry.getFirstChild(); child != null; child = child.getNextSibling())
                    if (child instanceof Element) throw new IllegalArgumentException("Nested mechanic field");
            }
        return Map.copyOf(result);
    }

    private static void checkField(String name, String value) {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_.-]{0,127}") || value == null || value.length() > 4096)
            throw new IllegalArgumentException("Invalid mechanic field");
    }
}
