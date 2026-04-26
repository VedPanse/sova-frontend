package org.sova.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.sova.logging.SovaLogger

actual object SpecialistCallAudio {
    actual fun microphoneChunks(): Flow<String> {
        SovaLogger.event(subsystem = "mic", event = "audio-capture-not-implemented", details = mapOf("platform" to "android"))
        return emptyFlow()
    }
    actual suspend fun playAgentAudio(audioBase64: String, format: String) {
        SovaLogger.event(subsystem = "specialist-call", event = "agent-audio-playback-not-implemented", details = mapOf("platform" to "android", "format" to format))
    }
}
