package org.sova.audio

actual object MicrophoneAccess {
    actual suspend fun request(): MicrophoneAccessState =
        MicrophoneAccessState.Unavailable
}
