package game.persistence;

import game.heroes.Abilities;
import game.heroes.HeroRegistry;
import game.history.GameEvent;
import model.FigureColor;
import model.Position;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

/** Generic cast events plus a frozen adapter for historical active-event names. */
public final class GameEventXml {
    public static final int GENERIC_ABILITY_VERSION = 15;

    private GameEventXml() {}

    // Existing wire IDs only. New abilities use ABILITY_USED + abilityId without changing this table.
    private static final Map<String, Integer> LEGACY_ABILITIES = Map.ofEntries(
            Map.entry("BOX", 10),
            Map.entry("ILLUSION", 10),
            Map.entry("MUD", 4),
            Map.entry("AMBUSH", 4),
            Map.entry("SPEAR", 9),
            Map.entry("LASER", 2),
            Map.entry("TELEPORT", 2),
            Map.entry("SUMMON_YAGA", 6),
            Map.entry("HOOK", 1),
            Map.entry("SWALLOW", 1),
            Map.entry("GRAPPLE", 13),
            Map.entry("EVOLVE", 13),
            Map.entry("BOUNTY", 3),
            Map.entry("STEAL", 3),
            Map.entry("PLANT_MINE", 11),
            Map.entry("DETONATE", 11),
            Map.entry("YARN", 14),
            Map.entry("CLAY_POT", 14),
            Map.entry("DISPLACE", 7),
            Map.entry("COPY_SPELL", 7),
            Map.entry("CLOUD", 8),
            Map.entry("INVISIBILITY", 8),
            Map.entry("CHAIN_LIGHTNING", 12),
            Map.entry("THUNDERSTORM", 12));

    public static int requiredVersion(List<GameEvent> events) {
        int version = 1;
        for (GameEvent event : events) {
            if (event.borrowed() || event.copiedAbility() != null) version = Math.max(version, 7);
            if (event.ability() != null) version = Math.max(version, abilityVersion(event.ability()));
            if (event.copiedAbility() != null) version = Math.max(version, abilityVersion(event.copiedAbility()));
        }
        return version;
    }

    private static int abilityVersion(Abilities ability) {
        return LEGACY_ABILITIES.getOrDefault(ability.id(), GENERIC_ABILITY_VERSION);
    }

    public static void write(Element entry, GameEvent event, int version) {
        if (version < requiredVersion(List.of(event)))
            throw new IllegalArgumentException("Ability event requires a newer save format");
        boolean legacy = event.ability() != null && LEGACY_ABILITIES.containsKey(event.abilityId());
        put(entry, "sequence", event.sequence());
        put(entry, "turn", event.turn());
        put(entry, "kind", legacy ? event.abilityId() : event.kind().name());
        if (event.ability() != null && !legacy) put(entry, "abilityId", event.abilityId());
        put(entry, "color", event.color());
        put(entry, "actorId", event.actorId());
        put(entry, "targetId", event.targetId());
        put(entry, "from", cell(event.from()));
        put(entry, "to", cell(event.to()));
        put(entry, "message", event.message());
        if (version >= 7) {
            put(entry, "borrowed", event.borrowed());
            put(entry, "copiedAbility", event.copiedAbility() == null ? null : event.copiedAbility().id());
        }
    }

    public static GameEvent read(Element entry, int version, HeroRegistry heroes) {
        String wireKind = text(entry, "kind");
        Abilities ability = null;
        GameEvent.Kind kind;
        if (LEGACY_ABILITIES.containsKey(wireKind)) {
            require(!entry.hasAttribute("abilityId"), "Ability ID on a legacy event");
            require(version >= LEGACY_ABILITIES.get(wireKind), "Ability event requires a newer format");
            ability = heroes.abilityById(wireKind);
            kind = GameEvent.Kind.ABILITY_USED;
        } else {
            kind = GameEvent.Kind.valueOf(wireKind);
            if (kind == GameEvent.Kind.ABILITY_USED) {
                require(version >= GENERIC_ABILITY_VERSION, "Generic ability event requires format 15");
                ability = heroes.abilityById(text(entry, "abilityId"));
            } else require(!entry.hasAttribute("abilityId"), "Ability ID on a non-active event");
        }
        require(version >= 7 || (!entry.hasAttribute("borrowed") && !entry.hasAttribute("copiedAbility")),
                "Spell metadata requires format 7");
        Abilities copied = entry.hasAttribute("copiedAbility")
                ? heroes.abilityById(text(entry, "copiedAbility")) : null;
        require(copied == null || version >= abilityVersion(copied), "Copied ability requires a newer format");
        return new GameEvent(number(entry, "sequence"), number(entry, "turn"), kind,
                FigureColor.valueOf(text(entry, "color")), optionalLong(entry, "actorId"), optionalLong(entry, "targetId"),
                position(entry, "from"), position(entry, "to"), text(entry, "message"), ability, copied,
                version >= 7 && bool(entry, "borrowed"));
    }

    private static void put(Element entry, String name, Object value) {
        if (value != null) entry.setAttribute(name, value.toString());
    }

    private static String text(Element entry, String name) {
        require(entry.hasAttribute(name), "Missing event." + name);
        String value = entry.getAttribute(name);
        require(value.length() <= 4096, "Attribute too long");
        return value;
    }

    private static long number(Element entry, String name) { return Long.parseLong(text(entry, name)); }
    private static Long optionalLong(Element entry, String name) {
        return entry.hasAttribute(name) ? number(entry, name) : null;
    }
    private static boolean bool(Element entry, String name) {
        String value = text(entry, name);
        require(value.equals("true") || value.equals("false"), "Invalid boolean");
        return Boolean.parseBoolean(value);
    }
    private static Position position(Element entry, String name) {
        if (!entry.hasAttribute(name)) return null;
        String value = text(entry, name);
        require(value.matches("[a-h][1-8]"), "Invalid square");
        return new Position(value.charAt(1) - '1', value.charAt(0) - 'a');
    }
    private static String cell(Position position) {
        return position == null ? null : "" + (char) ('a' + position.getColumn()) + (position.getRow() + 1);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
