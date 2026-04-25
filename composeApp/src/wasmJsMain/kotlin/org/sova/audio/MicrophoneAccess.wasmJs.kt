package org.sova.audio

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsNumber
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
actual object MicrophoneAccess {
    actual suspend fun request(): MicrophoneAccessState =
        when (requestBrowserMicrophone().await<JsNumber>().toDouble().toInt()) {
            1 -> MicrophoneAccessState.Granted
            -1 -> MicrophoneAccessState.Unavailable
            else -> MicrophoneAccessState.Denied
        }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    () => {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            return Promise.resolve(-1);
        }
        return navigator.mediaDevices
            .getUserMedia({ audio: true })
            .then((stream) => {
                stream.getTracks().forEach((track) => track.stop());
                return 1;
            })
            .catch(() => 0);
    }
    """,
)
private external fun requestBrowserMicrophone(): Promise<JsNumber>
