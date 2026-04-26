package org.sova.audio

import kotlinx.coroutines.flow.Flow

expect object SpecialistCallAudio {
    fun microphoneChunks(): Flow<String>
    suspend fun playAgentAudio(audioBase64: String, format: String)
    fun stopAgentAudio(reason: String)
}
