package game.heroes.sunwukong;

import java.util.Map;
import java.util.Objects;
import model.Figure;
import model.Position;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Queen;
import model.figures.Rook;

/** Личность клона независима от текущей формы; seed сохраняет случайность предпросмотров. */
public record WukongState(long initialSeed, long randomState, Map<Long, Form> forms, Map<Long, Origin> clones) {
    public enum Body {
        PAWN(Pawn.class, "пешка"), BISHOP(Bishop.class, "слон"), KNIGHT(Knight.class, "конь"),
        ROOK(Rook.class, "ладья"), QUEEN(Queen.class, "ферзь"),
        EGG(model.figures.Egg.class, "яйцо", false), ARCHBISHOP(model.figures.Archbishop.class, "конь + слон", false);
        private final Class<? extends Figure> type;
        private final String displayName;
        private final boolean classic;
        Body(Class<? extends Figure> type, String displayName) { this(type, displayName, true); }
        Body(Class<? extends Figure> type, String displayName, boolean classic) {
            this.type = type;
            this.displayName = displayName;
            this.classic = classic;
        }
        public boolean isClassic() { return classic; }
        public Class<? extends Figure> type() { return type; }
        public String displayName() { return displayName; }
        public static Body of(Class<? extends Figure> type) {
            for (Body body : values()) if (body.type == type) return body;
            throw new IllegalArgumentException("Unsupported temporary body");
        }
    }
    public record Form(Body original, Body chosen, long until) {
        public Form {
            Objects.requireNonNull(original); Objects.requireNonNull(chosen);
            if (until < 1 || (chosen != Body.KNIGHT && chosen != Body.BISHOP && chosen != Body.ROOK))
                throw new IllegalArgumentException("Invalid temporary form");
        }
    }
    public record Origin(long source, long turn, Position square) {
        public Origin {
            if (source < 1 || turn < 1 || square == null || square.getRow() < 0 || square.getRow() > 7
                    || square.getColumn() < 0 || square.getColumn() > 7)
                throw new IllegalArgumentException("Invalid Wukong origin");
        }
    }
    public WukongState {
        forms = Map.copyOf(forms); clones = Map.copyOf(clones);
        if (forms.size() > 100_000 || clones.size() > 100_000
                || forms.keySet().stream().anyMatch(id -> id < 1) || clones.keySet().stream().anyMatch(id -> id < 1))
            throw new IllegalArgumentException("Invalid Wukong state");
    }
    public static WukongState empty() { return new WukongState(0, 0, Map.of(), Map.of()); }
}
