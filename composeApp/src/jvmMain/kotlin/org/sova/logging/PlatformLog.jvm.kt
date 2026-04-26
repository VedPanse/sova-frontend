package org.sova.logging

actual object PlatformLog {
    actual fun write(tag: String, message: String) {
        println("$tag $message")
    }
}
