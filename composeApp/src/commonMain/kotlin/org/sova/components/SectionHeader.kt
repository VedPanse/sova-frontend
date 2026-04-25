package org.sova.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.sova.design.HealthColors
import org.sova.design.HealthSpacing

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
        Text(title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(subtitle, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
