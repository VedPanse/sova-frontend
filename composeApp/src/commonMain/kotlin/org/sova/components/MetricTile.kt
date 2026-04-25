package org.sova.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing

@Composable
fun MetricTile(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = HealthShapes.SmallCard,
        colors = CardDefaults.cardColors(containerColor = HealthColors.SurfaceSubtle),
        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = HealthSpacing.None),
    ) {
        Column(
            modifier = Modifier.padding(HealthSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
        ) {
            Text(label, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(value, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text(detail, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}
