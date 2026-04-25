package org.sova.data

import java.util.prefs.Preferences

actual object PatientLocalStorage {
    private const val PatientIdKey = "patient_id"
    private const val DraftKey = "profile_draft"
    private val preferences: Preferences = Preferences.userRoot().node("org/sova/patient")

    actual fun readPatientId(): String? =
        preferences.get(PatientIdKey, null)

    actual fun writePatientId(value: String) {
        preferences.put(PatientIdKey, value)
    }

    actual fun readDraft(): String? =
        preferences.get(DraftKey, null)

    actual fun writeDraft(value: String) {
        preferences.put(DraftKey, value)
    }

    actual fun clearDraft() {
        preferences.remove(DraftKey)
    }
}
