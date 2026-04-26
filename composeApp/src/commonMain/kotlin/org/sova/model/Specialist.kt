package org.sova.model

import kotlinx.serialization.Serializable

@Serializable
data class Specialist(
    val id: String,
    val name: String,
    val specialty: String,
) {
    val initials: String
        get() = name
            .split(" ")
            .filter { it.isNotBlank() }
            .takeLast(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .take(2)
            .ifBlank { "AI" }
}

