package org.sova.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
        item { CurrentVitalsCard(vitals) }
        item { LiveInsightsPanel() }
        item { TrajectoryCard() }
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
                    CurrentVitalsCard(vitals)
                    LiveInsightsPanel()
                }
                Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                    TrajectoryCard()
                    JournalCard {
                        JournalLabel("Recommended action")
                        Text(result.recommendation, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("Continue passive monitoring. Escalate only if symptoms change or oxygen trends downward.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
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
        StabilityIndex(score = 94, modifier = Modifier.padding(vertical = HealthSpacing.Xs))
    }
}

@Composable
private fun CurrentVitalsCard(vitals: Vitals, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = HealthColors.Surface,
        shape = HealthShapes.Card,
        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
        shadowElevation = HealthSpacing.None,
    ) {
        Column(
            modifier = Modifier.padding(HealthSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
            ) {
                JournalLabel("Current vitals", modifier = Modifier.weight(1f))
                Text(
                    if (vitals.medicationTaken) "Medication ok" else "Medication missed",
                    color = if (vitals.medicationTaken) HealthColors.Success else HealthColors.Warning,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                VitalSummary("Heart", "${vitals.heartRate}", "bpm", heartRateColor(vitals.heartRate), Modifier.weight(1f))
                VitalSummary("HRV", "${vitals.hrv}", "ms", hrvColor(vitals.hrv), Modifier.weight(1f))
                VitalSummary("Oxygen", "${vitals.spo2}", "%", oxygenColor(vitals.spo2), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                VitalSummary("Sleep", "${vitals.sleepHours}", "hr", sleepColor(vitals.sleepHours), Modifier.weight(1f))
                VitalSummary("Temp", "98.6", "F", HealthColors.Success, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VitalSummary(
    label: String,
    value: String,
    unit: String,
    statusColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(HealthColors.SurfaceSubtle, HealthShapes.SmallCard)
            .padding(HealthSpacing.Xs),
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HealthSpacing.SmallBar)
                .background(statusColor, HealthShapes.Pill),
        )
        JournalLabel(label)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
            Text(value, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text(unit, color = HealthColors.MutedBlue, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun heartRateColor(value: Int) =
    if (value in 50..100) HealthColors.Success else HealthColors.Warning

private fun hrvColor(value: Int) =
    if (value >= 45) HealthColors.Success else HealthColors.Warning

private fun oxygenColor(value: Int) =
    if (value >= 95) HealthColors.Success else HealthColors.Danger

private fun sleepColor(value: Double) =
    if (value >= 6.5) HealthColors.Success else HealthColors.Warning

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
private fun LiveInsightsPanel() {
    JournalCard {
        Text("Live Insights", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        JournalLabel("Patient ID: SOVA-8821 // Real-time neural synthesis")
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HealthSpacing.SmallBar)
                .background(color, HealthShapes.Pill),
        )
    }
}
