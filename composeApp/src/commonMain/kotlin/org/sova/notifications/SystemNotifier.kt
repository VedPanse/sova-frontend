package org.sova.notifications

expect object SystemNotifier {
    suspend fun show(title: String, message: String)
}
