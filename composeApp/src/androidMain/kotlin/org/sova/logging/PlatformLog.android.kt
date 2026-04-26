package org.sova.logging

import android.util.Log

actual object PlatformLog {
    actual fun write(tag: String, message: String) {
        Log.d(tag, message)
    }
}
