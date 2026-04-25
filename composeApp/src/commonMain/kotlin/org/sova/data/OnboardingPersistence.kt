package org.sova.data

import org.sova.model.MedicalProfile
import org.sova.model.UserProfile

expect object OnboardingStorage {
    fun read(): String?
    fun readBackup(): String?
    fun write(value: String)
    fun clear()
}

object OnboardingPersistence {
    fun load(): PersistedOnboarding? =
        listOfNotNull(OnboardingStorage.read(), OnboardingStorage.readBackup())
            .firstNotNullOfOrNull(::decode)

    fun save(user: UserProfile, medical: MedicalProfile) {
        OnboardingStorage.write(encode(user, medical))
    }

    private fun encode(user: UserProfile, medical: MedicalProfile): String =
        listOf(
            "2",
            user.firstName,
            user.lastName,
            user.dob,
            user.sex,
            user.heightFeet.toString(),
            user.heightInches.toString(),
            user.weightPounds.toString(),
            user.emergencyContactName,
            user.emergencyContactPhone,
            user.doctorName.orEmpty(),
            user.doctorContact.orEmpty(),
            encodeList(medical.conditions),
            encodeList(medical.medications),
            encodeList(medical.allergies),
        ).joinToString("|") { escape(it) }

    private fun decode(value: String): PersistedOnboarding? {
        val parts = splitEscaped(value, '|').map(::unescape)
        val offset = if (parts.firstOrNull() == "2") 1 else 0
        if (parts.size - offset < 14) return null

        return runCatching {
            PersistedOnboarding(
                user = UserProfile(
                    firstName = parts[offset],
                    lastName = parts[offset + 1],
                    dob = parts[offset + 2],
                    sex = parts[offset + 3],
                    heightFeet = parts[offset + 4].toInt(),
                    heightInches = parts[offset + 5].toInt(),
                    weightPounds = parts[offset + 6].toInt(),
                    emergencyContactName = parts[offset + 7],
                    emergencyContactPhone = parts[offset + 8],
                    doctorName = parts[offset + 9].ifBlank { null },
                    doctorContact = parts[offset + 10].ifBlank { null },
                ),
                medical = MedicalProfile(
                    conditions = decodeList(parts[offset + 11]),
                    medications = decodeList(parts[offset + 12]),
                    allergies = decodeList(parts[offset + 13]),
                ),
            )
        }.getOrNull()
    }

    private fun encodeList(values: List<String>): String =
        values.joinToString(",")

    private fun decodeList(value: String): List<String> =
        value.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("|", "\\p")
            .replace(",", "\\c")

    private fun unescape(value: String): String {
        val result = StringBuilder()
        var escaped = false
        value.forEach { char ->
            if (escaped) {
                result.append(
                    when (char) {
                        'p' -> '|'
                        'c' -> ','
                        else -> char
                    },
                )
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else {
                result.append(char)
            }
        }
        if (escaped) result.append('\\')
        return result.toString()
    }

    private fun splitEscaped(value: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        value.forEach { char ->
            when {
                escaped -> {
                    current.append('\\')
                    current.append(char)
                    escaped = false
                }
                char == '\\' -> escaped = true
                char == delimiter -> {
                    parts += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        if (escaped) current.append('\\')
        parts += current.toString()
        return parts
    }
}
