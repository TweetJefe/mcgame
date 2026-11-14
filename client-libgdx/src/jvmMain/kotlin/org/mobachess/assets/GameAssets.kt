package org.mobachess.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Disposable
import game.heroes.HeroType
import game.state.PieceState
import model.FigureColor
import model.figures.Pawn

/** Явная привязка к пользовательским файлам. Оригиналы не переименовываются и не изменяются. */
object HeroArtCatalog {
    data class SpriteFrame(val left: Int, val top: Int, val width: Int, val height: Int, val anchorX: Float)

    // Рамка исключает отдельный пиксель ниже подставки; привязка учитывает выступающее копьё.
    val framing = mapOf(
        "art/heroes/chronomlin/chronomlin.png" to SpriteFrame(77, 109, 780, 698, .56f)
    )

    val files = mapOf(
        HeroType.PUDGE to "pudge/ebisu.png", HeroType.LUMESHARD to "lumeshard/lumeshard.png",
        HeroType.BOUNTY_CROW to "bounty_crow/bountycrow.png", HeroType.BOLOTNIK to "bolotnik/Bolotnik.png",
        HeroType.IZBA to "izba/izba.png", HeroType.BABA_YAGA to "baba_yaga/baba yaga.png",
        HeroType.GINSENG to "ginseng/ginseng.png", HeroType.FAUN to "faun/faun.png",
        HeroType.ARES to "ares/ares.png", HeroType.CLOWNGANGER to "clownganger/clownganger.png",
        HeroType.ILLUSION to "clownganger/clownganger - ultimate.png", HeroType.MOLEMINER to "moleminer/moleminer.png",
        HeroType.ZEUS to "zeus/zeus.png", HeroType.CHRONOMLIN to "chronomlin/chronomlin.png",
        HeroType.KIKIMORA to "kikimora/kikimora.png", HeroType.SUN_WUKONG to "sun_wukong/wukong.png",
        HeroType.STRIX to "strix/strix.png", HeroType.PHOENIX to "phoenix/phoenix.png",
        HeroType.LESHY to "leshy/leshy.png", HeroType.SHIVA to "shiva/shiva.png"
    ).mapValues { "art/heroes/${it.value}" }
}

class GameAssets : Disposable {
    val manager = AssetManager()
    val font: BitmapFont
    val small: BitmapFont
    val pixel: Texture
    private val cropped = mutableMapOf<String, TextureRegion>()
    private val anchors = mutableMapOf<TextureRegion, Float>()
    val backgroundPath = "art/environments/default/backgrounds/background.png"
    val boardPath = "art/environments/default/foregrounds/board.png"
    val hudPath = "art/environments/default/hud/hud-png.png"
    private val regular = buildMap<String, String> {
        for (side in listOf("white", "black")) for (body in listOf("pawn", "knight", "bishop", "rook", "queen", "king")) {
            val name = if (side == "black" && body == "bishop") "blockbishop" else side + body
            put("$side/$body", "art/defualt-figures/$side/$name.png")
        }
    }

    init {
        val paths = HeroArtCatalog.files.values + HeroFormArtCatalog.files.values + regular.values + SkillIconCatalog.paths + listOf(backgroundPath, boardPath, hudPath, "art/animations/ares/ares_skill1.png", "art/animations/ginseng/ginseng_ult1.png", org.mobachess.presentation.animation.WukongCloudEffectFactory.PATH)
        paths.distinct().filter { Gdx.files.internal(it).exists() }.forEach { manager.load(it, Texture::class.java) }
        manager.finishLoading()
        paths.distinct().filter { manager.isLoaded(it) }.forEach { manager.get<Texture>(it).setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSans-Regular.ttf"))
        val chars = FreeTypeFontGenerator.DEFAULT_CHARS + (0x400..0x45f).map(Int::toChar).joinToString("") + "→•—∞"
        font = generator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply { size = 22; characters = chars })
        small = generator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply { size = 15; characters = chars })
        generator.dispose()
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE); pixmap.fill(); pixel = Texture(pixmap); pixmap.dispose()
    }

    fun texture(path: String): Texture = manager.get(path, Texture::class.java)
    fun skillIcon(path: String): TextureRegion? = if (manager.isLoaded(path)) TextureRegion(texture(path)) else null
    fun heroSprite(hero: HeroType): TextureRegion? = HeroArtCatalog.files[hero]
        ?.takeIf { manager.isLoaded(it) }?.let(::croppedSprite)

    fun regularSprite(body: String, color: FigureColor): TextureRegion? {
        val side = if (color == FigureColor.WHITE) "white" else "black"
        return regular["$side/$body"]?.takeIf { manager.isLoaded(it) }?.let(::croppedSprite)
    }
    fun spriteAnchorX(region: TextureRegion): Float = anchors[region] ?: .5f
    fun sprite(piece: PieceState): TextureRegion? {
        val heroPath = HeroFormArtCatalog.pathFor(piece) ?: HeroArtCatalog.files[piece.hero()]
        val body = piece.type().simpleName.lowercase()
        val side = if (piece.color() == FigureColor.WHITE) "white" else "black"
        val path = heroPath?.takeIf { manager.isLoaded(it) } ?: regular["$side/$body"] ?: return null
        return croppedSprite(path)
    }

    /** Общая высота пешек, включая героев, определяется обычной пешкой своей стороны. */
    fun boardHeightRatio(piece: PieceState): Float {
        if (piece.type() != Pawn::class.java) return 1f
        val side = if (piece.color() == FigureColor.WHITE) "white" else "black"
        val pawn = croppedSprite(regular.getValue("$side/pawn"))
        val reference = croppedSprite(regular.getValue("$side/king"))
        return (pawn.regionHeight.toFloat() / reference.regionHeight).coerceAtMost(1f)
    }

    private fun croppedSprite(path: String): TextureRegion = cropped.getOrPut(path) {
            (HeroFormArtCatalog.framing[path] ?: HeroArtCatalog.framing[path])?.let { frame ->
                val region = TextureRegion(texture(path), frame.left, frame.top, frame.width, frame.height)
                anchors[region] = frame.anchorX
                return@getOrPut region
            }
            val image = Pixmap(Gdx.files.internal(path))
            var left = image.width; var right = -1; var bottom = image.height; var top = -1
            for (y in 0 until image.height step 2) for (x in 0 until image.width step 2) {
                if ((image.getPixel(x, y) and 255) > 24) {
                    left = minOf(left, x); right = maxOf(right, x); bottom = minOf(bottom, y); top = maxOf(top, y)
                }
            }
            image.dispose()
            if (right < left) TextureRegion(texture(path))
            else TextureRegion(texture(path), left, bottom, (right - left + 2).coerceAtMost(texture(path).width-left),
                (top - bottom + 2).coerceAtMost(texture(path).height-bottom))
    }

    override fun dispose() { font.dispose(); small.dispose(); pixel.dispose(); manager.dispose() }
}
