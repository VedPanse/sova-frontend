package org.sova.model

data class Vitals(
    val heartRate: Int? = null,
    val hrv: Int? = null,
    val spo2: Int? = null,
    val sleepHours: Double? = null,
    val medicationTaken: Boolean? = null,
    val bloodPressure: String? = null,
    val temperature: Double? = null,
    val timestamp: String? = null,
)
