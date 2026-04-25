package org.sova.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

actual object MicrophoneAccess {
    actual suspend fun request(): MicrophoneAccessState =
        runCatching {
            openMicrophoneLine()?.use { line ->
                val buffer = ByteArray(line.bufferSize.coerceAtMost(512).coerceAtLeast(64))
                line.start()
                line.read(buffer, 0, buffer.size)
                line.stop()
                return@runCatching MicrophoneAccessState.Granted
            }
            println("Sova microphone: no supported desktop microphone input line was found.")
            MicrophoneAccessState.Unavailable
        }.getOrElse {
            println("Sova microphone: desktop microphone request failed: ${it.message ?: it::class.simpleName}")
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
                    println("Sova microphone: $mixerName supports ${format.sampleRate.toInt()}Hz input but could not open it: ${it.message ?: it::class.simpleName}")
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
