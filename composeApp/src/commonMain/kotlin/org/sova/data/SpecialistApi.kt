package org.sova.data

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.sova.logging.SovaLogger
import org.sova.model.MedicalProfile
import org.sova.model.Specialist
import org.sova.model.SpecialistCallEvent
import org.sova.model.SpecialistCallLine
import org.sova.model.SpecialistCallSession
import org.sova.model.UserProfile
import kotlin.random.Random
import kotlin.time.TimeSource

@Serializable
private data class SpecialistCallStartRequest(
    val specialistId: String,
    val clientSessionId: String,
    val patientContext: PatientProfilePayload? = null,
)

@Serializable
private data class SpecialistCallStartResponse(
    val sessionId: String,
    val websocketUrl: String,
)

@Serializable
private data class SpecialistStreamPayload(
    val type: String,
    val sessionId: String? = null,
    val turnId: String? = null,
    val specialistName: String? = null,
    val speaker: String? = null,
    val text: String? = null,
    val message: String? = null,
    val format: String = "mp3",
    val audioBase64: String? = null,
)

object SpecialistApi {
    private const val HttpBaseUrl = "https://sova-agents.onrender.com"
    private const val WsBaseUrl = "wss://sova-agents.onrender.com"
    private const val AudioEndMarker = "__SOVA_AUDIO_END__"
    private const val SpeechStartMarker = "__SOVA_SPEECH_START__"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = sovaHttpClient().config {
        install(ContentNegotiation) { json(json) }
        install(WebSockets)
    }

    suspend fun specialists(): List<Specialist> =
        runCatching {
            client.get("$HttpBaseUrl/v1/specialists").body<List<Specialist>>()
        }.getOrElse {
            println("Sova specialists: unable to read specialists from backend. ${it.message.orEmpty()}")
            fallbackSpecialists()
        }

    suspend fun startCall(
        patientId: String,
        specialistId: String,
        user: UserProfile? = null,
        medical: MedicalProfile? = null,
    ): SpecialistCallSession {
        val mark = TimeSource.Monotonic.markNow()
        val patientContext = if (user != null && medical != null) {
            user.toPatientProfilePayload(medical)
        } else {
            null
        }
        SovaLogger.event(
            subsystem = "specialist-call",
            event = "post-start-request",
            patientId = patientId,
            specialistId = specialistId,
            details = mapOf(
                "url" to "$HttpBaseUrl/v1/patients/$patientId/specialist-calls",
                "hasPatientContext" to (patientContext != null).toString(),
            ),
        )
        val response = client.post("$HttpBaseUrl/v1/patients/$patientId/specialist-calls") {
            contentType(ContentType.Application.Json)
            setBody(
                SpecialistCallStartRequest(
                    specialistId = specialistId,
                    clientSessionId = "kmp-${Random.nextLong().toString(16)}",
                    patientContext = patientContext,
                ),
            )
        }.body<SpecialistCallStartResponse>()
        SovaLogger.event(
            subsystem = "specialist-call",
            event = "post-start-success",
            patientId = patientId,
            specialistId = specialistId,
            sessionId = response.sessionId,
            details = mapOf(
                "durationMs" to mark.elapsedNow().inWholeMilliseconds.toString(),
                "websocketUrl" to response.websocketUrl,
            ),
        )
        return SpecialistCallSession(response.sessionId, response.websocketUrl)
    }

