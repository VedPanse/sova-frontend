package org.sova.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.components.PrimaryButton
import org.sova.components.SecondaryButton
import org.sova.components.SmallCapsPill
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.HistoryItem

@Composable
fun HistoryScreen(
    history: List<HistoryItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                JournalLabel("Patient status: stable")
                Text("Actions &\nRecords", color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text(
                    "A chronological ledger of patient interactions and clinical escalations overseen by the SOVA Care Protocol.",
                    color = HealthColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            JournalCard {
                Text("Care Support", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text("Initiate an immediate encrypted voice session with the SOVA Clinical AI for real-time guidance.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                PrimaryButton("Call AI Care Team", {})
            }
        }
        item {
            JournalCard {
                Text("Data Retrieval", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text("Export clinical transcripts and longitudinal vital summaries for external provider review.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                SecondaryButton("Retrieve Transcripts", {})
            }
        }
        item {
            Text("Care Protocol", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        }
        item { ProtocolStep("Current score", "Level 01", "Active Clinical Observation", "Continuous passive monitoring of respiratory and heart rate variability.", active = true) }
        item { ProtocolStep(null, "Level 02", "Automated Check-In", "AI initiated verbal dialogue to assess cognitive and physical discomfort.") }
        item { ProtocolStep(null, "Level 03", "Caregiver Notification", "Direct escalation to primary emergency contact via encrypted channel.") }
        item {
            Text("Recent Events", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        }
        items(history) { item ->
            JournalCard {
                Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    Column(modifier = Modifier.weight(0.34f)) {
                        JournalLabel(item.time.substringBefore(",").ifBlank { "Today" })
                        JournalLabel(item.time.substringAfter(", ", item.time))
                    }
                    Text(
                        "AI\nReport",
                        modifier = Modifier
                            .background(HealthColors.SurfaceSubtle, HealthShapes.SmallCard)
                            .padding(HealthSpacing.Xs),
                        color = HealthColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                        Text(item.title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(item.summary, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            Text(
                "Load Older Journal Entries",
                color = HealthColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ProtocolStep(
    score: String?,
    level: String,
    title: String,
    body: String,
    active: Boolean = false,
) {
    JournalCard {
        if (score != null) SmallCapsPill(score)
        JournalLabel(level)
        Text(title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        Text(body, color = if (active) HealthColors.TextSecondary else HealthColors.MutedBlue, style = MaterialTheme.typography.bodyLarge)
    }
}
