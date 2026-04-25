package org.sova.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PatientProfileRequestTest {
    @Test
    fun validationAcceptsCompleteProfile() {
        assertNull(validRequest().validationError())
    }

    @Test
    fun validationRejectsInvalidDatesAndPhones() {
        assertEquals("dateOfBirth must be YYYY-MM-DD.", validRequest(dateOfBirth = "02/14/1988").validationError())
        assertEquals("emergencyContactPhone is invalid.", validRequest(emergencyContactPhone = "123").validationError())
    }

    @Test
    fun mapsApiFieldsToBigQueryColumnsAndLeavesTelemetryNull() {
        val row = validRequest().toBigQueryRow()

        assertEquals("stable-id", row["patientId"])
        assertEquals("1988-02-14", row["DateOfBirth"])
        assertEquals("(415) 555-0100", row["EmergencyContactPhone"])
        assertEquals("Aspirin", row["CurrentMedications"])
        assertTrue(row.containsKey("BloodPressure"))
        assertNull(row["BloodPressure"])
        assertNull(row["HeartRate"])
        assertNull(row["RiskLevel"])
    }

    private fun validRequest(
        dateOfBirth: String = "1988-02-14",
        emergencyContactPhone: String = "(415) 555-0100",
    ) = PatientProfileRequest(
        patientId = "stable-id",
        dateOfBirth = dateOfBirth,
        age = 38,
        gender = "Female",
        address = "1 Apple Park Way",
        surgery = "Knee repair",
        dischargeDate = "2026-04-18",
        doctorPhoneNumber = "",
        emergencyContactName = "Avery",
        emergencyContactPhone = emergencyContactPhone,
        allergies = "None",
        currentMedications = "Aspirin",
    )
}
