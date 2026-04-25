package org.sova.data

import org.sova.model.Agent
import org.sova.model.AgentMessage
import org.sova.model.HistoryItem

object DisplayContent {
    val agents = listOf(
        Agent("Vitals", "Reads live signals", "Done", "Heart rate and oxygen are steady."),
        Agent("Recovery", "Checks strain", "Done", "Sleep and HRV support normal recovery."),
        Agent("Medication", "Reviews routine", "Done", "Morning dose is confirmed."),
        Agent("Care", "Chooses next step", "Ready", "Continue monitoring today."),
    )

    val conversation = listOf(
        AgentMessage("Vitals", "Signals are steady right now."),
        AgentMessage("Recovery", "No recovery drop is visible."),
        AgentMessage("Medication", "Medication routine is complete."),
        AgentMessage("Care", "Recommendation: continue monitoring."),
    )

    val history = listOf(
        HistoryItem("Morning check", "Stable signals. No action needed.", "Today, 8:12 AM"),
        HistoryItem("Simulation", "Low risk over the next 6 hours.", "Yesterday, 7:40 PM"),
        HistoryItem("Medication", "Dose confirmed.", "Yesterday, 8:05 AM"),
    )
}
