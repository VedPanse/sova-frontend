package org.sova.data

import org.sova.model.MedicalProfile
import org.sova.model.UserProfile
import org.sova.logic.PatientIdGenerator

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
            "3",
            user.patientId,
            user.firstName,
            user.lastName,
            user.dob,
            user.sex,
            user.address.orEmpty(),
            user.heightFeet.toString(),
            user.heightInches.toString(),
            user.weightPounds.toString(),
            user.surgery.orEmpty(),
            user.dischargeDate.orEmpty(),
            user.emergencyContactName,
            user.emergencyContactPhone,
            user.caregiverName.orEmpty(),
            user.caregiverContact.orEmpty(),
            encodeList(medical.conditions),
            encodeList(medical.medications),
            encodeList(medical.allergies),
        ).joinToString("|") { escape(it) }

    private fun decode(value: String): PersistedOnboarding? {
        val parts = splitEscaped(value, '|').map(::unescape)
        val version = parts.firstOrNull()
        val offset = if (version == "2" || version == "3") 1 else 0
        if (version == "3" && parts.size - offset < 18) return null
        if (version != "3" && parts.size - offset < 14) return null

        return runCatching {
            if (version == "3") {
                PersistedOnboarding(
                    user = UserProfile(
                        patientId = parts[offset],
                        firstName = parts[offset + 1],
                        lastName = parts[offset + 2],
                        dob = parts[offset + 3],
                        sex = parts[offset + 4],
                        address = parts[offset + 5].ifBlank { null },
                        heightFeet = parts[offset + 6].toInt(),
                        heightInches = parts[offset + 7].toInt(),
                        weightPounds = parts[offset + 8].toInt(),
                        surgery = parts[offset + 9].ifBlank { null },
                        dischargeDate = parts[offset + 10].ifBlank { null },
                        emergencyContactName = parts[offset + 11],
                        emergencyContactPhone = parts[offset + 12],
                        caregiverName = parts[offset + 13].ifBlank { null },
                        caregiverContact = parts[offset + 14].ifBlank { null },
                    ),
                    medical = MedicalProfile(
                        conditions = decodeList(parts[offset + 15]),
                        medications = decodeList(parts[offset + 16]),
                        allergies = decodeList(parts[offset + 17]),
                    ),
                )
            } else {
                PersistedOnboarding(
                    user = UserProfile(
                        patientId = PatientIdGenerator.newUuid(),
                        firstName = parts[offset],
                        lastName = parts[offset + 1],
                        dob = parts[offset + 2],
                        sex = parts[offset + 3],
                        address = null,
                        heightFeet = parts[offset + 4].toInt(),
                        heightInches = parts[offset + 5].toInt(),
                        weightPounds = parts[offset + 6].toInt(),
                        surgery = null,
                        dischargeDate = null,
                        emergencyContactName = parts[offset + 7],
                        emergencyContactPhone = parts[offset + 8],
                        caregiverName = parts[offset + 9].ifBlank { null },
                        caregiverContact = parts[offset + 10].ifBlank { null },
                    ),
                    medical = MedicalProfile(
                        conditions = decodeList(parts[offset + 11]),
                        medications = decodeList(parts[offset + 12]),
                        allergies = decodeList(parts[offset + 13]),
                    ),
                )
            }
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
