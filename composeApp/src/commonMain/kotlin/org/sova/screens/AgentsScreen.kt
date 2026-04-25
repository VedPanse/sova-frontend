package org.sova.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.components.PrimaryButton
import org.sova.components.SmallCapsPill
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.Agent

@Composable
fun AgentsScreen(
    agents: List<Agent>,
    onConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= HealthSpacing.DesktopBreakpoint) {
            AgentsWide(onConversation = onConversation, modifier = Modifier.fillMaxWidth())
        } else {
            AgentsCompact(onConversation = onConversation, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AgentsCompact(
    onConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            InsightsHeader()
        }
        item { SensorStreamCard() }
        item { PipelineStatusCard() }
        item { ConsensusCard(onConversation) }
    }
}

@Composable
private fun AgentsWide(
    onConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item { InsightsHeader() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                    SensorStreamCard()
                    PipelineStatusCard()
                    JournalCard {
                        JournalLabel("Care model")
                        Text("Agents are reading live vitals, medication adherence, and recovery markers before proposing a single action.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Column(modifier = Modifier.weight(1.35f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                    ConsensusCard(onConversation)
                }
            }
        }
    }
}

@Composable
private fun InsightsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        Text("Live Insights", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        JournalLabel("Patient ID: SOVA-8821 // Real-time neural synthesis")
    }
}

@Composable
private fun SensorStreamCard() {
    JournalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JournalLabel("Sensor input stream", modifier = Modifier.weight(1f))
            Text("ok", color = HealthColors.Success, style = MaterialTheme.typography.labelMedium)
        }
        SensorLine("Neural latency", "12ms", HealthColors.Accent)
        SensorLine("Oxygen", "98%", HealthColors.Success)
    }
}

@Composable
private fun SensorLine(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HealthColors.SurfaceSubtle)
            .padding(HealthSpacing.Xs),
    ) {
        Row {
            JournalLabel(label, modifier = Modifier.weight(1f), color = color)
            Text(value, color = HealthColors.Ink, style = MaterialTheme.typography.labelMedium)
        }
        Canvas(Modifier.fillMaxWidth().height(HealthSpacing.VitalBar)) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.58f)
                repeat(7) { index ->
                    val x = size.width * (index + 1) / 8f
                    val y = if (index % 2 == 0) size.height * 0.32f else size.height * 0.70f
                    quadraticTo(x - size.width / 16f, y, x, size.height * 0.52f)
                }
            }
            drawPath(path, color, style = Stroke(width = HealthSpacing.Stroke.toPx()))
        }
    }
}

@Composable
private fun PipelineStatusCard() {
    JournalCard {
        JournalLabel("AI pipeline status")
        listOf("Normalization", "Feature Extraction", "Agent Deliberation", "Synthesis Result").forEachIndexed { index, text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, modifier = Modifier.weight(1f), color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                if (index < 2) {
                    Text("ok", color = HealthColors.Success, style = MaterialTheme.typography.labelMedium)
                } else {
                    SmallCapsPill(if (index == 2) "Active" else "Wait", if (index == 2) HealthColors.SurfaceSubtle else HealthColors.MutedBlue)
                }
            }
        }
    }
}

@Composable
private fun ConsensusCard(onConversation: () -> Unit) {
    JournalCard(dark = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Consensus Deliberation", modifier = Modifier.weight(1f), color = HealthColors.Surface, style = MaterialTheme.typography.titleLarge)
            JournalLabel("Protocol v4.2", color = HealthColors.Surface)
        }
        Column(
            modifier = Modifier
                .background(HealthColors.Surface)
                .padding(HealthSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md),
        ) {
            DialogueBubble("CD", "Analyzing respiratory rhythm deviations. Index remains stable; maintain baseline.")
            DialogueBubble("SN", "Observations consistent. Current protocol can continue.")
            JournalLabel("Synthesizing conclusion")
            DialogueBubble("CD", "Confirmed. Generating action plan for review.")
            JournalCard(dark = true) {
                Text("Final Analysis Output", color = HealthColors.Surface, style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic))
                Text("Shift strategy to moderate hydration with baseline preservation. Efficiency index: 94.2%", color = HealthColors.Surface, style = MaterialTheme.typography.bodyLarge)
                PrimaryButton("Approve action plan", onConversation)
            }
        }
    }
}

@Composable
private fun DialogueBubble(initials: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm), verticalAlignment = Alignment.Top) {
        Text(
            initials,
            modifier = Modifier
                .background(HealthColors.Accent, HealthShapes.Pill)
                .padding(HealthSpacing.Sm),
            color = HealthColors.Surface,
            style = MaterialTheme.typography.labelMedium,
        )
        JournalCard {
            JournalLabel("Clinical director agent")
            Text(text, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
