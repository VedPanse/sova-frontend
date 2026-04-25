package org.sova.data

import kotlinx.browser.window

actual object PatientLocalStorage {
    private const val PatientIdKey = "sova_patient_id"
    private const val DraftKey = "sova_profile_draft"

    actual fun readPatientId(): String? =
        runCatching { window.localStorage.getItem(PatientIdKey) }.getOrNull()

    actual fun writePatientId(value: String) {
        runCatching { window.localStorage.setItem(PatientIdKey, value) }
    }

    actual fun readDraft(): String? =
        runCatching { window.localStorage.getItem(DraftKey) }.getOrNull()

    actual fun writeDraft(value: String) {
        runCatching { window.localStorage.setItem(DraftKey, value) }
    }

    actual fun clearDraft() {
        runCatching { window.localStorage.removeItem(DraftKey) }
    }
}
