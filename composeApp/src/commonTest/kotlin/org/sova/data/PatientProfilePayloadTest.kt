package org.sova.data

import org.sova.model.MedicalProfile
import org.sova.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PatientProfilePayloadTest {
    @Test
    fun userProfileMapsToBackendPayloadWithoutVitalsOrRisk() {
        val payload = UserProfile(
            patientId = "stable-id",
            firstName = "Patient",
            lastName = "",
            dob = "02/14/1988",
            sex = "Female",
            address = "1 Apple Park Way",
            heightFeet = 0,
            heightInches = 0,
            weightPounds = 0,
            surgery = "Knee repair",
            dischargeDate = "04/18/2026",
            emergencyContactName = "Avery",
            emergencyContactPhone = "(415) 555-0100",
            doctorPhoneNumber = "(415) 555-0199",
        ).toPatientProfilePayload(
            MedicalProfile(
                conditions = emptyList(),
                medications = listOf("Aspirin"),
                allergies = emptyList(),
            ),
        )

        assertEquals("stable-id", payload.patientId)
        assertEquals("1988-02-14", payload.dateOfBirth)
        assertEquals("2026-04-18", payload.dischargeDate)
        assertEquals("None", payload.allergies)
        assertEquals("Aspirin", payload.currentMedications)
        assertTrue(payload.hasRequiredFields())
    }
}
