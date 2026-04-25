package org.sova.data

import platform.Foundation.NSUserDefaults

actual object PatientLocalStorage {
    private const val PatientIdKey = "sova_patient_id"
    private const val DraftKey = "sova_profile_draft"

    actual fun readPatientId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(PatientIdKey)

    actual fun writePatientId(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = PatientIdKey)
    }

    actual fun readDraft(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(DraftKey)

    actual fun writeDraft(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = DraftKey)
    }

    actual fun clearDraft() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(DraftKey)
    }
}
