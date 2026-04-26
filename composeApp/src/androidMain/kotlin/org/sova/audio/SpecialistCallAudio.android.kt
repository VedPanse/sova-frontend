package org.sova.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual object SpecialistCallAudio {
    actual fun microphoneChunks(): Flow<String> = emptyFlow()
    actual suspend fun playAgentAudio(audioBase64: String, format: String) = Unit
}

