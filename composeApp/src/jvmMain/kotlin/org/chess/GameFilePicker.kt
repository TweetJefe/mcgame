package org.chess

import java.awt.KeyboardFocusManager
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

/** The picker is injectable so UI tests exercise file actions without opening native dialogs. */
fun interface GameFilePicker {
    fun choose(save: Boolean): Path?
}

object DesktopGameFilePicker : GameFilePicker {
    override fun choose(save: Boolean): Path? {
        val chooser = JFileChooser().apply {
            dialogTitle = if (save) "Сохранить партию" else "Загрузить партию"
            fileFilter = FileNameExtensionFilter("Moba Chess (*.moba.xml)", "xml")
            isAcceptAllFileFilterUsed = false
            if (save) selectedFile = java.io.File("Партия.moba.xml")
        }
        val parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
        val result = if (save) chooser.showSaveDialog(parent) else chooser.showOpenDialog(parent)
        if (result != JFileChooser.APPROVE_OPTION) return null
        var path = chooser.selectedFile.toPath().toAbsolutePath()
        if (save && !path.fileName.toString().endsWith(".xml", ignoreCase = true))
            path = path.resolveSibling(path.fileName.toString() + ".moba.xml")
        if (save && Files.exists(path) && JOptionPane.showConfirmDialog(parent,
                "Заменить файл ${path.fileName}?", "Сохранение партии", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return null
        return path
    }
}
