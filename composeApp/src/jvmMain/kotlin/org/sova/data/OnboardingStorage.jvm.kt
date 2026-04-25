package org.sova.data

import java.util.prefs.Preferences

actual object OnboardingStorage {
    private const val ProfileKey = "profile"
    private val preferences: Preferences = Preferences.userRoot().node("org/sova/onboarding")

    actual fun read(): String? =
        preferences.get(ProfileKey, null)

    actual fun readBackup(): String? = null

    actual fun write(value: String) {
        preferences.put(ProfileKey, value)
    }

    actual fun clear() {
        preferences.remove(ProfileKey)
    }
}
