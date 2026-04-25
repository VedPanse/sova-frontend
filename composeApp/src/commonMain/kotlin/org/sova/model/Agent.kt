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
    val agentName: String,
    val specialty: String,
    val initials: String,
    val message: String,
    val stance: DeliberationStance,
)

enum class DeliberationStance {
    Observe,
    Concern,
    Support,
    Decision,
}
