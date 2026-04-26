package org.sova.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import org.sova.logging.SovaLogger

actual object MicrophoneAccess {
    actual suspend fun request(): MicrophoneAccessState =
        runCatching {
            SovaLogger.event(subsystem = "mic", event = "desktop-line-probe-start")
            openMicrophoneLine()?.use { line ->
                SovaLogger.event(
                    subsystem = "mic",
                    event = "desktop-line-probe-success",
                    details = mapOf("format" to line.format.toString()),
                )
                return@runCatching MicrophoneAccessState.Granted
            }
            SovaLogger.event(subsystem = "mic", event = "desktop-line-probe-unavailable")
            MicrophoneAccessState.Unavailable
        }.getOrElse {
            SovaLogger.event(
                subsystem = "mic",
                event = "desktop-line-probe-failed",
                details = mapOf("error" to (it.message ?: it::class.simpleName)),
            )
            MicrophoneAccessState.Denied
        }
}

private fun openMicrophoneLine(): TargetDataLine? {
    val formats = listOf(
        AudioFormat(44_100f, 16, 1, true, false),
        AudioFormat(48_000f, 16, 1, true, false),
        AudioFormat(16_000f, 16, 1, true, false),
        AudioFormat(44_100f, 16, 2, true, false),
    )
    val mixers = listOf(null) + AudioSystem.getMixerInfo().map { AudioSystem.getMixer(it) }
    if (mixers.size <= 1) {
        println("Sova microphone: no desktop audio mixers reported by the JVM audio system.")
    }

    mixers.forEach { mixer ->
        formats.forEach { format ->
            val info = DataLine.Info(TargetDataLine::class.java, format)
            val supported = if (mixer == null) AudioSystem.isLineSupported(info) else mixer.isLineSupported(info)
            if (supported) {
                runCatching {
                    val line = (mixer?.getLine(info) ?: AudioSystem.getLine(info)) as TargetDataLine
                    line.open(format)
                    return line
                }.onFailure {
                    val mixerName = mixer?.mixerInfo?.name ?: "default mixer"
                    SovaLogger.event(
                        subsystem = "mic",
                        event = "desktop-line-open-failed",
                        details = mapOf(
                            "mixer" to mixerName,
                            "sampleRate" to format.sampleRate.toInt().toString(),
                            "error" to (it.message ?: it::class.simpleName),
                        ),
                    )
                }
            }
        }
    }

    return null
}

private inline fun TargetDataLine.use(block: (TargetDataLine) -> Unit) {
    try {
        block(this)
    } finally {
        close()
    }
}
