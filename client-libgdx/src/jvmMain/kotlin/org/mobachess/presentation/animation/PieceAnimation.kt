package org.mobachess.presentation.animation

import game.history.GameEvent
import game.state.GameSnapshot
import model.Position
import org.mobachess.render.BoardGeometry
import org.mobachess.render.Point

data class PiecePose(val anchor: Point, val ground: Point, val rotation: Float, val depth: Float)

interface PieceAnimation {
    val pieceId: Long
    val destination: Position
    val duration: Float
    fun pose(age: Float, geometry: BoardGeometry): PiecePose
}

fun interface AbilityAnimationFactory {
    fun create(event: GameEvent, before: GameSnapshot, after: GameSnapshot): PieceAnimation?
}