    fun observeCall(
        session: SpecialistCallSession,
        microphoneChunks: Flow<String>,
        muted: () -> Boolean,
    ): Flow<SpecialistCallEvent> = flow {
        val socketUrl = "$WsBaseUrl${session.websocketUrl}"
        SovaLogger.event(
            subsystem = "websocket",
            event = "connect-attempt",
            sessionId = session.sessionId,
            details = mapOf("url" to socketUrl),
        )
        client.webSocket("$WsBaseUrl${session.websocketUrl}") {
            SovaLogger.event(
                subsystem = "websocket",
                event = "socket-open",
                sessionId = session.sessionId,
            )
            coroutineScope {
                val micJob = launch(Dispatchers.Default) {
                    var chunksTotal = 0
                    var chunksInTurn = 0
                    var mutedLogged = false
                    var firstChunkLogged = false
                    SovaLogger.event(
                        subsystem = "mic",
                        event = "chunk-collector-start",
                        sessionId = session.sessionId,
                    )
                    runCatching {
                        microphoneChunks.collect { chunk ->
                            if (!isActive) return@collect
                            if (chunk == SpeechStartMarker) {
                                send(buildJsonObject {
                                    put("type", "audio.speech_start")
                                    put("turnId", "turn-$chunksTotal")
                                }.toString())
                                SovaLogger.event(
                                    subsystem = "websocket",
                                    event = "audio-speech-start-sent",
                                    sessionId = session.sessionId,
                                    details = mapOf("chunksTotal" to chunksTotal.toString()),
                                )
                                return@collect
                            }
                            if (chunk == AudioEndMarker) {
                                if (chunksInTurn > 0) {
                                    send(buildJsonObject {
                                        put("type", "audio.end")
                                        put("format", "pcm16")
                                    }.toString())
                                    SovaLogger.event(
                                        subsystem = "websocket",
                                        event = "audio-end-sent",
                                        sessionId = session.sessionId,
                                        details = mapOf(
                                            "chunksTotal" to chunksTotal.toString(),
                                            "chunksInTurn" to chunksInTurn.toString(),
                                        ),
                                    )
                                    chunksInTurn = 0
                                } else {
                                    SovaLogger.event(
                                        subsystem = "websocket",
                                        event = "audio-end-skipped-empty-turn",
                                        sessionId = session.sessionId,
                                    )
                                }
                                return@collect
                            }
                            if (muted()) {
                                if (!mutedLogged) {
                                    SovaLogger.event(
                                        subsystem = "mic",
                                        event = "audio-send-muted",
                                        sessionId = session.sessionId,
                                    )
                                    mutedLogged = true
                                }
                                return@collect
                            }
                            mutedLogged = false
                            if (!firstChunkLogged) {
                                SovaLogger.event(
                                    subsystem = "mic",
                                    event = "first-chunk-ready",
                                    sessionId = session.sessionId,
                                    details = mapOf("bytesBase64" to chunk.length.toString()),
                                )
                                firstChunkLogged = true
                            }
                            send(buildJsonObject {
                                put("type", "audio.chunk")
                                put("audioBase64", chunk)
                                put("format", "pcm16")
                            }.toString())
                            chunksTotal += 1
                            chunksInTurn += 1
                            if (chunksTotal == 1 || chunksTotal % 25 == 0) {
                                SovaLogger.event(
                                    subsystem = "websocket",
                                    event = "audio-chunk-sent",
                                    sessionId = session.sessionId,
                                    details = mapOf(
                                        "chunksTotal" to chunksTotal.toString(),
                                        "chunksInTurn" to chunksInTurn.toString(),
                                    ),
                                )
                            }
                        }
                    }.onFailure {
                        SovaLogger.event(
                            subsystem = "mic",
                            event = "chunk-collector-failed",
                            sessionId = session.sessionId,
                            details = mapOf("error" to (it.message ?: it::class.simpleName)),
                        )
                    }
                    SovaLogger.event(
                        subsystem = "mic",
                        event = "chunk-collector-finished",
                        sessionId = session.sessionId,
                    )
                }
                try {
                    var firstFrameLogged = false
                    val audioChunkBuffers = mutableMapOf<String, StringBuilder>()
                    val audioChunkFormats = mutableMapOf<String, String>()
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()
                        if (!firstFrameLogged) {
                            SovaLogger.event(
                                subsystem = "websocket",
                                event = "first-frame-received",
                                sessionId = session.sessionId,
                                details = mapOf("chars" to text.length.toString()),
                            )
                            firstFrameLogged = true
                        }
                        val decoded = runCatching {
                            json.decodeFromString(SpecialistStreamPayload.serializer(), text)
                        }
                        val payload = decoded.getOrNull()
                        if (payload == null) {
                            SovaLogger.event(
                                subsystem = "websocket",
                                event = "frame-decode-failed",
                                sessionId = session.sessionId,
                                details = mapOf("error" to decoded.exceptionOrNull()?.message),
                            )
                            emit(SpecialistCallEvent.Error("Unable to read specialist call event."))
                            continue
                        }
                        SovaLogger.event(
                            subsystem = "websocket",
                            event = "event-received",
                            sessionId = session.sessionId,
                            details = mapOf(
                                "type" to payload.type,
                                "speaker" to payload.speaker,
                                "textChars" to payload.text?.length?.toString(),
                            ),
                        )
                        when (payload.type) {
                            "session.started" -> emit(
                                SpecialistCallEvent.Started(
                                    sessionId = payload.sessionId.orEmpty(),
                                    specialistName = payload.specialistName ?: "AI specialist",
                                ),
                            )
                            "user.transcript.partial" -> Unit
                            "user.transcript.final" -> emit(
                                SpecialistCallEvent.Caption(
                                    SpecialistCallLine(
                                        speaker = payload.speaker ?: "Patient",
                                        text = payload.text.orEmpty(),
                                        fromPatient = true,
                                    ),
                                ),
                            )
                            "agent.transcript" -> emit(
                                SpecialistCallEvent.Caption(
                                    SpecialistCallLine(
                                        speaker = payload.speaker ?: "AI specialist",
                                        text = payload.text.orEmpty(),
                                        fromPatient = false,
                                    ),
                                ),
                            )
                            "agent.audio" -> payload.audioBase64?.let {
                                emit(SpecialistCallEvent.Audio(it, payload.format))
                            }
                            "agent.audio.chunk" -> payload.audioBase64?.let { chunk ->
                                val turnId = payload.turnId ?: payload.sessionId ?: "current"
                                val buffer = audioChunkBuffers.getOrPut(turnId) { StringBuilder() }
                                buffer.append(chunk)
                                audioChunkFormats[turnId] = payload.format
                                SovaLogger.event(
                                    subsystem = "websocket",
                                    event = "agent-audio-chunk-buffered",
                                    sessionId = session.sessionId,
                                    details = mapOf(
                                        "turnId" to turnId,
                                        "chunkChars" to chunk.length.toString(),
                                        "totalChars" to buffer.length.toString(),
                                    ),
                                )
                            }
                            "agent.audio.end" -> {
                                val turnId = payload.turnId ?: payload.sessionId ?: "current"
                                val audio = audioChunkBuffers.remove(turnId)?.toString()
                                val format = audioChunkFormats.remove(turnId) ?: payload.format
                                if (!audio.isNullOrBlank()) {
                                    emit(SpecialistCallEvent.Audio(audio, format))
                                }
                            }
                            "session.error" -> {
                                val rawMessage = payload.message ?: "Specialist call failed."
                                SovaLogger.event(
                                    subsystem = "specialist-call",
                                    event = "backend-session-error",
                                    sessionId = session.sessionId,
                                    details = mapOf(
                                        "rawChars" to rawMessage.length.toString(),
                                        "summary" to rawMessage.redactedProviderErrorSummary(),
                                    ),
                                )
                                emit(SpecialistCallEvent.Error(rawMessage.toUserFacingCallError()))
                            }
                            "session.ended" -> emit(SpecialistCallEvent.Ended)
                        }
                    }
                } finally {
                    runCatching {
                        send(buildJsonObject {
                            put("type", "session.end")
                        }.toString())
                        SovaLogger.event(
                            subsystem = "websocket",
                            event = "session-end-sent",
                            sessionId = session.sessionId,
                        )
                    }.onFailure {
                        SovaLogger.event(
                            subsystem = "websocket",
                            event = "session-end-send-failed",
                            sessionId = session.sessionId,
                            details = mapOf("error" to (it.message ?: it::class.simpleName)),
                        )
                    }
                    SovaLogger.event(
                        subsystem = "websocket",
                        event = "receive-loop-finished",
                        sessionId = session.sessionId,
                    )
                    micJob.cancelAndJoin()
                    SovaLogger.event(
                        subsystem = "mic",
                        event = "chunk-collector-cancelled",
                        sessionId = session.sessionId,
                    )
                }
            }
        }
    }
}

