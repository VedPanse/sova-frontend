package org.sova.audio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val MicrophonePermissionRequestCode = 4207

object AndroidMicrophoneAccess {
    var activity: ComponentActivity? = null
    private var pending: ((MicrophoneAccessState) -> Unit)? = null

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != MicrophonePermissionRequestCode) return false
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        pending?.invoke(if (granted) MicrophoneAccessState.Granted else MicrophoneAccessState.Denied)
        pending = null
        return true
    }

    suspend fun request(): MicrophoneAccessState {
        val currentActivity = activity ?: return MicrophoneAccessState.Unavailable
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return MicrophoneAccessState.Granted
        if (currentActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return MicrophoneAccessState.Granted
        }

        return suspendCancellableCoroutine { continuation ->
            pending = { state ->
                if (continuation.isActive) continuation.resume(state)
            }
            continuation.invokeOnCancellation { pending = null }
            currentActivity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MicrophonePermissionRequestCode)
        }
    }
}

actual object MicrophoneAccess {
    actual suspend fun request(): MicrophoneAccessState =
        AndroidMicrophoneAccess.request()
}
