package org.sova.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProfileValidationTest {
    @Test
    fun dateValidationAcceptsPastDatesAndRejectsBadInput() {
        assertNull(ProfileValidation.dobError("02/14/1988"))
        assertEquals("Use a real date, like 02/14/1988.", ProfileValidation.dobError("02/31/1988"))
        assertNotNull(ProfileValidation.dobError("01/01/3000"))
    }

    @Test
    fun dateConversionSupportsDisplayAndIsoFormats() {
        assertEquals("1988-02-14", ProfileValidation.toIsoDate("02/14/1988"))
        assertEquals("02/14/1988", ProfileValidation.toDisplayDate("1988-02-14"))
    }

    @Test
    fun ageIsDerivedFromDateOfBirth() {
        val age = ProfileValidation.ageFromDob("02/14/1988")

        assertNotNull(age)
        assert(age in 37..38)
    }
}
