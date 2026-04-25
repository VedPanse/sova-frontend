package org.sova.audio

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession

actual object MicrophoneAccess {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun request(): MicrophoneAccessState =
        suspendCancellableCoroutine { continuation ->
            AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                if (continuation.isActive) {
                    continuation.resume(if (granted) MicrophoneAccessState.Granted else MicrophoneAccessState.Denied)
                }
            }
        }
}
