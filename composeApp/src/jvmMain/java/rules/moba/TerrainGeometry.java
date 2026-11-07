package rules.moba;

import model.Figure;
import model.Position;
import model.figures.Knight;
import model.figures.Archbishop;
import java.util.Set;

/** Blocks intermediate squares only; entering the destination is resolved as a trap reaction. */
public final class TerrainGeometry {
    private TerrainGeometry() {}
    public static boolean blocks(Position from, Position to, Class<? extends Figure> type, Set<Position> mud) {
        if (mud.isEmpty() || from == null || to == null) return false;
        int dr = to.getRow() - from.getRow(), dc = to.getColumn() - from.getColumn();
        if ((type == Knight.class || type == Archbishop.class) && ((Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2))) {
            int r = Math.abs(dr) == 2 ? Integer.signum(dr) : 0;
            int c = Math.abs(dc) == 2 ? Integer.signum(dc) : 0;
            return mud.contains(new Position(from.getRow()+r, from.getColumn()+c))
                    || mud.contains(new Position(from.getRow()+2*r, from.getColumn()+2*c));
        }
        return blocksRay(from, to, mud);
    }
    public static boolean isKnightLeap(Class<? extends Figure> type, Position from, Position to) {
        if (type != Knight.class && type != Archbishop.class) return false;
        int rows = Math.abs(to.getRow() - from.getRow()), columns = Math.abs(to.getColumn() - from.getColumn());
        return rows == 2 && columns == 1 || rows == 1 && columns == 2;
    }
    public static boolean blocksPassage(Position from, Position to, Class<? extends Figure> type, Set<Position> obstacles) {
        return !isKnightLeap(type, from, to) && blocksRay(from, to, obstacles);
    }
    public static boolean blocksRay(Position from, Position to, Set<Position> mud) {
        int dr = to.getRow() - from.getRow(), dc = to.getColumn() - from.getColumn();
        if (dr != 0 && dc != 0 && Math.abs(dr) != Math.abs(dc)) return false;
        int distance = Math.max(Math.abs(dr), Math.abs(dc));
        for (int step = 1; step < distance; step++)
            if (mud.contains(new Position(from.getRow()+Integer.signum(dr)*step, from.getColumn()+Integer.signum(dc)*step))) return true;
        return false;
    }
}
