package org.chess

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import game.Game

fun main() = application {
    val game = remember { Game(8) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Custom Chess Game",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        ChessApp(game)
    }
}