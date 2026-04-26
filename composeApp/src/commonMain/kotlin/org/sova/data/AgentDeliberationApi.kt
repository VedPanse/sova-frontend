package org.sova.data

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.sova.logic.ProfileValidation
import org.sova.model.AgentDeliberationDecision
import org.sova.model.AgentDeliberationMessage
import org.sova.model.DeliberationStance
import org.sova.model.MedicalProfile
import org.sova.model.UserProfile
import org.sova.model.Vitals
import kotlin.time.Clock

@Serializable
data class AgentDeliberationStartRequest(
    val patientId: String,
    @SerialName("Age") val age: Int,
    @SerialName("Gender") val gender: String,
    @SerialName("DateOfBirth") val dateOfBirth: String? = null,
    @SerialName("Address") val address: String? = null,
    @SerialName("Surgery") val surgery: String,
    @SerialName("DischargeDate") val dischargeDate: String,
    @SerialName("RiskLevel") val riskLevel: String,
    @SerialName("BloodPressure") val bloodPressure: String? = null,
    @SerialName("HeartRate") val heartRate: Int? = null,
    @SerialName("Allergies") val allergies: String? = null,
    @SerialName("CurrentMedications") val currentMedications: String? = null,
    @SerialName("DoctorPhoneNumber") val doctorPhoneNumber: String? = null,
    @SerialName("EmergencyContactName") val emergencyContactName: String? = null,
    @SerialName("EmergencyContactPhone") val emergencyContactPhone: String? = null,
    val severity: Int? = null,
    val stage: Int? = null,
    val vitals: AgentVitalsPayload? = null,
    @SerialName("anomaly_level") val anomalyLevel: Int? = null,
    val interval: Int? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null,
)

@Serializable
data class AgentVitalsPayload(
    @SerialName("HeartRate") val heartRate: Int? = null,
    @SerialName("BloodPressure") val bloodPressure: String? = null,
    @SerialName("Temperature") val temperature: Double? = null,
    @SerialName("TimeStamp") val timestamp: String? = null,
    @SerialName("SpO2") val spo2: Int? = null,
)

@Serializable
data class AgentDeliberationStartResponse(
    val status: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("stream_url") val streamUrl: String,
    val note: String = "",
)

sealed interface AgentDeliberationEvent {
    data object Started : AgentDeliberationEvent
    data class Message(val value: AgentDeliberationMessage) : AgentDeliberationEvent
    data class Convergence(val value: Double) : AgentDeliberationEvent
    data class Decision(val value: AgentDeliberationDecision) : AgentDeliberationEvent
    data object Done : AgentDeliberationEvent
    data class Error(val message: String) : AgentDeliberationEvent
}

