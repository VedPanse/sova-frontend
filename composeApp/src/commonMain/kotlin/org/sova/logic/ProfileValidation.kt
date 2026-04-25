package org.sova.logic

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

object ProfileValidation {
    fun nameError(value: String, label: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "$label is required."
            trimmed.length < 2 -> "$label should be at least 2 letters."
            else -> null
        }
    }

    fun dobError(value: String): String? {
        val date = parseDate(value) ?: return if (value.isBlank()) {
            "Date of birth is required."
        } else {
            "Use a real date, like 02/14/1988."
        }
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return if (date > today) "Date of birth cannot be in the future." else null
    }

    fun requiredError(value: String, label: String): String? =
        if (value.trim().isEmpty()) "$label is required." else null

    fun numberRangeError(value: String, label: String, min: Int, max: Int): String? {
        val number = value.trim().toIntOrNull()
        return when {
            value.trim().isEmpty() -> "$label is required."
            number == null -> "$label must be a number."
            number !in min..max -> "$label should be between $min and $max."
            else -> null
        }
    }

    fun phoneError(value: String, required: Boolean): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return if (required) "Phone number is required." else null
        val digits = trimmed.count { it.isDigit() }
        return if (digits < 7) "Use a valid phone number." else null
    }

    fun splitList(value: String): List<String> =
        value.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun parseDate(value: String): LocalDate? {
        val trimmed = value.trim()
        val parts = when {
            "/" in trimmed -> trimmed.split("/")
            "-" in trimmed -> trimmed.split("-")
            else -> return null
        }
        if (parts.size != 3) return null

        val first = parts[0].toIntOrNull() ?: return null
        val second = parts[1].toIntOrNull() ?: return null
        val third = parts[2].toIntOrNull() ?: return null

        val (year, month, day) = if (parts[0].length == 4) {
            Triple(first, second, third)
        } else {
            Triple(third, first, second)
        }

        return try {
            LocalDate(year, month, day)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
