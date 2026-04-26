package org.sova.audio

import java.util.Base64
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

actual object SpecialistCallAudio {
    actual fun microphoneChunks(): Flow<String> = flow {
        val format = AudioFormat(16_000f, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        if (!AudioSystem.isLineSupported(info)) {
            println("Sova specialist call: desktop microphone PCM capture is unavailable.")
            return@flow
        }
        val line = runCatching {
            (AudioSystem.getLine(info) as TargetDataLine).apply { open(format) }
        }.getOrElse {
            println("Sova specialist call: could not open desktop microphone stream: ${it.message ?: it::class.simpleName}")
            return@flow
        }

        try {
            val buffer = ByteArray(8_000)
            line.start()
            while (currentCoroutineContext().isActive) {
                val read = line.read(buffer, 0, buffer.size)
                if (read > 0) {
                    emit(Base64.getEncoder().encodeToString(buffer.copyOf(read)))
                }
            }
        } finally {
            line.stop()
            line.close()
        }
    }

    actual suspend fun playAgentAudio(audioBase64: String, format: String) {
        println("Sova specialist call: received $format audio (${audioBase64.length} base64 chars).")
    }
}
