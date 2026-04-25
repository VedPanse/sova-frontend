package org.sova.data

import org.sova.logic.ProfileValidation
import org.sova.model.SimulationResult
import org.sova.model.UserProfile

data class BigQueryPatientRecord(
    val patientId: String,
    val age: Int?,
    val caregiverPhoneNumber: String?,
    val riskLevel: String,
    val surgery: String?,
    val dischargeDate: String?,
    val gender: String,
    val address: String?,
) {
    fun toTableRow(): Map<String, Any?> =
        mapOf(
            "PatientId" to patientId,
            "Age" to age,
            "DoctorPhoneNumber" to caregiverPhoneNumber,
            "RiskLevel" to riskLevel,
            "Surgery" to surgery,
            "DischargeDate" to dischargeDate,
            "Gender" to gender,
            "Address" to address,
        )
}

fun UserProfile.toBigQueryPatientRecord(result: SimulationResult): BigQueryPatientRecord =
    BigQueryPatientRecord(
        patientId = patientId,
        age = ProfileValidation.ageFromDob(dob),
        caregiverPhoneNumber = caregiverContact?.takeIf { it.isNotBlank() },
        riskLevel = result.riskLevel.name,
        surgery = surgery?.takeIf { it.isNotBlank() },
        dischargeDate = dischargeDate?.let(ProfileValidation::toIsoDate),
        gender = sex,
        address = address?.takeIf { it.isNotBlank() },
    )
