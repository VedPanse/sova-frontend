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
)

data class Trajectory(
    val points: List<TrajectoryPoint>,
)
