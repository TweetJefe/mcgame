package org.mobachess.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

fun main() {
    val config=Lwjgl3ApplicationConfiguration().apply {
        setTitle("Moba Chess")
        setWindowedMode(1440,810)
        setWindowSizeLimits(960,540,-1,-1)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(MobaChessApp(),config)
}
