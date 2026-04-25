package org.sova

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.sova.design.HealthTheme
import org.sova.navigation.AppNavigation

@Composable
@Preview
fun App() {
    HealthTheme {
        Box(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            AppNavigation()
        }
    }
}
