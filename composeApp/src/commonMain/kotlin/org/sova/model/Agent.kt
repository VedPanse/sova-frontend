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
