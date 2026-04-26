package org.sova.model

enum class RiskLevel {
    Low,
    Moderate,
    High,
}

data class TrajectoryPoint(
    val label: String,
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val hoursFromNow: Double? = null,
)

data class Trajectory(
    val points: List<TrajectoryPoint>,
)
