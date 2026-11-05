package game.heroes;

import model.Position;

/** Метаданные успешного применения; запись в журнал публикует ядро. */
public record AbilityEventDetails(Long targetId, Position to, Abilities copiedAbility) {}
