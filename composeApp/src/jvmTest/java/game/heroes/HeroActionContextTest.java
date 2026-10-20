package game.heroes;

import game.state.PieceState;
import model.Field;
import model.Position;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class HeroActionContextTest {
    @Test public void contextCopiesTheMapAndLooksUpOnlyBoardPiecesByPosition() {
        Field field = new Field(8);
        Position square = new Position(0, 0);
        PieceState board = PieceState.initial(1, field.getFigureByPosition(square), HeroType.PUDGE_ROOK);
        PieceState swallowed = PieceState.initial(2, field.getFigureByPosition(new Position(1, 0)), HeroType.NONE)
                .hidden(PieceState.Status.SWALLOWED);
        PieceState captured = PieceState.initial(3, field.getFigureByPosition(new Position(1, 1)), HeroType.NONE)
                .hidden(PieceState.Status.CAPTURED);
        var pieces = new HashMap<>(Map.of(1L, board, 2L, swallowed, 3L, captured));
        var context = new HeroActionContext(field, pieces, 42);
        pieces.clear();

        assertSame(field, context.field());
        assertEquals(42, context.currentTurn());
        assertEquals(board, context.pieceAt(new Position(0, 0)));
        assertEquals(swallowed, context.pieceById(2));
        assertEquals(captured, context.pieceById(3));
        assertNull(context.pieceAt(new Position(1, 0)));
        assertNull(context.pieceAt(new Position(1, 1)));
        assertNull(context.pieceAt(null));
        assertNull(context.pieceAt(new Position(9, 9)));
        assertNull(context.pieceById(999));
        assertThrows(UnsupportedOperationException.class, () -> context.pieces().clear());
    }

    @Test public void contextRejectsMissingSnapshotsAndNullMapEntries() {
        Field field = new Field(8);
        assertThrows(NullPointerException.class, () -> new HeroActionContext(null, Map.of(), 0));
        assertThrows(NullPointerException.class, () -> new HeroActionContext(field, null, 0));
        var pieces = new HashMap<Long, PieceState>();
        pieces.put(1L, null);
        assertThrows(NullPointerException.class, () -> new HeroActionContext(field, pieces, 0));
    }
}
