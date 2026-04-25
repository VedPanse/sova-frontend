package org.sova.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.RiskLevel
import org.sova.model.Trajectory

@Composable
fun TrajectoryTimeline(
    trajectory: Trajectory,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        trajectory.points.forEachIndexed { index, point ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(HealthSpacing.Md)
                        .background(riskColor(point.riskLevel), HealthShapes.Pill),
                )
                Text(point.label, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                Text(point.riskLevel.name, color = HealthColors.TextPrimary, style = MaterialTheme.typography.labelMedium)
            }
            if (index < trajectory.points.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .width(HealthSpacing.Md)
                        .height(HealthSpacing.Stroke)
                        .padding(horizontal = HealthSpacing.Xs)
                        .background(HealthColors.Border),
                )
            }
        }
    }
}

private fun riskColor(riskLevel: RiskLevel) = when (riskLevel) {
    RiskLevel.Low -> HealthColors.Success
    RiskLevel.Moderate -> HealthColors.Warning
    RiskLevel.High -> HealthColors.Danger
}
