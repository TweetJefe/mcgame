package game.mechanics;

import game.state.PieceState;
import model.Position;

public record MoveIntent(PieceState actor, Position from, Position to, boolean castling) {}
