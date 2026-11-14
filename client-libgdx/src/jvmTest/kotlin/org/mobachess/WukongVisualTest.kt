package org.mobachess

import game.core.Game
import game.heroes.HeroType
import game.heroes.sunwukong.SunWukongAbility
import game.history.GameEvent
import model.FigureColor
import model.Position
import model.figures.Pawn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mobachess.assets.HeroFormArtCatalog
import org.mobachess.presentation.animation.SpriteEffectTimeline
import org.mobachess.presentation.animation.WukongCloudEffectFactory

class WukongVisualTest {
    private val origin = Position(1, 2)
    private val target = Position(1, 5)

    private fun game(side: FigureColor = FigureColor.WHITE): Game = Game.sandbox().also { game ->
        assertTrue(game.placeEditorPiece(Pawn::class.java, HeroType.SUN_WUKONG, side, origin).isSuccess)
        assertTrue(game.placeEditorPiece(Pawn::class.java, HeroType.NONE, side, target).isSuccess)
        if (side == FigureColor.BLACK) assertTrue(game.endTurn(game.currentPlayer).isSuccess)
    }

    @Test fun shapeshiftUsesTheChosenBodyAndSideAndRestoresThePawnArtWhenItExpires() {
        for (side in listOf(FigureColor.WHITE, FigureColor.BLACK)) {
            val game = game(side)
            assertNull(HeroFormArtCatalog.pathFor(game.getPieceState(origin)))
            assertTrue(game.useAbility(game.currentPlayer, origin, null, SunWukongAbility.SHAPESHIFT).isSuccess)
            val form = game.getPieceState(origin)
            assertNotNull(HeroFormArtCatalog.pathFor(form))
            val filename = HeroFormArtCatalog.pathFor(form)!!
            assertTrue(filename.contains(if (side == FigureColor.WHITE) "white" else "black"))
            // Имена файлов пользователя сохраняются, включая kngiht у чёрного коня.
            val body = if (side == FigureColor.BLACK && form.type().simpleName == "Knight") "kngiht"
                else form.type().simpleName.lowercase()
            assertTrue(filename.endsWith("_$body.png"))
            repeat(3) { assertTrue(game.endTurn(game.currentPlayer).isSuccess) }
            assertEquals(Pawn::class.java, game.getPieceState(origin).type())
            assertNull(HeroFormArtCatalog.pathFor(game.getPieceState(origin)))
        }
    }

    @Test fun cloudAppearsOnCasterForSkillAndOnTheSelectedPawnForUltimate() {
        val game = game()
        val timeline = SpriteEffectTimeline(WukongCloudEffectFactory())
        timeline.update(game, game.snapshot, 0f)
        assertTrue(game.useAbility(game.currentPlayer, origin, null, SunWukongAbility.SHAPESHIFT).isSuccess)
        timeline.update(game, game.snapshot, 0f)
        assertEquals(origin, timeline.active.single().effect.square)
        timeline.update(game, game.snapshot, WukongCloudEffectFactory.DURATION)
        assertTrue(timeline.active.isEmpty())
        assertTrue(game.useAbility(game.currentPlayer, origin, target, SunWukongAbility.CLONE).isSuccess)
        timeline.update(game, game.snapshot, 0f)
        assertEquals(target, timeline.active.single().effect.square)
        assertEquals(HeroType.SUN_WUKONG, game.getPieceState(target).hero())
        assertTrue(game.useAbility(game.currentPlayer, target, null, SunWukongAbility.SHAPESHIFT).isSuccess)
        assertNotNull(HeroFormArtCatalog.pathFor(game.getPieceState(target)))
    }

