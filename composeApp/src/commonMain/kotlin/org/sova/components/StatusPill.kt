package org.sova.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.RiskLevel

@Composable
fun StatusPill(
    text: String,
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
) {
    val color = when (riskLevel) {
        RiskLevel.Low -> HealthColors.Success
        RiskLevel.Moderate -> HealthColors.Warning
        RiskLevel.High -> HealthColors.Danger
    }
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), HealthShapes.Pill)
            .padding(PaddingValues(horizontal = HealthSpacing.Sm, vertical = HealthSpacing.Xs)),
    ) {
        Text(text = text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}