private fun String.toUserFacingCallError(): String {
    val lower = lowercase()
    return when {
        "unable to transcribe" in lower || "detected_unusual_activity" in lower || "401" in lower ->
            "Still listening."
        "eleven" in lower || "transcribe" in lower || "audio" in lower ->
            "One moment."
        "openai" in lower || "llm" in lower ->
            "The specialist is reconnecting."
        else -> "One moment."
    }
}

private fun String.redactedProviderErrorSummary(): String {
    val lower = lowercase()
    return when {
        "detected_unusual_activity" in lower -> "provider_401_detected_unusual_activity"
        "401" in lower -> "provider_401"
        "unable to transcribe" in lower -> "transcription_failed"
        "eleven" in lower -> "voice_provider_error"
        else -> take(120)
    }
}

fun fallbackSpecialists(): List<Specialist> = listOf(
    Specialist("general_physician", "General Physician", "Family Medicine"),
    Specialist("cardiologist", "Cardiologist", "Cardiology"),
    Specialist("critical_care", "Critical Care Specialist", "Critical Care"),
    Specialist("pharmacist", "Clinical Pharmacist", "Clinical Pharmacy"),
    Specialist("pulmonologist", "Pulmonologist", "Pulmonology"),
    Specialist("nutritionist", "Clinical Nutritionist", "Clinical Nutrition"),
)
