package org.sova.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.components.HealthCard
import org.sova.components.InfoRow
import org.sova.components.PrimaryButton
import org.sova.components.SectionHeader
import org.sova.design.HealthColors
import org.sova.design.HealthSpacing
import org.sova.logic.ProfileValidation
import org.sova.model.MedicalProfile
import org.sova.model.SimulationResult
import org.sova.model.UserProfile
import org.sova.model.Vitals

@Composable
fun ShareWithCaregiverScreen(
    user: UserProfile,
    medical: MedicalProfile,
    vitals: Vitals,
    result: SimulationResult,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                Text("Care summary", color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text("A concise update for the care team.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            HealthCard {
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    SectionHeader("Patient")
                    InfoRow("Patient ID", user.patientId)
                    InfoRow("Age", ProfileValidation.ageFromDob(user.dob)?.toString() ?: "Not available")
                    InfoRow("Date of birth", user.dob)
                    InfoRow("Sex", user.sex)
                    InfoRow("Address", user.address?.takeIf { it.isNotBlank() } ?: "Not provided")
                }
            }
        }
        item {
            HealthCard {
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    SectionHeader("Medical details")
                    InfoRow("Current medications", readableList(medical.medications))
                    InfoRow("Allergies", readableList(medical.allergies))
                    InfoRow("Surgery", user.surgery?.takeIf { it.isNotBlank() } ?: "Not provided")
                    InfoRow("Discharge date", user.dischargeDate?.takeIf { it.isNotBlank() } ?: "Not provided")
                }
            }
        }
        item {
            HealthCard {
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    SectionHeader("Current state", result.summary)
                    InfoRow("Heart rate", vitals.heartRate?.let { "$it bpm" } ?: "Not available")
                    vitals.bloodPressure?.let { InfoRow("Blood pressure", it) }
                    vitals.temperature?.let { InfoRow("Temperature", "${oneDecimal(it)} F") }
                    InfoRow("HRV", vitals.hrv?.let { "$it ms" } ?: "Not available")
                    InfoRow("SpO2", vitals.spo2?.let { "$it%" } ?: "Not available")
                    InfoRow("Sleep", vitals.sleepHours?.let { "$it hours" } ?: "Not available")
                    vitals.timestamp?.let { InfoRow("Updated", it) }
                    InfoRow("Risk", result.riskLevel.name)
                    InfoRow("Trajectory", result.trajectory.points.joinToString { "${it.label}: ${it.riskLevel.name}" })
                    InfoRow("Recommendation", result.recommendation)
                }
            }
        }
        item {
            HealthCard {
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    SectionHeader("Care contacts")
                    InfoRow("Emergency", "${user.emergencyContactName}, ${user.emergencyContactPhone}")
                    InfoRow("Care team phone", user.doctorPhoneLabel)
                }
            }
        }
        item { PrimaryButton("Prepare summary", {}) }
    }
}

private fun readableList(values: List<String>): String =
    values.ifEmpty { listOf("None reported") }.joinToString()

private fun oneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    val text = rounded.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}
