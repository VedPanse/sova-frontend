package org.sova.data

import android.content.Context

object AndroidOnboardingStorageContext {
    lateinit var appContext: Context

    fun isInitialized(): Boolean =
        ::appContext.isInitialized
}

actual object OnboardingStorage {
    private const val PreferencesName = "sova_onboarding"
    private const val ProfileKey = "profile"

    actual fun read(): String? =
        if (AndroidOnboardingStorageContext.isInitialized()) {
            prefs().getString(ProfileKey, null)
        } else {
            null
        }

    actual fun readBackup(): String? = null

    actual fun write(value: String) {
        if (AndroidOnboardingStorageContext.isInitialized()) {
            prefs().edit().putString(ProfileKey, value).apply()
        }
    }

    actual fun clear() {
        if (AndroidOnboardingStorageContext.isInitialized()) {
            prefs().edit().remove(ProfileKey).apply()
        }
    }

    private fun prefs() =
        AndroidOnboardingStorageContext.appContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}
