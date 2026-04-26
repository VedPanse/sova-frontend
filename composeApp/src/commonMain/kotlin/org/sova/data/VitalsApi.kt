package org.sova.data

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.sova.model.Vitals

@Serializable
data class LatestVitalsPayload(
    val patientId: String,
    val heartRate: Int? = null,
    val hrv: Int? = null,
    val sleepHours: Double? = null,
    val bloodPressure: String? = null,
    val temperature: Double? = null,
    val timestamp: String? = null,
) {
    fun toVitals(): Vitals =
        Vitals(
            heartRate = heartRate,
            hrv = hrv,
            sleepHours = sleepHours,
            bloodPressure = bloodPressure?.takeIf { it.isNotBlank() },
            temperature = temperature,
            timestamp = timestamp,
        )
}

object VitalsApi {
    private const val BaseUrl = "http://localhost:8080"
    private val client = sovaHttpClient().config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    suspend fun latest(patientId: String): LatestVitalsPayload? {
        val result = runCatching {
            client.get("$BaseUrl/api/vitals/latest/$patientId").body<LatestVitalsPayload>()
        }
        result.exceptionOrNull()?.let { cause ->
            println("Sova vitals: unable to read database values for patientId=$patientId. ${cause.message.orEmpty()}")
        }
        return result.getOrNull()
    }
}