object AgentDeliberationApi {
    private const val BaseUrl = "https://sova-agents.onrender.com"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = sovaHttpClient().config {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun start(request: AgentDeliberationStartRequest): AgentDeliberationStartResponse {
        val response = client.post("$BaseUrl/start-debate/${request.patientId}") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<AgentDeliberationStartResponse>()
        require(response.patientId == request.patientId) {
            "Agent service returned patient ${response.patientId}, expected ${request.patientId}."
        }
        return response
    }

    fun observe(patientId: String): Flow<AgentDeliberationEvent> = flow {
        client.prepareGet("$BaseUrl/stream/$patientId") {
            header(HttpHeaders.Accept, "text/event-stream")
            header(HttpHeaders.CacheControl, "no-cache")
        }.execute { response ->
            val channel = response.bodyAsChannel()
            val dataLines = mutableListOf<String>()

            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                when {
                    line.startsWith("data:") -> dataLines += line.removePrefix("data:").trim()
                    line.isBlank() && dataLines.isNotEmpty() -> {
                        val data = dataLines.joinToString("\n")
                        dataLines.clear()
                        emitPayload(data, patientId)
                    }
                }
            }

            if (dataLines.isNotEmpty()) {
                emitPayload(dataLines.joinToString("\n"), patientId)
            }
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<AgentDeliberationEvent>.emitPayload(
        data: String,
        expectedPatientId: String,
    ) {
        val payload = runCatching {
            json.decodeFromString(StreamPayload.serializer(), data)
        }.getOrElse { cause ->
            emit(AgentDeliberationEvent.Error("Unable to read agent stream event: ${cause.message.orEmpty()}"))
            return
        }
        if (payload.patientId != null && payload.patientId != expectedPatientId) {
            emit(AgentDeliberationEvent.Error("Agent stream patient mismatch. Expected $expectedPatientId but received ${payload.patientId}."))
            return
        }
        when (payload.type) {
            "agent" -> {
                emit(AgentDeliberationEvent.Message(payload.toAgentMessage()))
                emit(AgentDeliberationEvent.Convergence(payload.convergence))
            }
            "decision" -> emit(AgentDeliberationEvent.Decision(payload.toDecision()))
            "done" -> emit(AgentDeliberationEvent.Done)
            "error" -> emit(AgentDeliberationEvent.Error(payload.message ?: "Agent stream failed."))
        }
    }
}

fun UserProfile.toAgentDeliberationStartRequest(
    medical: MedicalProfile,
    vitals: Vitals,
): AgentDeliberationStartRequest {
    val riskLevel = riskLevelFor(vitals)
    val severity = severityFor(riskLevel)
    val dischargeIso = dischargeDate?.let(ProfileValidation::toIsoDate)
    return AgentDeliberationStartRequest(
        patientId = patientId,
        age = ProfileValidation.ageFromDob(dob) ?: 0,
        gender = sex,
        dateOfBirth = ProfileValidation.toIsoDate(dob),
        address = address?.takeIf { it.isNotBlank() },
        surgery = surgery?.takeIf { it.isNotBlank() } ?: "Post-discharge recovery",
        dischargeDate = dischargeIso ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
        riskLevel = riskLevel,
        bloodPressure = vitals.bloodPressure?.takeIf { it.isNotBlank() },
        heartRate = vitals.heartRate,
        allergies = medical.allergies.joinToString(", ").ifBlank { "None" },
        currentMedications = medical.medications.joinToString(", ").ifBlank { "None" },
        doctorPhoneNumber = doctorPhoneNumber?.takeIf { it.isNotBlank() },
        emergencyContactName = emergencyContactName.takeIf { it.isNotBlank() },
        emergencyContactPhone = emergencyContactPhone.takeIf { it.isNotBlank() },
        severity = severity,
        stage = dischargeIso?.let(::stageForDischargeDate),
        vitals = AgentVitalsPayload(
            heartRate = vitals.heartRate,
            bloodPressure = vitals.bloodPressure?.takeIf { it.isNotBlank() },
            temperature = vitals.temperature,
            timestamp = vitals.timestamp?.takeIf { it.isNotBlank() },
            spo2 = vitals.spo2,
        ),
        anomalyLevel = anomalyLevelFor(riskLevel),
        interval = intervalFor(riskLevel),
    )
}

@Serializable
private data class StreamPayload(
    val type: String,
    val event: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    val round: Int = 1,
    val agent: String? = null,
    val specialty: String? = null,
    val statement: String? = null,
    @SerialName("utterance_number") val utteranceNumber: Int = 0,
    val convergence: Double = 0.0,
    val timestamp: String? = null,
    @SerialName("immediate_action") val immediateAction: String? = null,
    val decision: String? = null,
    @SerialName("doctor_report") val doctorReport: String = "",
    @SerialName("urgency_level") val urgencyLevel: String = "medium",
    @SerialName("confidence_score") val confidenceScore: Double = 0.0,
    @SerialName("action_items") val actionItems: List<String> = emptyList(),
    val message: String? = null,
) {
    fun toAgentMessage(): AgentDeliberationMessage {
        val agentName = agent ?: "AI specialist"
        val specialtyName = specialty ?: "Clinical review"
        val text = statement.orEmpty()
        return AgentDeliberationMessage(
            sequence = utteranceNumber,
            agentId = agentId(agentName, specialtyName),
            agentName = agentName,
            specialty = specialtyName,
            initials = initials(agentName, specialtyName),
            message = text,
            stance = inferStance(text),
            createdAt = timestamp.orEmpty(),
        )
    }

    fun toDecision(): AgentDeliberationDecision =
        AgentDeliberationDecision(
            urgencyLevel = urgencyLevel,
            confidence = confidenceScore,
            recommendation = decision ?: immediateAction ?: "",
            actions = actionItems,
            doctorReport = doctorReport,
        )
}

private fun agentId(agentName: String, specialty: String): String {
    val source = specialty.ifBlank { agentName }
    return source.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "agent" }
}

private fun initials(agentName: String, specialty: String): String {
    val key = "$agentName $specialty".lowercase()
    val known = listOf(
        "cardio" to "CD",
        "cardiology" to "CD",
        "pharma" to "PH",
        "pharmacy" to "PH",
        "nutrition" to "NT",
        "diet" to "NT",
        "behavior" to "BH",
        "general" to "GP",
        "primary" to "GP",
        "surgery" to "SG",
        "surgeon" to "SG",
        "critical" to "CC",
    )
    known.firstOrNull { (needle, _) -> needle in key }?.let { return it.second }
    return agentName
        .replace("Dr.", "")
        .split(Regex("[^A-Za-z]+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "AI" }
}

private fun inferStance(statement: String): DeliberationStance {
    val text = statement.lowercase()
    return when {
        listOf("urgent", "concern", "risk", "escalat", "worsen", "danger").any { it in text } -> DeliberationStance.Concern
        listOf("agree", "support", "reasonable", "continue", "stable").any { it in text } -> DeliberationStance.Support
        listOf("recommend", "consensus", "decision", "final").any { it in text } -> DeliberationStance.Decision
        else -> DeliberationStance.Observe
    }
}

private fun riskLevelFor(vitals: Vitals): String {
    val heartRate = vitals.heartRate
    val spo2 = vitals.spo2
    val temperature = vitals.temperature
    return when {
        spo2 != null && spo2 < 92 -> "High"
        heartRate != null && heartRate > 115 -> "High"
        temperature != null && temperature >= 101.0 -> "High"
        spo2 != null && spo2 < 95 -> "Medium"
        heartRate != null && heartRate > 100 -> "Medium"
        temperature != null && temperature >= 100.4 -> "Medium"
        else -> "Low"
    }
}

private fun severityFor(riskLevel: String): Int =
    when (riskLevel) {
        "High" -> 2
        "Medium" -> 1
        else -> 0
    }

private fun anomalyLevelFor(riskLevel: String): Int =
    when (riskLevel) {
        "High" -> 3
        "Medium" -> 2
        else -> 0
    }

private fun intervalFor(riskLevel: String): Int =
    when (riskLevel) {
        "High" -> 30
        "Medium" -> 60
        else -> 300
    }

private fun stageForDischargeDate(value: String): Int? {
    val discharge = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val days = today.toEpochDays() - discharge.toEpochDays()
    return when {
        days <= 0 -> 0
        days <= 3 -> 1
        days <= 7 -> 2
        days <= 14 -> 3
        days <= 28 -> 4
        else -> 5
    }
}
