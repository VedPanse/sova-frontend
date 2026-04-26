package org.sova.data

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.sova.model.RiskLevel
import org.sova.model.SimulationResult
import org.sova.model.Trajectory
import org.sova.model.TrajectoryPoint
import org.sova.model.Vitals

@Serializable
data class LiveMonitoringStatusPayload(
    val patientId: String,
    val vitals: LiveMonitoringVitalsPayload = LiveMonitoringVitalsPayload(),
    val anomalyLevel: Int = 0,
    val riskLevel: String = "low",
    val recommendedAction: String = "Continue monitoring",
    val escalation: LiveMonitoringEscalationPayload = LiveMonitoringEscalationPayload(),
    val deliberation: LiveMonitoringDeliberationPayload = LiveMonitoringDeliberationPayload(),
) {
    fun toSimulationResult(): SimulationResult {
        val risk = riskLevel.toRiskLevel()
        val baseScore = when (risk) {
            RiskLevel.Low -> 8 + anomalyLevel.coerceIn(0, 1) * 8
            RiskLevel.Moderate -> 48
            RiskLevel.High -> 78 + (anomalyLevel - 3).coerceAtLeast(0) * 8
        }.coerceIn(0, 100)
        val summary = when (risk) {
            RiskLevel.Low -> "Signals look stable. Sova will keep watching quietly."
            RiskLevel.Moderate -> "Sova needs a little more context before deciding the next step."
            RiskLevel.High -> escalation.reason ?: "Sova is escalating care now."
        }
        val reasons = listOfNotNull(
            "Live anomaly level $anomalyLevel.",
            vitals.timestamp?.takeIf { it.isNotBlank() }?.let { "Latest vitals received at $it." },
            escalation.reason,
        )
        return SimulationResult(
            riskLevel = risk,
            summary = summary,
            reasons = reasons,
            recommendation = recommendedAction,
            trajectory = Trajectory(
                points = listOf(
                    TrajectoryPoint("Now", risk, baseScore),
                    TrajectoryPoint("2h", risk.projectedRisk(1), (baseScore + risk.projectionDelta(1)).coerceIn(0, 100)),
                    TrajectoryPoint("6h", risk.projectedRisk(2), (baseScore + risk.projectionDelta(2)).coerceIn(0, 100)),
                ),
            ),
        )
    }
}

@Serializable
data class LiveMonitoringVitalsPayload(
    val heartRate: Int? = null,
    val hrv: Int? = null,
    val spo2: Int? = null,
    val sleepHours: Double? = null,
    val bloodPressure: String? = null,
    val temperature: Double? = null,
    val timestamp: String? = null,
) {
    fun toVitals(): Vitals =
        Vitals(
            heartRate = heartRate,
            hrv = hrv,
            spo2 = spo2,
            sleepHours = sleepHours,
            bloodPressure = bloodPressure?.takeIf { it.isNotBlank() },
            temperature = temperature,
            timestamp = timestamp?.takeIf { it.isNotBlank() },
        )
}

@Serializable
data class LiveMonitoringEscalationPayload(
    val caregiverCallTriggered: Boolean = false,
    val reason: String? = null,
)

@Serializable
data class LiveMonitoringDeliberationPayload(
    val triggered: Boolean = false,
    val status: String = "idle",
    val streamUrl: String? = null,
)

object LiveMonitoringApi {
    private const val BaseUrl = "https://sova-agents.onrender.com"
    private val client = sovaHttpClient().config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    suspend fun status(patientId: String): LiveMonitoringStatusPayload? {
        val result = runCatching {
            client.get("$BaseUrl/v1/patients/$patientId/status").body<LiveMonitoringStatusPayload>()
        }
        result.exceptionOrNull()?.let { cause ->
            println("Sova live monitoring: unable to read status for patientId=$patientId. ${cause.message.orEmpty()}")
        }
        return result.getOrNull()
    }
}

fun fallbackMonitoringResult(): SimulationResult =
    SimulationResult(
        riskLevel = RiskLevel.Low,
        summary = "Waiting for live monitoring data.",
        reasons = listOf("Sova has not received live vitals yet."),
        recommendation = "Continue monitoring",
        trajectory = Trajectory(
            points = listOf(
                TrajectoryPoint("Now", RiskLevel.Low, 20),
                TrajectoryPoint("2h", RiskLevel.Low, 20),
                TrajectoryPoint("6h", RiskLevel.Low, 20),
            ),
        ),
    )

private fun String.toRiskLevel(): RiskLevel =
    when (lowercase()) {
        "high", "critical" -> RiskLevel.High
        "medium", "moderate" -> RiskLevel.Moderate
        else -> RiskLevel.Low
    }

private fun RiskLevel.projectedRisk(step: Int): RiskLevel =
    when (this) {
        RiskLevel.High -> RiskLevel.High
        RiskLevel.Moderate -> if (step == 2) RiskLevel.High else RiskLevel.Moderate
        RiskLevel.Low -> RiskLevel.Low
    }

private fun RiskLevel.projectionDelta(step: Int): Int =
    when (this) {
        RiskLevel.High -> 6 * step
        RiskLevel.Moderate -> 8 * step
        RiskLevel.Low -> 0
    }
