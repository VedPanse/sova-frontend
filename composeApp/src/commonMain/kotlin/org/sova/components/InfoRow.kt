package org.sova.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.design.HealthColors
import org.sova.design.HealthSpacing

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = HealthColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = HealthColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
