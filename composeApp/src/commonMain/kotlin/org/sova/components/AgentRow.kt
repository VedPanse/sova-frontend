package org.sova.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.sova.design.HealthColors
import org.sova.design.HealthSpacing
import org.sova.model.Agent
import org.sova.model.RiskLevel

@Composable
fun AgentRow(
    agent: Agent,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = HealthSpacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
            Text(agent.name, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text(agent.role, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(agent.insight, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
        }
        StatusPill(text = agent.status, riskLevel = RiskLevel.Low)
    }
}
