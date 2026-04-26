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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.sova.model.Specialist
import org.sova.model.SpecialistCallEvent
import org.sova.model.SpecialistCallLine
import org.sova.model.SpecialistCallSession
import kotlin.random.Random

@Serializable
private data class SpecialistCallStartRequest(
    val specialistId: String,
    val clientSessionId: String,
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

    suspend fun startCall(patientId: String, specialistId: String): SpecialistCallSession {
        val response = client.post("$HttpBaseUrl/v1/patients/$patientId/specialist-calls") {
            contentType(ContentType.Application.Json)
            setBody(
                SpecialistCallStartRequest(
                    specialistId = specialistId,
                    clientSessionId = "kmp-${Random.nextLong().toString(16)}",
                ),
            )
        }.body<SpecialistCallStartResponse>()
        return SpecialistCallSession(response.sessionId, response.websocketUrl)
    }

    fun observeCall(
        session: SpecialistCallSession,
        microphoneChunks: Flow<String>,
        muted: () -> Boolean,
    ): Flow<SpecialistCallEvent> = flow {
        client.webSocket("$WsBaseUrl${session.websocketUrl}") {
            coroutineScope {
                launch {
                    var chunksSent = 0
                    microphoneChunks.collect { chunk ->
                        if (!muted()) {
                            send(buildJsonObject {
                                put("type", "audio.chunk")
                                put("audioBase64", chunk)
                                put("format", "pcm16")
                            }.toString())
                            chunksSent += 1
                            if (chunksSent >= 12) {
                                send(buildJsonObject {
                                    put("type", "audio.end")
                                    put("format", "pcm16")
                                }.toString())
                                chunksSent = 0
                            }
                        }
                    }
                }
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val decoded = runCatching {
                        json.decodeFromString(SpecialistStreamPayload.serializer(), frame.readText())
                    }
                    val payload = decoded.getOrNull()
                    if (payload == null) {
                        emit(SpecialistCallEvent.Error("Unable to read specialist call event."))
                        continue
                    }
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
                        "session.error" -> emit(SpecialistCallEvent.Error(payload.message ?: "Specialist call failed."))
                        "session.ended" -> emit(SpecialistCallEvent.Ended)
                    }
                }
            }
        }
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
