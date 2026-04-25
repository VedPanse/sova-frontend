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

        if (!vitals.medicationTaken) {
            score += 2
            reasons += "Medication was missed"
        }
        if (vitals.hrv < 45) {
            score += 1
            reasons += "Recovery is lower than usual"
        }
        if (vitals.sleepHours < 6.0) {
            score += 1
            reasons += "Sleep was short"
        }
        if (vitals.heartRate < 50 || vitals.heartRate > 105) {
            score += 2
            reasons += "Heart rate is outside your usual range"
        }
        if (vitals.spo2 < 94) {
            score += 2
            reasons += "Oxygen is lower than expected"
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
            RiskLevel.High -> "Contact your doctor"
        }

        return SimulationResult(
            riskLevel = risk,
            summary = summary,
            reasons = reasons.ifEmpty { listOf("Signals are within your expected range") },
            recommendation = recommendation,
            trajectory = buildTrajectory(risk),
        )
    }

    private fun buildTrajectory(riskLevel: RiskLevel): Trajectory {
        val points = when (riskLevel) {
            RiskLevel.Low -> listOf(RiskLevel.Low, RiskLevel.Low, RiskLevel.Low)
            RiskLevel.Moderate -> listOf(RiskLevel.Low, RiskLevel.Moderate, RiskLevel.Moderate)
            RiskLevel.High -> listOf(RiskLevel.Moderate, RiskLevel.High, RiskLevel.High)
        }
        return Trajectory(
            points = listOf("Now", "2h", "6h").mapIndexed { index, label ->
                TrajectoryPoint(label = label, riskLevel = points[index])
            },
        )
    }
}
