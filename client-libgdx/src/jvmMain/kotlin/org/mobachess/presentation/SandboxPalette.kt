package org.mobachess.presentation

import game.heroes.HeroRules
import game.heroes.HeroType
import model.Figure
import model.figures.*

enum class SandboxGroup(val title: String) {
    FIGURES("Фигуры"), PAWNS("Пешки"), KNIGHTS("Кони"), BISHOPS("Слоны"), ROOKS("Ладьи"), SPECIAL("Особые")
}
data class SandboxPiece(val body: Class<out Figure>, val hero: HeroType, val title: String)

/** Каталог строится из определений героев; специальных диспетчеров их поведения здесь нет. */
class SandboxPalette(rules: HeroRules) {
    private val regular = listOf(
        SandboxPiece(Pawn::class.java,HeroType.NONE,"Пешка"), SandboxPiece(Knight::class.java,HeroType.NONE,"Конь"),
        SandboxPiece(Bishop::class.java,HeroType.NONE,"Слон"), SandboxPiece(Rook::class.java,HeroType.NONE,"Ладья"),
        SandboxPiece(Queen::class.java,HeroType.NONE,"Ферзь"), SandboxPiece(King::class.java,HeroType.NONE,"Король")
    )
    private val heroes = rules.definitions().filter { it.type()!=HeroType.NONE && it.type()!=HeroType.ILLUSION }
        .map { SandboxPiece(it.supportedBodies().single(), it.type(), it.displayName()) }

    fun pieces(group: SandboxGroup): List<SandboxPiece> {
        if (group==SandboxGroup.FIGURES) return regular
        return heroes.filter { piece -> when(group) {
            SandboxGroup.PAWNS -> piece.body==Pawn::class.java
            SandboxGroup.KNIGHTS -> piece.body==Knight::class.java
            SandboxGroup.BISHOPS -> piece.body==Bishop::class.java
            SandboxGroup.ROOKS -> piece.body==Rook::class.java
            SandboxGroup.SPECIAL -> piece.body !in listOf(Pawn::class.java,Knight::class.java,Bishop::class.java,Rook::class.java)
            else -> false
        } }
    }
}
