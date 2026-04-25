package org.sova.data

import platform.Foundation.NSUserDefaults

actual object OnboardingStorage {
    private const val ProfileKey = "sova_onboarding_profile"

    actual fun read(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileKey)

    actual fun readBackup(): String? = null

    actual fun write(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = ProfileKey)
    }

    actual fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileKey)
    }
}