    @Test fun bothModelsSwitchExactlyAtTheStartOfFrameEightWithoutDelayingGameRules() {
        for (clone in listOf(false, true)) {
            val game = game()
            val square = if (clone) target else origin
            val previousModel = game.getPieceState(square)
            val timeline = SpriteEffectTimeline(WukongCloudEffectFactory())
            timeline.update(game, game.snapshot, 0f)
            assertTrue(game.useAbility(game.currentPlayer, origin, if (clone) target else null,
                if (clone) SunWukongAbility.CLONE else SunWukongAbility.SHAPESHIFT).isSuccess)
            val actual = game.getPieceState(square)
            val confirmed = game.snapshot
            timeline.update(game, confirmed, 0f)
            assertEquals(previousModel, timeline.modelFor(actual, 16))
            val switchTime = 7f / 16f * WukongCloudEffectFactory.DURATION
            timeline.update(game, confirmed, switchTime - .001f)
            assertEquals(6, timeline.active.single().frameIndex(16))
            assertEquals(previousModel, timeline.modelFor(actual, 16))
            timeline.update(game, confirmed, .001f)
            assertEquals(7, timeline.active.single().frameIndex(16))
            assertEquals(actual, timeline.modelFor(actual, 16))
            assertEquals(confirmed, game.snapshot)
            timeline.update(game, confirmed, WukongCloudEffectFactory.DURATION)
            assertTrue(timeline.active.isEmpty())
            assertEquals(actual, timeline.modelFor(actual, 16))
        }
    }

    @Test fun aCloneCastingImmediatelyDoesNotRevealItsModelBeforeEitherAnimationReachesFrameEight() {
        val game = game()
        val pawn = game.getPieceState(target)
        val timeline = SpriteEffectTimeline(WukongCloudEffectFactory())
        timeline.update(game, game.snapshot, 0f)
        assertTrue(game.useAbility(game.currentPlayer, origin, target, SunWukongAbility.CLONE).isSuccess)
        timeline.update(game, game.snapshot, 0f)
        val clone = game.getPieceState(target)
        timeline.update(game, game.snapshot, .2f)
        assertTrue(game.useAbility(game.currentPlayer, target, null, SunWukongAbility.SHAPESHIFT).isSuccess)
        timeline.update(game, game.snapshot, 0f)
        val form = game.getPieceState(target)
        assertEquals(pawn, timeline.modelFor(form, 16))
        timeline.update(game, game.snapshot, .25f)
        assertEquals(clone, timeline.modelFor(form, 16))
        timeline.update(game, game.snapshot, .2f)
        assertEquals(form, timeline.modelFor(form, 16))
        timeline.update(Any(), game.snapshot, 0f)
        assertEquals(form, timeline.modelFor(form, 16))
    }

    @Test fun historicalRejectedAndRepeatedEventsDoNotReplayAndSessionResetClearsClouds() {
        val game = game()
        assertTrue(game.useAbility(game.currentPlayer, origin, null, SunWukongAbility.SHAPESHIFT).isSuccess)
        val timeline = SpriteEffectTimeline(WukongCloudEffectFactory())
        timeline.update(game, game.snapshot, 0f)
        assertTrue(timeline.active.isEmpty())
        assertFalse(game.useAbility(game.currentPlayer, origin, origin, SunWukongAbility.CLONE).isSuccess)
        timeline.update(game, game.snapshot, 0f)
        assertTrue(timeline.active.isEmpty())
        assertTrue(game.useAbility(game.currentPlayer, origin, target, SunWukongAbility.CLONE).isSuccess)
        timeline.update(game, game.snapshot, 0f)
        timeline.update(game, game.snapshot, 0f)
        assertEquals(1, timeline.active.size)
        timeline.update(Any(), game.snapshot, 0f)
        assertTrue(timeline.active.isEmpty())
        val copiedCast = GameEvent.abilityUsed(1, 1, SunWukongAbility.SHAPESHIFT, FigureColor.WHITE,
            1L, null, origin, null, "Копия", null, true)
        assertEquals(origin, WukongCloudEffectFactory().create(copiedCast, game.snapshot, game.snapshot)?.square)
    }
}
