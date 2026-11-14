package game.core;

import game.creation.CreatedBody;
import game.creation.CreationMechanic;
import game.creation.PieceBirth;
import game.heroes.HeroPieceFactory;
import game.heroes.HeroType;
import game.moves.MovementRejection;
import game.state.PieceState;
import java.util.HashMap;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.Pawn;
import rules.standard.MoveValidator;

/** Подготовка размещения: новая идентичность, локальная инициализация и обычные реакции входа. */
final class BoardEditor {
    record Placement(ActionEngine.Preview preview, long actor) {}
    private BoardEditor() {}

    static Placement prepare(ActionEngine.Context before, Class<? extends Figure> body,
                             HeroType hero, FigureColor color, Position square, FigureColor active) {
        if (!MoveValidator.inside(before.field(), square)) return rejected(MovementRejection.OUT_OF_BOUNDS);
        if (before.at(square) != null) return rejected(MovementRejection.OWN_PIECE_AT_TARGET);
        if (color != FigureColor.WHITE && color != FigureColor.BLACK) return rejected(MovementRejection.WRONG_FIGURE_COLOR);
        if (hero == null || body == null || hero != HeroType.NONE && !before.heroes().get(hero).supports(body))
            return rejected(MovementRejection.INVALID_BOARD);
        try {
            long id = before.pieces().keySet().stream().mapToLong(Long::longValue).max().orElse(0) + 1;
            var figure = before.field().createFigure(body, square, color);
            var factory = new HeroPieceFactory(before.heroes());
            var piece = factory.initial(id, figure, hero);
            var pieces = new HashMap<>(before.pieces());
            var mechanics = before.mechanics();
            var evolution = before.heroes().revival(piece, before.heroContext()).evolution();
            if (evolution != null) {
                // Служебный источник не находится на доске и не является погибшей фигурой для воскрешения.
                var source = factory.initial(id, before.field().createFigure(Pawn.class, square, color), HeroType.NONE)
                        .hidden(PieceState.Status.REPLACED);
                pieces.put(id, source);
                piece = factory.initial(++id, figure, hero);
                var state = mechanics.get(CreationMechanic.KEY);
                var births = new HashMap<>(state.births());
                births.put(id, new PieceBirth(id, source.id(), null, before.turn(), hero, CreatedBody.of(body),
                        piece.startingColor(), 0, square, "editor.place", evolution));
                mechanics = mechanics.with(CreationMechanic.KEY,
                        new CreationMechanic.State(births, state.dismissed(), state.evolved()));
            }
            pieces.put(id, piece);
            return new Placement(ActionEngine.place(before, pieces, mechanics, active), id);
        } catch (IllegalArgumentException exception) {
            return rejected(MovementRejection.INVALID_BOARD);
        }
    }

    private static Placement rejected(game.moves.MoveResult reason) {
        return new Placement(ActionEngine.Preview.rejected(reason), 0);
    }
}
