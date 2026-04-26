package org.sova.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
            PatientFocusCard(user, vitals, result, onSpecialistSelected)
        }
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
            PatientFocusCard(
                user = user,
                vitals = vitals,
                result = result,
                onSpecialistSelected = onSpecialistSelected,
            )
        }
        item {
            TrajectoryCard(trajectory = result.trajectory)
        }
    }
}

@Composable
private fun PatientFocusCard(
    user: UserProfile,
    vitals: Vitals,
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
            modifier = Modifier.padding(HealthSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md),
        ) {
            BoxWithConstraints {
                if (maxWidth < HealthSpacing.DesktopBreakpoint / 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                        StatusCopy(user, result)
                        StabilityIndex(score = stabilityScore(result))
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusCopy(user, result, modifier = Modifier.weight(1f))
                        StabilityIndex(score = stabilityScore(result), modifier = Modifier.weight(0.45f))
                    }
                }
            }
            VitalsStrip(vitals)
            RecommendationCard(user, result, onSpecialistSelected)
        }
    }
}

@Composable
private fun StatusCopy(user: UserProfile, result: SimulationResult, modifier: Modifier = Modifier) {
    val title = when (result.riskLevel) {
        RiskLevel.Low -> "${user.firstName} is stable."
        RiskLevel.Moderate -> "${user.firstName} needs a check-in."
        RiskLevel.High -> "${user.firstName} needs help now."
    }
    val body = when (result.riskLevel) {
        RiskLevel.Low -> result.summary
        RiskLevel.Moderate -> result.summary
        RiskLevel.High -> result.summary
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        JournalLabel("Current status")
        Text(title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
        Text(body, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
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

@Composable
private fun VitalsStrip(vitals: Vitals, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
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
        val metrics = currentVitalMetrics(vitals)
        if (metrics.isEmpty()) {
            Text("Waiting for live vitals.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        } else {
            metrics.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                    row.forEach { metric ->
                        VitalSummary(metric.label, metric.value, metric.unit, metric.color, Modifier.weight(1f))
                    }
                    repeat(3 - row.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
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
            body = result.summary,
            background = HealthColors.SurfaceSubtle,
        )
        RiskLevel.Moderate -> RecommendationCopy(
            title = result.recommendation,
            body = result.summary,
            background = HealthColors.AccentSoft,
        )
        RiskLevel.High -> RecommendationCopy(
            title = result.recommendation,
            body = result.summary,
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
        StabilityIndex(score = stabilityScore(result), modifier = Modifier.padding(vertical = HealthSpacing.Xs))
    }
}

private fun stabilityScore(result: SimulationResult): Int {
    val currentRisk = result.trajectory.points.firstOrNull()?.riskScore
    return if (currentRisk == null) {
        when (result.riskLevel) {
            RiskLevel.Low -> 92
            RiskLevel.Moderate -> 52
            RiskLevel.High -> 18
        }
    } else {
        (100 - currentRisk).coerceIn(0, 100)
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
            val metrics = currentVitalMetrics(vitals)
            if (metrics.isEmpty()) {
                Text("Waiting for live vitals.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            } else {
                metrics.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                        row.forEach { metric ->
                            VitalSummary(metric.label, metric.value, metric.unit, metric.color, Modifier.weight(1f))
                        }
                        repeat(3 - row.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            vitals.timestamp?.let {
                Text("Updated $it", color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private data class VitalMetric(
    val label: String,
    val value: String,
    val unit: String,
    val color: androidx.compose.ui.graphics.Color,
)

private fun currentVitalMetrics(vitals: Vitals): List<VitalMetric> =
    listOfNotNull(
        vitals.heartRate?.let { VitalMetric("Heart", it.toString(), "bpm", heartRateColor(it)) },
        vitals.bloodPressure?.let { VitalMetric("Pressure", it, "", bloodPressureColor(it)) },
        vitals.temperature?.let { VitalMetric("Temp", oneDecimal(it), "F", temperatureColor(it)) },
        vitals.spo2?.let { VitalMetric("Oxygen", it.toString(), "%", oxygenColor(it)) },
        vitals.hrv?.let { VitalMetric("HRV", it.toString(), "ms", hrvColor(it)) },
        vitals.sleepHours?.let { VitalMetric("Sleep", oneDecimal(it), "hr", sleepColor(it)) },
    )

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

private fun bloodPressureColor(value: String): androidx.compose.ui.graphics.Color {
    val parts = value.replace(" ", "").split("/")
    val systolic = parts.getOrNull(0)?.toIntOrNull()
    val diastolic = parts.getOrNull(1)?.toIntOrNull()
    return if (systolic == null || diastolic == null) {
        HealthColors.Success
    } else if (systolic >= 140 || diastolic >= 90) {
        HealthColors.Warning
    } else {
        HealthColors.Success
    }
}

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
