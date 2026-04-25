package org.sova.logic

import org.sova.model.RiskLevel
import org.sova.model.SimulationResult
import org.sova.model.Trajectory
import org.sova.model.TrajectoryPoint
import org.sova.model.Vitals

object SimulationEngine {
    fun run(vitals: Vitals): SimulationResult {
        val reasons = mutableListOf<String>()
        var score = 0

        if (vitals.medicationTaken == false) {
            score += 2
            reasons += "Medication was missed"
        }
        vitals.hrv?.let {
            if (it < 45) {
                score += 1
                reasons += "Recovery is lower than usual"
            }
        }
        vitals.sleepHours?.let {
            if (it < 6.0) {
                score += 1
                reasons += "Sleep was short"
            }
        }
        vitals.heartRate?.let {
            if (it < 50 || it > 105) {
                score += 2
                reasons += "Heart rate is outside your usual range"
            }
        }
        vitals.spo2?.let {
            if (it < 94) {
                score += 2
                reasons += "Oxygen is lower than expected"
            }
        }

        val risk = when {
            score >= 5 -> RiskLevel.High
            score >= 3 -> RiskLevel.Moderate
            else -> RiskLevel.Low
        }

        val summary = when (risk) {
            RiskLevel.Low -> "You are stable. The next few hours look steady."
            RiskLevel.Moderate -> "A small change is forming. Watch the next 6 hours."
            RiskLevel.High -> "Risk is elevated. Take action now."
        }

        val recommendation = when (risk) {
            RiskLevel.Low -> "Continue monitoring"
            RiskLevel.Moderate -> "Rest and recheck in 2 hours"
            RiskLevel.High -> "Contact your caregiver"
        }

        return SimulationResult(
            riskLevel = risk,
            summary = summary,
            reasons = reasons.ifEmpty { listOf("No concerning signal has been reported yet") },
            recommendation = recommendation,
            trajectory = buildTrajectory(risk, score, vitals),
        )
    }

    private fun buildTrajectory(riskLevel: RiskLevel, score: Int, vitals: Vitals): Trajectory {
        val currentRisk = currentRiskScore(score, vitals)
        val scores = when (riskLevel) {
            RiskLevel.Low -> listOf(
                currentRisk,
                currentRisk + 5,
                currentRisk + 2,
                currentRisk + 6,
                currentRisk + 3,
            ).map { it.coerceIn(8, 34) }
            RiskLevel.Moderate -> listOf(
                currentRisk.coerceAtLeast(36),
                currentRisk + 10,
                currentRisk + 16,
                currentRisk + 12,
                currentRisk + 7,
            ).map { it.coerceIn(36, 67) }
            RiskLevel.High -> listOf(
                currentRisk.coerceAtLeast(68),
                currentRisk + 12,
                currentRisk + 20,
                currentRisk + 15,
                currentRisk + 9,
            ).map { it.coerceIn(68, 95) }
        }
        return Trajectory(
            points = listOf("Now", "1h", "2h", "4h", "6h").mapIndexed { index, label ->
                TrajectoryPoint(
                    label = label,
                    riskLevel = riskLevelForScore(scores[index]),
                    riskScore = scores[index],
                )
            },
        )
    }

    private fun currentRiskScore(score: Int, vitals: Vitals): Int {
        var risk = 14 + score * 10
        vitals.heartRate?.let { risk += ((it - 72) / 8).coerceIn(-2, 5) }
        vitals.spo2?.let { risk += ((97 - it) * 3).coerceIn(-3, 12) }
        vitals.hrv?.let { risk += ((52 - it) / 4).coerceIn(-3, 8) }
        vitals.sleepHours?.let { risk += ((7.0 - it) * 3).toInt().coerceIn(-3, 9) }
        return risk.coerceIn(8, 95)
    }

    private fun riskLevelForScore(score: Int): RiskLevel =
        when {
            score >= 68 -> RiskLevel.High
            score >= 36 -> RiskLevel.Moderate
            else -> RiskLevel.Low
        }
}
