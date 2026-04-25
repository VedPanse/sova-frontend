package org.sova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sova.audio.AndroidMicrophoneAccess
import org.sova.data.AndroidOnboardingStorageContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidOnboardingStorageContext.appContext = applicationContext
        AndroidMicrophoneAccess.activity = this

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
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
