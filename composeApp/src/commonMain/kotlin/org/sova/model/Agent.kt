package org.sova.model

data class Agent(
    val name: String,
    val role: String,
    val status: String,
    val insight: String,
)

data class AgentMessage(
    val agentName: String,
    val message: String,
)

data class AgentDeliberationMessage(
    val sequence: Int = 0,
    val agentId: String = "",
    val agentName: String,
    val specialty: String,
    val initials: String,
    val message: String,
    val stance: DeliberationStance,
    val createdAt: String = "",
)

enum class DeliberationStance {
    Observe,
    Concern,
    Support,
    Decision,
}

data class AgentDeliberationDecision(
    val urgencyLevel: String,
    val confidence: Double,
    val recommendation: String,
    val actions: List<String>,
    val doctorReport: String,
)

sealed interface AgentDeliberationState {
    data object Idle : AgentDeliberationState
    data object Starting : AgentDeliberationState
    data class Streaming(
        val messages: List<AgentDeliberationMessage>,
        val convergence: Double = 0.0,
        val activeAgent: String? = null,
    ) : AgentDeliberationState
    data class Completed(
        val messages: List<AgentDeliberationMessage>,
        val decision: AgentDeliberationDecision?,
    ) : AgentDeliberationState
    data class Failed(
        val message: String,
        val canRetry: Boolean = true,
    ) : AgentDeliberationState
}
