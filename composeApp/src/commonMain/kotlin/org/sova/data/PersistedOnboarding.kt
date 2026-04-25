package org.sova.data

import org.sova.model.MedicalProfile
import org.sova.model.UserProfile

data class PersistedOnboarding(
    val user: UserProfile,
    val medical: MedicalProfile,
)
