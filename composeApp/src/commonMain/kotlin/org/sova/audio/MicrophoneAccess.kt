package org.sova.audio

enum class MicrophoneAccessState {
    Granted,
    Denied,
    Unavailable,
}

expect object MicrophoneAccess {
    suspend fun request(): MicrophoneAccessState
}
