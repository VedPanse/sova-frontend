package org.sova.audio

import kotlinx.coroutines.await
import kotlin.js.Promise

actual object MicrophoneAccess {
    actual suspend fun request(): MicrophoneAccessState =
        runCatching {
            js("navigator.mediaDevices.getUserMedia({ audio: true })")
                .unsafeCast<Promise<dynamic>>()
                .await()
            MicrophoneAccessState.Granted
        }.getOrElse {
            MicrophoneAccessState.Denied
        }
}
