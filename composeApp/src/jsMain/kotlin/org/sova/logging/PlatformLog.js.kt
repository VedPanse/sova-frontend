package org.sova.logging

actual object PlatformLog {
    actual fun write(tag: String, message: String) {
        console.log("$tag $message")
    }
}
