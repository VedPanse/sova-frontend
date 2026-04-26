package org.sova.audio

import java.io.File
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
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
    private const val AudioEndMarker = "__SOVA_AUDIO_END__"
    private const val SpeechStartMarker = "__SOVA_SPEECH_START__"
    private val activePlayback = AtomicReference<Process?>(null)
    private val activePlaybackLine = AtomicReference<SourceDataLine?>(null)

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
            val buffer = ByteArray(1_600)
            line.start()
            SovaLogger.event(subsystem = "mic", event = "desktop-capture-started")
            var firstChunkLogged = false
            var speechActive = false
            var speechChunks = 0
            var silentChunksAfterSpeech = 0
            var noiseFloor = 140.0
            var lastLoggedChunk = 0
            var highEnergyChunks = 0
            while (currentCoroutineContext().isActive) {
                val read = line.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val level = pcm16Rms(buffer, read)
                    if (!speechActive) {
                        noiseFloor = (noiseFloor * 0.96) + (level * 0.04)
                    }
                    val threshold = maxOf(420.0, noiseFloor * 3.2)
                    val speechDetected = level >= threshold
                    if (speechDetected) {
                        highEnergyChunks += 1
                    } else {
                        highEnergyChunks = 0
                    }
                    if (!firstChunkLogged) {
                        SovaLogger.event(
                            subsystem = "mic",
                            event = "desktop-capture-first-read",
                            details = mapOf(
                                "bytes" to read.toString(),
                                "rms" to level.toInt().toString(),
                                "threshold" to threshold.toInt().toString(),
                            ),
                        )
                        firstChunkLogged = true
                    }
                    if (!speechActive && speechDetected) {
                        speechActive = true
                        speechChunks = 0
                        silentChunksAfterSpeech = 0
                        stopCurrentPlayback("user-speech-start")
                        emit(SpeechStartMarker)
                        SovaLogger.event(
                            subsystem = "mic",
                            event = "speech-start-detected",
                            details = mapOf(
                                "rms" to level.toInt().toString(),
                                "threshold" to threshold.toInt().toString(),
                                "noiseFloor" to noiseFloor.toInt().toString(),
                            ),
                        )
                    }
                    if (speechActive) {
                        emit(Base64.getEncoder().encodeToString(buffer.copyOf(read)))
                        speechChunks += 1
                        if (speechDetected) {
                            silentChunksAfterSpeech = 0
                        } else {
                            silentChunksAfterSpeech += 1
                        }
                        if (speechChunks == 1 || speechChunks - lastLoggedChunk >= 20) {
                            lastLoggedChunk = speechChunks
                            SovaLogger.event(
                                subsystem = "mic",
                                event = "speech-chunk-emitted",
                                details = mapOf(
                                    "speechChunks" to speechChunks.toString(),
                                    "rms" to level.toInt().toString(),
                                    "threshold" to threshold.toInt().toString(),
                                ),
                            )
                        }
                        val turnEndedBySilence = silentChunksAfterSpeech >= 5 && speechChunks >= 6
                        val turnEndedByMaxDuration = speechChunks >= 220
                        val turnEndedByLongSoftAudio = speechChunks >= 80 && highEnergyChunks == 0 && level < threshold * 0.7
                        if (turnEndedBySilence || turnEndedByMaxDuration || turnEndedByLongSoftAudio) {
                            emit(AudioEndMarker)
                            SovaLogger.event(
                                subsystem = "mic",
                                event = "speech-end-detected",
                                details = mapOf(
                                    "speechChunks" to speechChunks.toString(),
                                    "silentChunks" to silentChunksAfterSpeech.toString(),
                                    "rms" to level.toInt().toString(),
                                    "reason" to when {
                                        turnEndedByMaxDuration -> "max-duration"
                                        turnEndedByLongSoftAudio -> "long-soft-audio"
                                        else -> "silence"
                                    },
                                ),
                            )
                            speechActive = false
                            speechChunks = 0
                            silentChunksAfterSpeech = 0
                            lastLoggedChunk = 0
                            highEnergyChunks = 0
                        }
                    }
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

        val played = when {
            normalizedFormat == "wav" -> playWavWithJavaSound(audioBytes)
            isMacOs() -> playWithAfplay(audioBytes, normalizedFormat)
            else -> false
        }

        SovaLogger.event(
            subsystem = "specialist-call",
            event = if (played) "agent-audio-playback-succeeded" else "agent-audio-playback-unsupported",
            details = mapOf("format" to normalizedFormat),
        )
    }

    private fun isMacOs(): Boolean =
        System.getProperty("os.name").lowercase().contains("mac")

    private fun pcm16Rms(bytes: ByteArray, length: Int): Double {
        var sum = 0.0
        var samples = 0
        var index = 0
        while (index + 1 < length) {
            val low = bytes[index].toInt() and 0xff
            val high = bytes[index + 1].toInt()
            val sample = (high shl 8) or low
            sum += sample.toDouble() * sample.toDouble()
            samples += 1
            index += 2
        }
        return if (samples == 0) 0.0 else kotlin.math.sqrt(sum / samples)
    }

    private fun stopCurrentPlayback(reason: String) {
        val process = activePlayback.getAndSet(null)
        val line = activePlaybackLine.getAndSet(null)
        if (process == null && line == null) return
        SovaLogger.event(
            subsystem = "specialist-call",
            event = "agent-audio-playback-interrupted",
            details = mapOf("reason" to reason),
        )
        process?.destroy()
        line?.let {
            runCatching {
                it.stop()
                it.flush()
                it.close()
            }
        }
    }

    private fun playWithAfplay(audioBytes: ByteArray, format: String): Boolean {
        val suffix = when (format) {
            "wav" -> ".wav"
            "mp3" -> ".mp3"
            else -> ".$format"
        }
        val tempFile = File.createTempFile("sova-agent-audio-", suffix)
        return try {
            tempFile.writeBytes(audioBytes)
            val process = ProcessBuilder("afplay", tempFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            activePlayback.set(process)
            val exitCode = process.waitFor()
            activePlayback.compareAndSet(process, null)
            exitCode == 0
        } catch (error: Throwable) {
            SovaLogger.event(
                subsystem = "specialist-call",
                event = "agent-audio-afplay-failed",
                details = mapOf("error" to (error.message ?: error::class.simpleName)),
            )
            false
        } finally {
            activePlayback.getAndSet(null)?.destroy()
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
                activePlaybackLine.set(line)
                line.use {
                    it.start()
                    val buffer = ByteArray(4096)
                    while (it.isOpen) {
                        val read = input.read(buffer, 0, buffer.size)
                        if (read <= 0) break
                        it.write(buffer, 0, read)
                    }
                    if (it.isOpen) {
                        it.drain()
                        it.stop()
                    }
                }
                activePlaybackLine.compareAndSet(line, null)
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
            activePlaybackLine.getAndSet(null)?.close()
            tempFile.delete()
        }
    }
}
