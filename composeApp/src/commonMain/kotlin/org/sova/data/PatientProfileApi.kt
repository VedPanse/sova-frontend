package org.sova.data

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PatientProfileSyncResponse(
    val success: Boolean,
    val patientId: String,
    val message: String = "",
)

object PatientProfileApi {
    private const val BaseUrl = "http://localhost:8080"
    private val client = sovaHttpClient().config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    suspend fun fetch(patientId: String): PatientProfilePayload? =
        runCatching {
            client.get("$BaseUrl/api/patient-profile/$patientId").body<PatientProfilePayload>()
        }.getOrNull()

    suspend fun sync(payload: PatientProfilePayload): Boolean =
        runCatching {
            client.post("$BaseUrl/api/patient-profile/sync") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.body<PatientProfileSyncResponse>().success
        }.getOrDefault(false)
}
