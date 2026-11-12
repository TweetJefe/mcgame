package org.mobachess.render

import model.Position

data class Point(val x: Float, val y: Float)

/** Привязка к board.png 1024×1024: линии рядов измерены отдельно, без растягивания PNG. */
class BoardGeometry {
    var flipped = false
    val x = 185f
    val y = 25f
    val width = 950f
    val height = 950f

    // Координаты линий от первой горизонтали к восьмой, отсчитанные от верха исходного PNG.
    private val imageRows = floatArrayOf(785f, 697f, 612f, 532f, 455f, 383f, 312f, 244f, 178f)
    private val rows = imageRows.map { 1f - it / 1024f }
    private val nearLeft = 84f / 1024f
    private val nearRight = 940f / 1024f
    private val farLeft = 176f / 1024f
    private val farRight = 857f / 1024f

    private fun rankY(rank: Float): Float {
        val index = rank.toInt().coerceIn(0, 7)
        return rows[index] + (rows[index + 1] - rows[index]) * (rank - index)
    }

    private fun edges(localY: Float): Pair<Float, Float> {
        val t = (localY - rows.first()) / (rows.last() - rows.first())
        return (nearLeft + (farLeft - nearLeft) * t) to (nearRight + (farRight - nearRight) * t)
    }

    fun point(file: Float, rank: Float): Point {
        val localY = rankY(rank)
        val (left, right) = edges(localY)
        return Point(x + width * (left + (right - left) * file / 8f), y + height * localY)
    }

    fun display(square: Position) = if (flipped) Position(7 - square.row, 7 - square.column) else square

    fun anchor(square: Position): Point = display(square).let { point(it.column + .5f, it.row + .26f) }

    fun corners(square: Position): List<Point> = display(square).let {
        listOf(point(it.column.toFloat(), it.row.toFloat()), point(it.column + 1f, it.row.toFloat()),
            point(it.column + 1f, it.row + 1f), point(it.column.toFloat(), it.row + 1f))
    }

    fun cell(px: Float, py: Float): Position? {
        val localY = (py - y) / height
        if (localY < rows.first() || localY >= rows.last()) return null
        val (left, right) = edges(localY)
        val u = ((px - x) / width - left) / (right - left)
        if (u < 0f || u >= 1f) return null
        val row = (0..7).first { localY < rows[it + 1] }
        val col = (u * 8).toInt().coerceIn(0, 7)
        return if (flipped) Position(7 - row, 7 - col) else Position(row, col)
    }
}
