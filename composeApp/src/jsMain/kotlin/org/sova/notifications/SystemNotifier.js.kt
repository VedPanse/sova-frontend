package org.sova.notifications

import kotlinx.coroutines.await
import kotlin.js.Promise

actual object SystemNotifier {
    actual suspend fun show(title: String, message: String) {
        runCatching {
            val permission = notificationPermission()
            val granted = if (permission == "granted") {
                true
            } else {
                requestNotificationPermission().await() == "granted"
            }
            if (granted) {
                showBrowserNotification(title, message)
            } else {
                println("Sova notification: browser notification permission is not granted.")
            }
        }.onFailure {
            println("Sova notification failed: ${it.message ?: it::class.simpleName}")
        }
    }
}

private fun notificationPermission(): String =
    js("typeof Notification === 'undefined' ? 'denied' : Notification.permission").unsafeCast<String>()

private fun requestNotificationPermission(): Promise<String> =
    js("typeof Notification === 'undefined' ? Promise.resolve('denied') : Notification.requestPermission()").unsafeCast<Promise<String>>()

private fun showBrowserNotification(title: String, message: String) {
    js("new Notification(title, { body: message })")
}
