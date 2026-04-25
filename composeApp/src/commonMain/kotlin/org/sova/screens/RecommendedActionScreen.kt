package org.sova.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.components.ActionCard
import org.sova.components.HealthCard
import org.sova.components.SectionHeader
import org.sova.design.HealthColors
import org.sova.design.HealthSpacing

@Composable
fun RecommendedActionScreen(
    action: String,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                Text("Recommended action", color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text("The clearest next step based on today's signals.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            ActionCard(
                title = "Do this now",
                action = action,
                steps = listOf("Keep monitoring active", "Rest if you feel unwell", "Share the summary if symptoms change"),
            )
        }
        item {
            HealthCard {
                SectionHeader("Escalation", "Call emergency services if symptoms become severe.")
            }
        }
    }
}
