package org.sova.data

import kotlinx.serialization.Serializable
import org.sova.logic.ProfileValidation
import org.sova.model.MedicalProfile
import org.sova.model.UserProfile

@Serializable
data class PatientProfilePayload(
    val patientId: String,
    val dateOfBirth: String,
    val age: Int,
    val gender: String,
    val address: String = "",
    val surgery: String,
    val dischargeDate: String,
    val doctorPhoneNumber: String = "",
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val allergies: String = "None",
    val currentMedications: String = "None",
) {
    fun toUserProfile(): UserProfile =
        UserProfile(
            patientId = patientId,
            firstName = "Patient",
            lastName = "",
            dob = ProfileValidation.toDisplayDate(dateOfBirth),
            sex = gender,
            address = address.ifBlank { null },
            heightFeet = 5,
            heightInches = 8,
            weightPounds = 160,
            surgery = surgery,
            dischargeDate = ProfileValidation.toDisplayDate(dischargeDate),
            emergencyContactName = emergencyContactName,
            emergencyContactPhone = emergencyContactPhone,
            doctorPhoneNumber = doctorPhoneNumber.ifBlank { null },
        )

    fun toMedicalProfile(): MedicalProfile =
        MedicalProfile(
            conditions = emptyList(),
            medications = csvToList(currentMedications),
            allergies = csvToList(allergies),
        )

    fun hasRequiredFields(): Boolean =
        dateOfBirth.isNotBlank() &&
            age > 0 &&
            gender.isNotBlank() &&
            surgery.isNotBlank() &&
            dischargeDate.isNotBlank() &&
            emergencyContactName.isNotBlank() &&
            emergencyContactPhone.isNotBlank()
}

fun UserProfile.toPatientProfilePayload(medical: MedicalProfile): PatientProfilePayload =
    PatientProfilePayload(
        patientId = patientId,
        dateOfBirth = ProfileValidation.toIsoDate(dob) ?: dob,
        age = ProfileValidation.ageFromDob(dob) ?: 0,
        gender = sex,
        address = address.orEmpty(),
        surgery = surgery.orEmpty(),
        dischargeDate = dischargeDate?.let(ProfileValidation::toIsoDate).orEmpty(),
        doctorPhoneNumber = doctorPhoneNumber.orEmpty(),
        emergencyContactName = emergencyContactName,
        emergencyContactPhone = emergencyContactPhone,
        allergies = medical.allergies.joinToString(", ").ifBlank { "None" },
        currentMedications = medical.medications.joinToString(", ").ifBlank { "None" },
    )

private fun csvToList(value: String): List<String> =
    value.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.equals("none", ignoreCase = true) }
