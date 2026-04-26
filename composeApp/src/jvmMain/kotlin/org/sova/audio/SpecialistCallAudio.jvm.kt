package org.sova.audio

import java.io.File
import java.util.Base64
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.sova.logging.SovaLogger

actual object SpecialistCallAudio {
    actual fun microphoneChunks(): Flow<String> = flow {
        SovaLogger.event(subsystem = "mic", event = "desktop-capture-flow-created")
        val format = AudioFormat(16_000f, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        if (!AudioSystem.isLineSupported(info)) {
            SovaLogger.event(subsystem = "mic", event = "desktop-capture-unavailable")
            return@flow
        }
        val line = runCatching {
            (AudioSystem.getLine(info) as TargetDataLine).apply { open(format) }
        }.getOrElse {
            SovaLogger.event(
                subsystem = "mic",
                event = "desktop-capture-open-failed",
                details = mapOf("error" to (it.message ?: it::class.simpleName)),
            )
            return@flow
        }

        try {
            val buffer = ByteArray(8_000)
            line.start()
            SovaLogger.event(subsystem = "mic", event = "desktop-capture-started")
            var firstChunkLogged = false
            while (currentCoroutineContext().isActive) {
                val read = line.read(buffer, 0, buffer.size)
                if (read > 0) {
                    if (!firstChunkLogged) {
                        SovaLogger.event(
                            subsystem = "mic",
                            event = "desktop-capture-first-read",
                            details = mapOf("bytes" to read.toString()),
                        )
                        firstChunkLogged = true
                    }
                    emit(Base64.getEncoder().encodeToString(buffer.copyOf(read)))
                }
            }
        } finally {
            SovaLogger.event(subsystem = "mic", event = "desktop-capture-closing")
            line.stop()
            line.close()
        }
    }

    actual suspend fun playAgentAudio(audioBase64: String, format: String) = withContext(Dispatchers.IO) {
        val normalizedFormat = format.lowercase().substringBefore(";").ifBlank { "mp3" }
        val audioBytes = runCatching { Base64.getDecoder().decode(audioBase64) }.getOrElse {
            SovaLogger.event(
                subsystem = "specialist-call",
                event = "agent-audio-playback-decode-failed",
                details = mapOf("format" to normalizedFormat, "error" to (it.message ?: it::class.simpleName)),
            )
            return@withContext
        }
        SovaLogger.event(
            subsystem = "specialist-call",
            event = "agent-audio-playback-start",
            details = mapOf("format" to normalizedFormat, "bytes" to audioBytes.size.toString()),
        )

        val played = if (isMacOs()) {
            playWithAfplay(audioBytes, normalizedFormat)
        } else if (normalizedFormat == "wav") {
            playWavWithJavaSound(audioBytes)
        } else {
            false
        }

        SovaLogger.event(
            subsystem = "specialist-call",
            event = if (played) "agent-audio-playback-succeeded" else "agent-audio-playback-unsupported",
            details = mapOf("format" to normalizedFormat),
        )
    }

    private fun isMacOs(): Boolean =
        System.getProperty("os.name").lowercase().contains("mac")

    private fun playWithAfplay(audioBytes: ByteArray, format: String): Boolean {
        val suffix = when (format) {
            "wav" -> ".wav"
            "mp3" -> ".mp3"
            else -> ".$format"
        }
        val tempFile = File.createTempFile("sova-agent-audio-", suffix)
        return try {
            tempFile.writeBytes(audioBytes)
            val exitCode = ProcessBuilder("afplay", tempFile.absolutePath)
                .redirectErrorStream(true)
                .start()
                .waitFor()
            exitCode == 0
        } catch (error: Throwable) {
            SovaLogger.event(
                subsystem = "specialist-call",
                event = "agent-audio-afplay-failed",
                details = mapOf("error" to (error.message ?: error::class.simpleName)),
            )
            false
        } finally {
            tempFile.delete()
        }
    }

    private fun playWavWithJavaSound(audioBytes: ByteArray): Boolean {
        val tempFile = File.createTempFile("sova-agent-audio-", ".wav")
        return try {
            tempFile.writeBytes(audioBytes)
            AudioSystem.getAudioInputStream(tempFile).use { input ->
                val format = input.format
                val info = DataLine.Info(SourceDataLine::class.java, format)
                val line = (AudioSystem.getLine(info) as SourceDataLine).apply { open(format) }
                line.use {
                    it.start()
                    val buffer = ByteArray(4096)
                    while (true) {
                        val read = input.read(buffer, 0, buffer.size)
                        if (read <= 0) break
                        it.write(buffer, 0, read)
                    }
                    it.drain()
                    it.stop()
                }
            }
            true
        } catch (error: Throwable) {
            SovaLogger.event(
                subsystem = "specialist-call",
                event = "agent-audio-javasound-failed",
                details = mapOf("error" to (error.message ?: error::class.simpleName)),
            )
            false
        } finally {
            tempFile.delete()
        }
    }
}
