package org.sova.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.design.HealthColors
import org.sova.design.HealthSpacing

@Composable
fun ActionCard(
    title: String,
    action: String,
    steps: List<String>,
    modifier: Modifier = Modifier,
    onPrimaryAction: () -> Unit = {},
) {
    HealthCard(modifier = modifier) {
        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
            Text(title, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(action, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            steps.forEach { step ->
                Text(step, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
            PrimaryButton(text = action, onClick = onPrimaryAction)
        }
    }
}
