package org.chess

import game.heroes.HeroDefinitions
import game.heroes.HeroType

/** Display metadata is independent of the persisted hero identifier. */
internal fun HeroType.getName(): String = HeroDefinitions.get(this).displayName()
