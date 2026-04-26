package org.sova.logging

import kotlin.js.ExperimentalWasmJsInterop

actual object PlatformLog {
    actual fun write(tag: String, message: String) {
        writeConsole("$tag $message")
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(message) => console.log(message)")
private external fun writeConsole(message: String)
