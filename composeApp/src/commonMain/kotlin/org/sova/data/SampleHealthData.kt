package org.sova.data

import org.sova.logic.SimulationEngine
import org.sova.model.Agent
import org.sova.model.AgentDeliberationMessage
import org.sova.model.AgentMessage
import org.sova.model.DeliberationStance
import org.sova.model.HistoryItem
import org.sova.model.MedicalProfile
import org.sova.model.UserProfile
import org.sova.model.Vitals

object SampleHealthData {
    val user = UserProfile(
        patientId = "f7e4c2b8-12d9-4f3f-8d26-0a8c5f2d8f31",
        firstName = "Maya",
        lastName = "Shah",
        dob = "02/14/1988",
        sex = "Female",
        address = "22 Valencia Street, San Francisco, CA",
        heightFeet = 5,
        heightInches = 6,
        weightPounds = 138,
        surgery = "Appendectomy",
        dischargeDate = "04/18/2026",
        emergencyContactName = "Arun Shah",
        emergencyContactPhone = "(415) 555-0134",
        doctorPhoneNumber = "(415) 555-0178",
    )

    val medical = MedicalProfile(
        conditions = listOf("Asthma", "Mild hypertension"),
        medications = listOf("Lisinopril", "Albuterol"),
        allergies = listOf("Penicillin"),
    )

    val vitals = Vitals(
        heartRate = 108,
        hrv = 41,
        spo2 = 92,
        sleepHours = 7.1,
        medicationTaken = false,
    )

    val simulation = SimulationEngine.run(vitals)

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

    val deliberation = listOf(
        AgentDeliberationMessage(
            agentName = "Dr. Cardio",
            specialty = "Cardiology",
            initials = "CD",
            message = "Heart rate is elevated, but the pattern is consistent with post-discharge recovery. I do not see an emergency rhythm signal.",
            stance = DeliberationStance.Observe,
        ),
        AgentDeliberationMessage(
            agentName = "Dr. Pharma",
            specialty = "Medication",
            initials = "PH",
            message = "Medication was missed this morning. That increases near-term risk and could explain some instability.",
            stance = DeliberationStance.Concern,
        ),
        AgentDeliberationMessage(
            agentName = "Nutritionist",
            specialty = "Recovery nutrition",
            initials = "NT",
            message = "Hydration and a light meal would be a reasonable first step before escalating, unless symptoms worsen.",
            stance = DeliberationStance.Support,
        ),
        AgentDeliberationMessage(
            agentName = "Behavioral Health",
            specialty = "Sleep and stress",
            initials = "BH",
            message = "Sleep is adequate. I do not think fatigue alone explains the risk change today.",
            stance = DeliberationStance.Observe,
        ),
        AgentDeliberationMessage(
            agentName = "Sova Lead",
            specialty = "Decision",
            initials = "SV",
            message = "Consensus: ask for a short AI care check-in, confirm medication timing, and keep caregiver escalation ready if oxygen trends down.",
            stance = DeliberationStance.Decision,
        ),
    )

    val history = listOf(
        HistoryItem("Morning check", "Stable signals. No action needed.", "Today, 8:12 AM"),
        HistoryItem("Simulation", "Low risk over the next 6 hours.", "Yesterday, 7:40 PM"),
        HistoryItem("Medication", "Dose confirmed.", "Yesterday, 8:05 AM"),
    )
}
