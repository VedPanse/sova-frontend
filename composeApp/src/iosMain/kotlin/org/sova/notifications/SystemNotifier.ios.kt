package org.sova.notifications

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUUID
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

actual object SystemNotifier {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun show(title: String, message: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val granted = suspendCancellableCoroutine { continuation ->
            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { allowed, _ ->
                if (continuation.isActive) continuation.resume(allowed)
            }
        }
        if (!granted) {
            println("Sova notification: iOS notification permission is not granted.")
            return
        }

        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            setSound(platform.UserNotifications.UNNotificationSound.defaultSound())
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(0.1, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "sova-${NSUUID().UUIDString}",
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request) { error ->
            error?.let { println("Sova notification failed: ${it.localizedDescription}") }
        }
    }
}
