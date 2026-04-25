package org.sova.model

data class MedicalProfile(
    val conditions: List<String>,
    val medications: List<String>,
    val allergies: List<String>,
)
