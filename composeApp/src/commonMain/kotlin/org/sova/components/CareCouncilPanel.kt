package org.sova.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing

@Composable
fun CareCouncilPanel(
    modifier: Modifier = Modifier,
    onSpecialistSelected: (String) -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = HealthColors.Surface,
        shape = HealthShapes.Card,
        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
    ) {
        Column {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HealthColors.AccentSoft)
                    .padding(HealthSpacing.Sm),
            ) {
                val compact = maxWidth < 440.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(HealthSpacing.Xl)
                            .background(HealthColors.Accent, HealthShapes.Pill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("5", color = HealthColors.Surface, style = MaterialTheme.typography.titleLarge)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs / 2),
                    ) {
                        Text(
                            text = "Care Intelligence Team",
                            color = HealthColors.Ink,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = if (compact) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (compact) {
                            Text(
                                text = "AI specialist council",
                                color = HealthColors.Accent,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!compact) {
                        Text(
                            text = "AI specialist council",
                            color = HealthColors.Accent,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(HealthSpacing.Sm),
                horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
            ) {
                items(councilAgents) { agent ->
                    CouncilAgentCard(agent, onClick = { onSpecialistSelected(agent.name) })
                }
            }
        }
    }
}

@Composable
private fun CouncilAgentCard(agent: CouncilAgent, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(HealthSpacing.CouncilCardWidth)
            .height(HealthSpacing.CouncilCardHeight)
            .clip(HealthShapes.Card)
            .background(HealthColors.SurfaceSubtle, HealthShapes.Card)
            .border(HealthSpacing.Stroke, HealthColors.Border, HealthShapes.Card)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(HealthSpacing.Sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        Box(
            modifier = Modifier
                .size(HealthSpacing.CouncilAvatar)
                .background(agent.avatarColor, HealthShapes.Pill),
            contentAlignment = Alignment.Center,
        ) {
            Text(agent.initials, color = HealthColors.Ink, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = agent.name,
            color = HealthColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = agent.role,
            modifier = Modifier.weight(1f),
            color = HealthColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Start check-in",
            color = HealthColors.Success,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private data class CouncilAgent(
    val initials: String,
    val name: String,
    val role: String,
    val avatarColor: Color,
)

private val councilAgents = listOf(
    CouncilAgent("GP", "General Care", "Primary care - synthesizes the whole picture", HealthColors.SurfaceSubtle),
    CouncilAgent("CD", "Cardio Care", "Heart rhythm and cardiac risk", HealthColors.AccentSoft),
    CouncilAgent("PH", "Pharma Care", "Medication timing and interactions", HealthColors.Success.copy(alpha = 0.35f)),
    CouncilAgent("BH", "Behavioral Health", "Sleep, stress, and daily barriers", HealthColors.MutedBlue.copy(alpha = 0.30f)),
    CouncilAgent("PA", "Patient Advocate", "Preference, comfort, and quality of life", HealthColors.Warning.copy(alpha = 0.35f)),
)
