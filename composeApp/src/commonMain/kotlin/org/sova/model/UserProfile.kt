package org.sova.model

data class UserProfile(
    val patientId: String,
    val firstName: String,
    val lastName: String,
    val dob: String,
    val sex: String,
    val address: String?,
    val heightFeet: Int,
    val heightInches: Int,
    val weightPounds: Int,
    val surgery: String?,
    val dischargeDate: String?,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val doctorPhoneNumber: String?,
) {
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Patient" }

    val heightLabel: String
        get() = "$heightFeet ft $heightInches in"

    val weightLabel: String
        get() = "$weightPounds lb"

    val doctorPhoneLabel: String
        get() = doctorPhoneNumber?.takeIf { it.isNotBlank() } ?: "Not provided"
}
