package game.heroes.compat;

import game.heroes.Abilities;
import game.heroes.HeroEffect;
import game.state.PieceState;
import model.Field;
import model.Position;

/** Старый статический предпросмотр без полного запроса и состояний механик. */
public interface LegacyPreview {
    HeroEffect legacyPreview(Field field, PieceState actor, PieceState target, Position square, Abilities ability);
}
