package org.sova.logging

import kotlin.time.Clock

object SovaLogger {
    fun event(
        subsystem: String,
        event: String,
        patientId: String? = null,
        specialistId: String? = null,
        sessionId: String? = null,
        details: Map<String, String?> = emptyMap(),
    ) {
        val parts = buildList {
            add("ts=${Clock.System.now()}")
            add("subsystem=$subsystem")
            add("event=$event")
            patientId?.let { add("patientId=$it") }
            specialistId?.let { add("specialistId=$it") }
            sessionId?.let { add("sessionId=$it") }
            details.forEach { (key, value) ->
                if (!value.isNullOrBlank()) add("$key=$value")
            }
        }
        PlatformLog.write("SovaTrace", parts.joinToString(" "))
    }
}

expect object PlatformLog {
    fun write(tag: String, message: String)
}
