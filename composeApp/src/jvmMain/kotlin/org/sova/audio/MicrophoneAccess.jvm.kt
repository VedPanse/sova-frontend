package org.sova.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

actual object MicrophoneAccess {
    actual suspend fun request(): MicrophoneAccessState =
        runCatching {
            val format = AudioFormat(16_000f, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) return@runCatching MicrophoneAccessState.Unavailable
            val line = AudioSystem.getLine(info) as TargetDataLine
            line.open(format)
            line.close()
            MicrophoneAccessState.Granted
        }.getOrElse {
            MicrophoneAccessState.Denied
        }
}
