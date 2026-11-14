package org.mobachess.assets

import game.heroes.HeroType
import game.state.PieceState
import model.Figure
import model.FigureColor
import model.figures.Bishop
import model.figures.Knight
import model.figures.Rook

/** Модель зависит от личности героя, текущего тела и стороны, включая созданных клонов. */
object HeroFormArtCatalog {
    data class Key(val hero: HeroType, val body: Class<out Figure>, val side: FigureColor)
    private const val ROOT = "art/animations/wukong/first_skill_models/"

    val files = mapOf(
        Key(HeroType.SUN_WUKONG, Bishop::class.java, FigureColor.WHITE) to "${ROOT}wukong_white_bishop.png",
        Key(HeroType.SUN_WUKONG, Knight::class.java, FigureColor.WHITE) to "${ROOT}wukong_white_knight.png",
        Key(HeroType.SUN_WUKONG, Rook::class.java, FigureColor.WHITE) to "${ROOT}wukong_white_rook.png",
        Key(HeroType.SUN_WUKONG, Bishop::class.java, FigureColor.BLACK) to "${ROOT}wukong_black_bishop.png",
        Key(HeroType.SUN_WUKONG, Knight::class.java, FigureColor.BLACK) to "${ROOT}wukong_black_kngiht.png",
        Key(HeroType.SUN_WUKONG, Rook::class.java, FigureColor.BLACK) to "${ROOT}wukong_black_rook.png"
    )

    // Подставка центрирована на x=512 исходных изображений; ленты не смещают фигуру на доске.
    private fun frame(left: Int, top: Int, right: Int, bottom: Int) =
        HeroArtCatalog.SpriteFrame(left, top, right - left, bottom - top, (512f - left) / (right - left))

    val framing = mapOf(
        "${ROOT}wukong_white_bishop.png" to frame(332, 48, 858, 827),
        "${ROOT}wukong_white_knight.png" to frame(288, 36, 875, 827),
        "${ROOT}wukong_white_rook.png" to frame(332, 72, 912, 827),
        "${ROOT}wukong_black_bishop.png" to frame(332, 47, 860, 828),
        "${ROOT}wukong_black_kngiht.png" to frame(288, 23, 896, 828),
        "${ROOT}wukong_black_rook.png" to frame(332, 71, 820, 828)
    )

    fun pathFor(piece: PieceState): String? = files[Key(piece.hero(), piece.type(), piece.color())]
}
