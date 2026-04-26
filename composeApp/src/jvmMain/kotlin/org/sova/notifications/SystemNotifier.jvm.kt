package org.sova.notifications

import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.image.BufferedImage

actual object SystemNotifier {
    actual suspend fun show(title: String, message: String) {
        runCatching {
            Toolkit.getDefaultToolkit().beep()
            if (!SystemTray.isSupported()) {
                println("Sova notification: $title - $message")
                return
            }
            val tray = SystemTray.getSystemTray()
            val icon = TrayIcon(BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), "Sova").apply {
                isImageAutoSize = true
            }
            tray.add(icon)
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
        }.onFailure {
            println("Sova notification failed: ${it.message ?: it::class.simpleName}")
        }
    }
}
