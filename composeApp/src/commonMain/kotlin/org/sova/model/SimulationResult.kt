package org.sova.model

data class SimulationResult(
    val riskLevel: RiskLevel,
    val summary: String,
    val reasons: List<String>,
    val recommendation: String,
    val trajectory: Trajectory,
)
