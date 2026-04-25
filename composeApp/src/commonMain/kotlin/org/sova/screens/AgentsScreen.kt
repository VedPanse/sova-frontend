package org.sova.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import org.sova.components.CareCouncilPanel
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.Agent
import kotlinx.coroutines.delay

@Composable
fun AgentsScreen(
    agents: List<Agent>,
    onConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSpecialist by remember { mutableStateOf<String?>(null) }

    activeSpecialist?.let { specialist ->
        SpecialistCallView(
            specialist = specialist,
            onBack = { activeSpecialist = null },
            modifier = modifier,
        )
        return
    }

    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= HealthSpacing.DesktopBreakpoint) {
            AgentsWide(onSpecialistSelected = { activeSpecialist = it }, modifier = Modifier.fillMaxWidth())
        } else {
            AgentsCompact(onSpecialistSelected = { activeSpecialist = it }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AgentsCompact(
    onSpecialistSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            AiCareHeader()
        }
        item { CareCouncilPanel(onSpecialistSelected = onSpecialistSelected) }
    }
}

@Composable
private fun AgentsWide(
    onSpecialistSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item { AiCareHeader() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                    CareCouncilPanel(onSpecialistSelected = onSpecialistSelected)
                    JournalCard {
                        JournalLabel("Care model")
                        Text("Agents are reading live vitals, medication adherence, and recovery markers before proposing a single action.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialistCallView(
    specialist: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var connected by remember(specialist) { mutableStateOf(false) }
    LaunchedEffect(specialist) {
        delay(3000)
        connected = true
    }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            CircularBackButton(onClick = onBack)
        }
        item {
            BoxWithConstraints {
                if (connected) {
                    if (maxWidth >= HealthSpacing.DesktopBreakpoint) {
                        Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md), verticalAlignment = Alignment.Top) {
                            ConnectedCallCard(specialist, Modifier.weight(0.92f))
                            LiveCaptionCard(specialist, Modifier.weight(1.08f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                            ConnectedCallCard(specialist)
                            LiveCaptionCard(specialist)
                        }
                    }
                } else {
                    if (maxWidth >= HealthSpacing.DesktopBreakpoint) {
                        Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md), verticalAlignment = Alignment.Top) {
                            ConnectingCallCard(specialist, Modifier.weight(0.92f))
                            WaitingCaptionCard(Modifier.weight(1.08f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                            ConnectingCallCard(specialist)
                            WaitingCaptionCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(HealthSpacing.Xl)
            .clip(HealthShapes.Pill)
            .background(HealthColors.Surface, HealthShapes.Pill)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(HealthSpacing.Icon)) {
            val stroke = Stroke(width = HealthSpacing.Stroke.toPx() * 2.2f, cap = StrokeCap.Round)
            drawLine(HealthColors.TextPrimary, Offset(size.width * 0.68f, size.height * 0.20f), Offset(size.width * 0.32f, size.height * 0.50f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(HealthColors.TextPrimary, Offset(size.width * 0.32f, size.height * 0.50f), Offset(size.width * 0.68f, size.height * 0.80f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun ConnectingCallCard(specialist: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "doctor-call-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulse-scale",
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulse-alpha",
    )

    JournalCard(modifier = modifier) {
        JournalLabel("Calling")
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
        ) {
            Box(modifier = Modifier.size(HealthSpacing.CouncilCardWidth), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(HealthSpacing.CouncilAvatar + HealthSpacing.Xl)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                            alpha = pulseAlpha
                        }
                        .background(HealthColors.AccentSoft, HealthShapes.Pill),
                )
                DoctorAvatar(specialist, size = HealthSpacing.CouncilAvatar + HealthSpacing.Md)
            }
            Text(specialist, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            JournalLabel("Ringing secure line", color = HealthColors.Accent)
            Text("Waiting for the AI doctor to join.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ConnectedCallCard(specialist: String, modifier: Modifier = Modifier) {
    JournalCard(modifier = modifier) {
        JournalLabel("Connected")
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
        ) {
            DoctorAvatar(specialist, size = HealthSpacing.CouncilAvatar + HealthSpacing.Lg)
            Text(specialist, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            JournalLabel("AI doctor connected", color = HealthColors.Success)
            Text("Secure voice check-in in progress.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DoctorAvatar(specialist: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(HealthColors.AccentSoft, HealthShapes.Pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(specialist.initials(), color = HealthColors.Ink, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun WaitingCaptionCard(modifier: Modifier = Modifier) {
    JournalCard(modifier = modifier) {
        JournalLabel("Live captions")
        Text("Connecting...", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        Text("Captions will appear here when the AI doctor joins.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LiveCaptionCard(specialist: String, modifier: Modifier = Modifier) {
    JournalCard(modifier = modifier) {
        JournalLabel("Live captions")
        liveCaptionsFor(specialist).forEach {
            Text(it, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun String.initials(): String =
    split(" ")
        .filter { it.isNotBlank() }
        .takeLast(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")
        .take(2)

private fun liveCaptionsFor(specialist: String): List<String> =
    when {
        specialist.contains("Cardio") -> listOf(
            "$specialist: I’m reviewing your heart rate and rhythm trends now.",
            "Patient: I don’t feel chest pain. My breathing feels normal.",
            "$specialist: Good. Your current heart rate remains inside the expected recovery range.",
            "$specialist: I recommend continued monitoring and hydration. Escalate if chest discomfort appears.",
        )
        specialist.contains("Pharma") -> listOf(
            "$specialist: I’m checking medication timing and interaction risk.",
            "Patient: I took the scheduled dose this morning.",
            "$specialist: Adherence looks complete. No timing conflict is visible right now.",
            "$specialist: Continue the current schedule unless your clinician changes the plan.",
        )
        specialist.contains("Behavioral") -> listOf(
            "$specialist: I’m checking sleep, stress, and daily barriers.",
            "Patient: Sleep was better, but I still feel tired.",
            "$specialist: That can fit the recovery pattern. Keep activity light and predictable today.",
            "$specialist: I’ll note fatigue without escalation because oxygen and heart signals are stable.",
        )
        else -> listOf(
            "$specialist: I’m here. Tell me what changed since your last check-in.",
            "Patient: I feel steady and took medication on time.",
            "$specialist: Your signals support continued passive monitoring.",
            "$specialist: Recommended action: hydrate, rest, and check back if symptoms change.",
        )
    }

@Composable
private fun AiCareHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        Text("AI Care", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        JournalLabel("Virtual care team")
        Text("Choose a virtual specialist for a focused check-in.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}
