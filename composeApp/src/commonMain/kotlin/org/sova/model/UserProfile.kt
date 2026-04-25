package org.sova.model

data class UserProfile(
    val firstName: String,
    val lastName: String,
    val dob: String,
    val sex: String,
    val heightFeet: Int,
    val heightInches: Int,
    val weightPounds: Int,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val doctorName: String?,
    val doctorContact: String?,
) {
    val fullName: String
        get() = "$firstName $lastName"

    val heightLabel: String
        get() = "$heightFeet ft $heightInches in"

    val weightLabel: String
        get() = "$weightPounds lb"

    val doctorLabel: String
        get() = listOfNotNull(doctorName, doctorContact).joinToString(" - ").ifBlank { "Not provided" }
}
