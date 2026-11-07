package game.heroes.sunwukong;

import game.mechanics.MechanicStateCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import model.Position;

/** Версия локальной схемы не зависит от имён Java-классов. */
public final class WukongStateCodec implements MechanicStateCodec<WukongState> {
    @Override public Map<String, String> encode(WukongState state) {
        String forms = state.forms().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "," + e.getValue().original() + "," + e.getValue().chosen() + "," + e.getValue().until())
                .collect(Collectors.joining(";"));
        String clones = state.clones().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "," + e.getValue().source() + "," + e.getValue().turn() + ","
                        + e.getValue().square().getRow() + "," + e.getValue().square().getColumn())
                .collect(Collectors.joining(";"));
        return Map.of("version", "1", "initialSeed", Long.toString(state.initialSeed()),
                "randomState", Long.toString(state.randomState()), "forms", forms, "clones", clones);
    }

    @Override public WukongState decode(Map<String, String> fields) {
        if (!fields.keySet().equals(Set.of("version", "initialSeed", "randomState", "forms", "clones"))
                || !"1".equals(fields.get("version"))) throw new IllegalArgumentException("Invalid Wukong schema");
        var forms = new HashMap<Long, WukongState.Form>();
        if (!fields.get("forms").isEmpty()) for (String line : fields.get("forms").split(";", -1)) {
            String[] item = line.split(",", -1);
            if (item.length != 4 || forms.putIfAbsent(Long.parseLong(item[0]), new WukongState.Form(
                    WukongState.Body.valueOf(item[1]), WukongState.Body.valueOf(item[2]), Long.parseLong(item[3]))) != null)
                throw new IllegalArgumentException("Invalid Wukong form entry");
        }
        var clones = new HashMap<Long, WukongState.Origin>();
        if (!fields.get("clones").isEmpty()) for (String line : fields.get("clones").split(";", -1)) {
            String[] item = line.split(",", -1);
            if (item.length != 5 || clones.putIfAbsent(Long.parseLong(item[0]), new WukongState.Origin(
                    Long.parseLong(item[1]), Long.parseLong(item[2]), new Position(Integer.parseInt(item[3]), Integer.parseInt(item[4])))) != null)
                throw new IllegalArgumentException("Invalid Wukong clone entry");
        }
        return new WukongState(Long.parseLong(fields.get("initialSeed")), Long.parseLong(fields.get("randomState")), forms, clones);
    }
}
