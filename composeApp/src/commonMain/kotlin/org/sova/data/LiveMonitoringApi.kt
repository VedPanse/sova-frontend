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
    val trajectory: List<LiveMonitoringTrajectoryPointPayload> = emptyList(),
    val notification: LiveMonitoringNotificationPayload? = null,
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
            trajectory = trajectory.toTrajectory(risk, baseScore),
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

@Serializable
data class LiveMonitoringNotificationPayload(
    val type: String,
    val title: String,
    val message: String,
    val requiresResponse: Boolean = false,
    val actions: List<String> = emptyList(),
)

@Serializable
data class LiveMonitoringTrajectoryPointPayload(
    val label: String,
    val hoursFromNow: Double,
    val riskLevel: String,
    val riskScore: Int,
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

private fun List<LiveMonitoringTrajectoryPointPayload>.toTrajectory(
    fallbackRisk: RiskLevel,
    fallbackScore: Int,
): Trajectory {
    if (isNotEmpty()) {
        return Trajectory(
            points = sortedBy { it.hoursFromNow }.map {
                TrajectoryPoint(
                    label = it.label,
                    riskLevel = it.riskLevel.toRiskLevel(),
                    riskScore = it.riskScore.coerceIn(0, 100),
                    hoursFromNow = it.hoursFromNow,
                )
            },
        )
    }

    return Trajectory(
        points = listOf(
            TrajectoryPoint("Now", fallbackRisk, fallbackScore, 0.0),
            TrajectoryPoint("2h", fallbackRisk.projectedRisk(1), (fallbackScore + fallbackRisk.projectionDelta(1)).coerceIn(0, 100), 2.0),
            TrajectoryPoint("6h", fallbackRisk.projectedRisk(2), (fallbackScore + fallbackRisk.projectionDelta(2)).coerceIn(0, 100), 6.0),
        ),
    )
}
