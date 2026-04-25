package org.sova.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.components.RecoveryCurve
import org.sova.components.StabilityIndex
import org.sova.components.VitalJournalCard
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.SimulationResult
import org.sova.model.UserProfile
import org.sova.model.Vitals

@Composable
fun DashboardScreen(
    user: UserProfile,
    vitals: Vitals,
    result: SimulationResult,
    onRunSimulation: () -> Unit,
    onRecommendedAction: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= HealthSpacing.DesktopBreakpoint) {
            DashboardWide(user, vitals, result, modifier = Modifier.fillMaxWidth())
        } else {
            DashboardCompact(user, vitals, result, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DashboardCompact(
    user: UserProfile,
    vitals: Vitals,
    result: SimulationResult,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        item {
            StatusHeader(user)
        }
        item {
            HeartRateCard(vitals)
        }
        item { OxygenCard(vitals) }
        item { TemperatureCard() }
        item { TrajectoryCard() }
        item { CareCouncilPanel() }
    }
}

@Composable
private fun DashboardWide(
    user: UserProfile,
    vitals: Vitals,
    result: SimulationResult,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(0.95f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                    StatusHeader(user)
                    Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                        HeartRateCard(vitals, Modifier.weight(1f))
                        OxygenCard(vitals, Modifier.weight(1f))
                    }
                    TemperatureCard()
                }
                Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                    TrajectoryCard()
                    JournalCard {
                        JournalLabel("Recommended action")
                        Text(result.recommendation, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("Continue passive monitoring. Escalate only if symptoms change or oxygen trends downward.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Column(modifier = Modifier.weight(0.95f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                    CareCouncilPanel()
                }
            }
        }
    }
}

@Composable
private fun StatusHeader(user: UserProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        JournalLabel("Current status")
        Text(
            "${user.firstName} shows remarkable recovery stability.",
            color = HealthColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
        )
        StabilityIndex(score = 94, modifier = Modifier.padding(vertical = HealthSpacing.Sm))
    }
}

@Composable
private fun HeartRateCard(vitals: Vitals, modifier: Modifier = Modifier) {
    VitalJournalCard(
        label = "Heart rate",
        value = "${vitals.heartRate}",
        unit = "bpm",
        iconText = "H",
        modifier = modifier,
        bars = true,
    )
}

@Composable
private fun OxygenCard(vitals: Vitals, modifier: Modifier = Modifier) {
    VitalJournalCard(
        label = "SpO2",
        value = "${vitals.spo2}",
        unit = "%",
        iconText = "S",
        modifier = modifier,
        chip = "Optimal",
    )
}

@Composable
private fun TemperatureCard(modifier: Modifier = Modifier) {
    VitalJournalCard(
        label = "Temperature",
        value = "98.6",
        unit = "F",
        iconText = "T",
        modifier = modifier,
    )
}

@Composable
private fun TrajectoryCard(modifier: Modifier = Modifier) {
    JournalCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
            Text("Patient\nTrajectory", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                JournalLabel("Actual", color = HealthColors.Ink)
                JournalLabel("Predicted", color = HealthColors.Success)
            }
        }
        RecoveryCurve()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri").forEach {
                Text(
                    it.uppercase(),
                    color = if (it == "Thu") HealthColors.TextPrimary else HealthColors.MutedBlue,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun CareCouncilPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HealthColors.Surface,
        shape = HealthShapes.Card,
        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HealthColors.AccentSoft)
                    .padding(HealthSpacing.Sm),
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
                Text(
                    text = "Care Intelligence Team",
                    modifier = Modifier.weight(1f),
                    color = HealthColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "AI specialist council",
                    color = HealthColors.Accent,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(HealthSpacing.Sm),
                horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
            ) {
                items(councilAgents) { agent ->
                    CouncilAgentCard(agent)
                }
            }
        }
    }
}

@Composable
private fun CouncilAgentCard(agent: CouncilAgent) {
    Column(
        modifier = Modifier
            .width(HealthSpacing.CouncilCardWidth)
            .background(HealthColors.SurfaceSubtle, HealthShapes.Card)
            .border(HealthSpacing.Stroke, HealthColors.Border, HealthShapes.Card)
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
        Text(agent.name, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = agent.role,
            color = HealthColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Start check-in",
            color = HealthColors.Success,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private data class CouncilAgent(
    val initials: String,
    val name: String,
    val role: String,
    val avatarColor: androidx.compose.ui.graphics.Color,
)

private val councilAgents = listOf(
    CouncilAgent("GP", "Dr. General", "Primary care - synthesizes the whole picture", HealthColors.SurfaceSubtle),
    CouncilAgent("CD", "Dr. Cardio", "Heart rhythm and cardiac risk", HealthColors.AccentSoft),
    CouncilAgent("PH", "Dr. Pharma", "Medication timing and interactions", HealthColors.Success.copy(alpha = 0.35f)),
    CouncilAgent("BH", "Behavioral Health", "Sleep, stress, and daily barriers", HealthColors.MutedBlue.copy(alpha = 0.30f)),
    CouncilAgent("PA", "Patient Advocate", "Preference, comfort, and quality of life", HealthColors.Warning.copy(alpha = 0.35f)),
)
