package org.sova.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.sova.components.CareCouncilPanel
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.components.PatientTrajectoryGraph
import org.sova.components.PrimaryButton
import org.sova.components.SecondaryButton
import org.sova.components.StabilityIndex
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.RiskLevel
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
    var activeSpecialist by remember { mutableStateOf<String?>(null) }

    activeSpecialist?.let { specialist ->
        SpecialistCallView(
            specialist = specialist,
            onBack = { activeSpecialist = null },
            modifier = modifier.fillMaxWidth(),
        )
        return
    }

    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= HealthSpacing.DesktopBreakpoint) {
            DashboardWide(
                user = user,
                vitals = vitals,
                result = result,
                onRunSimulation = onRunSimulation,
                onRecommendedAction = onRecommendedAction,
                onSpecialistSelected = { activeSpecialist = it },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            DashboardCompact(
                user = user,
                vitals = vitals,
                result = result,
                onSpecialistSelected = { activeSpecialist = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DashboardCompact(
    user: UserProfile,
    vitals: Vitals,
    result: SimulationResult,
    onSpecialistSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        item {
            StatusHeader(user, result)
        }
        item { CurrentVitalsCard(vitals) }
        item { RecommendationCard(user, result, onSpecialistSelected) }
        item { LiveInsightsPanel(user.patientId, vitals) }
        item { TrajectoryCard(result.trajectory) }
    }
}

@Composable
private fun DashboardWide(
    user: UserProfile,
    vitals: Vitals,
    result: SimulationResult,
    onRunSimulation: () -> Unit,
    onRecommendedAction: () -> Unit,
    onSpecialistSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md), verticalAlignment = Alignment.Top) {
                DesktopStatusCard(
                    user = user,
                    result = result,
                    onSpecialistSelected = onSpecialistSelected,
                    modifier = Modifier.weight(1.35f),
                )
                TrajectoryCard(
                    trajectory = result.trajectory,
                    modifier = Modifier.weight(0.85f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md),
                verticalAlignment = Alignment.Top,
            ) {
                CurrentVitalsCard(vitals, modifier = Modifier.weight(1.35f).fillMaxHeight())
                LiveInsightsPanel(user.patientId, vitals, modifier = Modifier.weight(0.85f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun DesktopStatusCard(
    user: UserProfile,
    result: SimulationResult,
    onSpecialistSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = HealthColors.Surface,
        shape = HealthShapes.Card,
        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
        shadowElevation = HealthSpacing.None,
    ) {
        Column(
            modifier = Modifier.padding(HealthSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    JournalLabel("Current status")
                    Text(
                        "${user.firstName} is being monitored.",
                        color = HealthColors.TextPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        "Monitoring the next 6 hours.",
                        color = HealthColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                StabilityIndex(score = stabilityScore(result.riskLevel), modifier = Modifier.weight(0.45f))
            }
            RecommendationCard(user, result, onSpecialistSelected)
        }
    }
}

@Composable
private fun RecommendationCard(
    user: UserProfile,
    result: SimulationResult,
    onSpecialistSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recommendation = recommendationCopy(result)
    val uriHandler = LocalUriHandler.current
    val caregiverName = "your caregiver"
    val caregiverContact = user.doctorPhoneNumber.orEmpty().filter { it.isDigit() || it == '+' }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(recommendation.background, HealthShapes.SmallCard)
            .padding(HealthSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
    ) {
        JournalLabel("Recommended action")
        Text(recommendation.title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        Text(
            recommendation.body,
            color = HealthColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        when (result.riskLevel) {
            RiskLevel.Low -> Unit
            RiskLevel.Moderate -> CareCouncilPanel(onSpecialistSelected = onSpecialistSelected)
            RiskLevel.High -> Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                PrimaryButton(
                    text = "Have Sova call $caregiverName",
                    onClick = { onSpecialistSelected("Caregiver Outreach") },
                    modifier = Modifier.weight(1f),
                    enabled = caregiverContact.isNotBlank(),
                )
                SecondaryButton(
                    text = "Call $caregiverName yourself",
                    onClick = {
                        if (caregiverContact.isNotBlank()) {
                            uriHandler.openUri("tel:$caregiverContact")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = caregiverContact.isNotBlank(),
                )
            }
        }
    }
}

private data class RecommendationCopy(
    val title: String,
    val body: String,
    val background: androidx.compose.ui.graphics.Color,
)

private fun recommendationCopy(result: SimulationResult): RecommendationCopy =
    when (result.riskLevel) {
        RiskLevel.Low -> RecommendationCopy(
            title = result.recommendation,
            body = "Continue passive monitoring. Escalate only if symptoms change or oxygen trends downward.",
            background = HealthColors.SurfaceSubtle,
        )
        RiskLevel.Moderate -> RecommendationCopy(
            title = "Talk with an AI care specialist",
            body = "Sova needs a little more context before it can stay passive. A short check-in can clarify symptoms, medication timing, and recovery comfort.",
            background = HealthColors.AccentSoft,
        )
        RiskLevel.High -> RecommendationCopy(
            title = "Call your caregiver",
            body = "Risk is elevated. Contact your caregiver now, especially if symptoms are worsening or oxygen stays low.",
            background = HealthColors.SurfaceSubtle,
        )
    }

@Composable
private fun StatusHeader(user: UserProfile, result: SimulationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        JournalLabel("Current status")
        Text(
            "${user.firstName} is being monitored.",
            color = HealthColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
        )
        StabilityIndex(score = stabilityScore(result.riskLevel), modifier = Modifier.padding(vertical = HealthSpacing.Xs))
    }
}

private fun stabilityScore(riskLevel: RiskLevel): Int =
    when (riskLevel) {
        RiskLevel.Low -> 92
        RiskLevel.Moderate -> 74
        RiskLevel.High -> 48
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
                    when (vitals.medicationTaken) {
                        true -> "Medication ok"
                        false -> "Medication missed"
                        null -> "Medication not reported"
                    },
                    color = when (vitals.medicationTaken) {
                        true -> HealthColors.Success
                        false -> HealthColors.Warning
                        null -> HealthColors.TextSecondary
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                VitalSummary("Heart", vitals.heartRate?.toString() ?: "--", "bpm", heartRateColor(vitals.heartRate), Modifier.weight(1f))
                VitalSummary("HRV", vitals.hrv?.toString() ?: "--", "ms", hrvColor(vitals.hrv), Modifier.weight(1f))
                VitalSummary("Oxygen", vitals.spo2?.toString() ?: "--", "%", oxygenColor(vitals.spo2), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                VitalSummary("Sleep", vitals.sleepHours?.toString() ?: "--", "hr", sleepColor(vitals.sleepHours), Modifier.weight(1f))
                VitalSummary("Temp", vitals.temperature?.let { oneDecimal(it) } ?: "--", "F", temperatureColor(vitals.temperature), Modifier.weight(1f))
            }
            vitals.bloodPressure?.let {
                Text("Blood pressure $it", color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
            vitals.timestamp?.let {
                Text("Updated $it", color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
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

private fun heartRateColor(value: Int?) =
    if (value == null || value in 50..100) HealthColors.Success else HealthColors.Warning

private fun hrvColor(value: Int?) =
    if (value == null || value >= 45) HealthColors.Success else HealthColors.Warning

private fun oxygenColor(value: Int?) =
    if (value == null || value >= 95) HealthColors.Success else HealthColors.Danger

private fun sleepColor(value: Double?) =
    if (value == null || value >= 6.5) HealthColors.Success else HealthColors.Warning

private fun temperatureColor(value: Double?) =
    if (value == null || value in 97.0..99.5) HealthColors.Success else HealthColors.Warning

private fun oneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    val text = rounded.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

@Composable
private fun TrajectoryCard(trajectory: org.sova.model.Trajectory, modifier: Modifier = Modifier) {
    JournalCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                Text("Patient trajectory", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text("Projected risk over the next 6 hours.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        PatientTrajectoryGraph(trajectory)
    }
}

@Composable
private fun LiveInsightsPanel(patientId: String, vitals: Vitals, modifier: Modifier = Modifier) {
    JournalCard(modifier = modifier) {
        Text("Live Insights", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        JournalLabel("Patient ID: $patientId // Real-time synthesis")
        SensorLine("Neural latency", "12ms", HealthColors.Accent)
        SensorLine("Oxygen", vitals.spo2?.let { "$it%" } ?: "Not reported", oxygenColor(vitals.spo2))
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
