package org.sova.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.components.HealthCard
import org.sova.components.SectionHeader
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.AgentMessage

@Composable
fun AgentConversationScreen(
    messages: List<AgentMessage>,
    recommendation: String,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                Text("Conversation", color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text("Short readout from the AI team.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(messages) { message ->
            Column(
                modifier = Modifier
                    .background(HealthColors.Surface, HealthShapes.SmallCard)
                    .padding(HealthSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
            ) {
                Text(message.agentName, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                Text(message.message, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            HealthCard {
                SectionHeader("Recommendation", recommendation)
            }
        }
    }
}
