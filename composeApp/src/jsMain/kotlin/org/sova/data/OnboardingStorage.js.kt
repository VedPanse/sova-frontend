package org.sova.data

import kotlinx.browser.window

actual object OnboardingStorage {
    private const val ProfileKey = "sova_onboarding_profile"

    actual fun read(): String? =
        runCatching { window.localStorage.getItem(ProfileKey) }.getOrNull()

    actual fun readBackup(): String? =
        runCatching { window.sessionStorage.getItem(ProfileKey) }.getOrNull()

    actual fun write(value: String) {
        runCatching { window.localStorage.setItem(ProfileKey, value) }
        runCatching { window.sessionStorage.setItem(ProfileKey, value) }
    }

    actual fun clear() {
        runCatching { window.localStorage.removeItem(ProfileKey) }
        runCatching { window.sessionStorage.removeItem(ProfileKey) }
    }
}
