package org.sova

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sova.audio.AndroidMicrophoneAccess
import org.sova.data.AndroidOnboardingStorageContext
import org.sova.notifications.AndroidNotificationContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidOnboardingStorageContext.appContext = applicationContext
        AndroidMicrophoneAccess.activity = this
        AndroidNotificationContext.appContext = applicationContext
        requestNotificationPermissionIfNeeded()

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        if (AndroidMicrophoneAccess.activity === this) {
            AndroidMicrophoneAccess.activity = null
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (!AndroidMicrophoneAccess.onRequestPermissionsResult(requestCode, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 3002
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
