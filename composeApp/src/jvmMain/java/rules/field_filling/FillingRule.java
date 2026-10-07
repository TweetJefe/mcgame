package rules.field_filling;

import model.Figure;
import model.Position;

import java.util.List;
import java.util.Map;

public abstract class FillingRule {
    protected Map<Figure, List<Position>> blackRules;
    protected Map<Figure, List<Position>> whiteRules;

    public Map<Figure, List<Position>> getBlackRules() {
        return blackRules;
    }

    public Map<Figure, List<Position>> getWhiteRules() {
        return whiteRules;
    }
}
