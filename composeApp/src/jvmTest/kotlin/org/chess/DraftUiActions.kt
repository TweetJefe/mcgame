package org.chess

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType

/** Scrolls the catalog to its last hero regardless of the platform's mouse-wheel scaling. */
internal fun ImageComposeScene.scrollDraftToLastHero() {
    sendPointerEvent(PointerEventType.Scroll, Offset(300f, 500f), scrollDelta = Offset(0f, 1000f))
    // ImageComposeScene starts its frame clock at zero; advance it until scrolling settles.
    repeat(60) { frame -> render((frame + 1) * 16_666_667L).close() }
}
