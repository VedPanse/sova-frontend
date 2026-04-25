package org.sova.data

import android.content.Context

object AndroidOnboardingStorageContext {
    lateinit var appContext: Context

    fun isInitialized(): Boolean =
        ::appContext.isInitialized
}

actual object PatientLocalStorage {
    private const val PreferencesName = "sova_patient"
    private const val PatientIdKey = "patient_id"
    private const val DraftKey = "profile_draft"

    actual fun readPatientId(): String? =
        if (AndroidOnboardingStorageContext.isInitialized()) prefs().getString(PatientIdKey, null) else null

    actual fun writePatientId(value: String) {
        if (AndroidOnboardingStorageContext.isInitialized()) prefs().edit().putString(PatientIdKey, value).apply()
    }

    actual fun readDraft(): String? =
        if (AndroidOnboardingStorageContext.isInitialized()) prefs().getString(DraftKey, null) else null

    actual fun writeDraft(value: String) {
        if (AndroidOnboardingStorageContext.isInitialized()) prefs().edit().putString(DraftKey, value).apply()
    }

    actual fun clearDraft() {
        if (AndroidOnboardingStorageContext.isInitialized()) prefs().edit().remove(DraftKey).apply()
    }

    private fun prefs() =
        AndroidOnboardingStorageContext.appContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}
