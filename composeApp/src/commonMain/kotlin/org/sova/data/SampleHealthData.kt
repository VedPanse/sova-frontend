package org.sova.data

import org.sova.logic.SimulationEngine
import org.sova.logic.PatientIdGenerator
import org.sova.model.Agent
import org.sova.model.AgentMessage
import org.sova.model.HistoryItem
import org.sova.model.MedicalProfile
import org.sova.model.UserProfile
import org.sova.model.Vitals

object SampleHealthData {
    val user = UserProfile(
        patientId = PatientIdGenerator.newUuid(),
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
        caregiverName = "Anika Shah",
        caregiverContact = "(415) 555-0178",
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

    val history = listOf(
        HistoryItem("Morning check", "Stable signals. No action needed.", "Today, 8:12 AM"),
        HistoryItem("Simulation", "Low risk over the next 6 hours.", "Yesterday, 7:40 PM"),
        HistoryItem("Medication", "Dose confirmed.", "Yesterday, 8:05 AM"),
    )
}
