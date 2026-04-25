package org.sova.model

data class Vitals(
    val heartRate: Int,
    val hrv: Int,
    val spo2: Int,
    val sleepHours: Double,
    val medicationTaken: Boolean,
)
