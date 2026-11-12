package org.mobachess.presentation

import org.mobachess.render.Point
import kotlin.math.exp
import kotlin.math.sin

/** Только экранное движение. Реальная позиция меняется исключительно через GameSession. */
class PieceDragState {
    var pieceId: Long? = null; private set
    private var held = false
    private var floating = false
    private var age = 0f
    private var returnAge = 0f
    private var pointer = Point(0f, 0f)
    private var pressedAt = pointer
    private var offset = pointer
    private var position = pointer
    private var angle = 0f

    fun begin(id: Long, point: Point, anchor: Point) {
        cancel()
        pieceId = id
        held = true
        pointer = point
        pressedAt = point
        offset = Point(anchor.x - point.x, anchor.y - point.y)
        position = anchor
    }

    fun move(point: Point) {
        if (!held) return
        pointer = point
        val dx = point.x - pressedAt.x
        val dy = point.y - pressedAt.y
        if (dx * dx + dy * dy > 36f) floating = true
    }

    fun release(): Boolean {
        if (!held) return false
        held = false
        if (!floating) { cancel(); return false }
        returnAge = 0f
        return true
    }

    fun update(delta: Float, home: Point?) {
        if (pieceId == null) return
        if (home == null) { cancel(); return }
        val dt = delta.coerceIn(0f, .05f)
        age += dt
        if (held && age >= .13f) floating = true
        if (!floating) return
        val target = if (held) Point(pointer.x + offset.x, pointer.y + offset.y + 16f) else home
        val follow = 1f - exp(-20f * dt)
        position = Point(position.x + (target.x - position.x) * follow,
            position.y + (target.y - position.y) * follow)
        angle = if (held) sin(age * 4.2f) * 2.4f else angle * (1f - follow)
        if (!held) {
            returnAge += dt
            if (returnAge >= .28f) cancel()
        }
    }

    fun isFloating(id: Long): Boolean = pieceId == id && floating
    fun anchor(id: Long, home: Point): Point = if (isFloating(id)) position else home
    fun rotation(id: Long): Float = if (isFloating(id)) angle else 0f

    fun cancel() {
        pieceId = null
        held = false
        floating = false
        age = 0f
        returnAge = 0f
        angle = 0f
    }
}
