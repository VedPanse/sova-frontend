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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.sova.audio.MicrophoneAccess
import org.sova.audio.MicrophoneAccessState
import org.sova.components.AgentDeliberationPanel
import org.sova.components.CareCouncilPanel
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.components.SecondaryButton
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.Agent
import org.sova.model.AgentDeliberationState
import org.sova.model.MedicalProfile
import org.sova.model.UserProfile
import org.sova.model.Vitals

@Composable
fun AgentsScreen(
    agents: List<Agent>,
    user: UserProfile,
    medical: MedicalProfile,
    vitals: Vitals,
    deliberationState: AgentDeliberationState,
    onRefreshDeliberation: () -> Unit,
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
            AgentsWide(
                deliberationState = deliberationState,
                onRetry = onRefreshDeliberation,
                onSpecialistSelected = { activeSpecialist = it },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AgentsCompact(
                deliberationState = deliberationState,
                onRetry = onRefreshDeliberation,
                onSpecialistSelected = { activeSpecialist = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AgentsCompact(
    deliberationState: AgentDeliberationState,
    onRetry: () -> Unit,
    onSpecialistSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            AiCareHeader()
        }
        item { CareCouncilPanel(onSpecialistSelected = onSpecialistSelected) }
        item { DeliberationSection(deliberationState, onRetry, onRetry) }
    }
}

@Composable
private fun AgentsWide(
    deliberationState: AgentDeliberationState,
    onRetry: () -> Unit,
    onSpecialistSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item { AiCareHeader() }
        item { CareCouncilPanel(onSpecialistSelected = onSpecialistSelected) }
        item { DeliberationSection(state = deliberationState, onRetry = onRetry, onRefresh = onRetry) }
    }
}

@Composable
private fun DeliberationSection(
    state: AgentDeliberationState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        AgentDeliberationState.Idle,
        AgentDeliberationState.Starting -> AgentDeliberationPanel(
            messages = emptyList(),
            modifier = modifier,
            statusText = "Starting",
            activeAgent = "Sova council",
        )
        is AgentDeliberationState.Streaming -> AgentDeliberationPanel(
            messages = state.messages,
            modifier = modifier,
            statusText = "Live",
            activeAgent = state.activeAgent ?: "Sova council",
            convergence = state.convergence,
        )
        is AgentDeliberationState.Completed -> AgentDeliberationPanel(
            messages = state.messages,
            modifier = modifier,
            statusText = "Complete",
            decision = state.decision,
            onRefresh = onRefresh,
        )
        is AgentDeliberationState.Failed -> AgentDeliberationPanel(
            messages = emptyList(),
            modifier = modifier,
            statusText = "Unavailable",
            errorText = state.message,
            onRetry = if (state.canRetry) onRetry else null,
        )
    }
}

@Composable
fun SpecialistCallView(
    specialist: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var connected by remember(specialist) { mutableStateOf(false) }
    var muted by remember(specialist) { mutableStateOf(false) }
    var microphoneAccess by remember(specialist) { mutableStateOf<MicrophoneAccessState?>(null) }
    val coroutineScope = rememberCoroutineScope()
    fun requestMicrophone() {
        coroutineScope.launch {
            microphoneAccess = MicrophoneAccess.request()
            muted = microphoneAccess != MicrophoneAccessState.Granted
        }
    }

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
                            ConnectedCallCard(
                                specialist = specialist,
                                muted = muted,
                                microphoneAccess = microphoneAccess,
                                onToggleMute = {
                                    if (microphoneAccess == MicrophoneAccessState.Granted) {
                                        muted = !muted
                                    } else {
                                        requestMicrophone()
                                    }
                                },
                                onRequestMicrophone = ::requestMicrophone,
                                modifier = Modifier.weight(0.80f),
                            )
                            LiveCaptionCard(specialist, Modifier.weight(0.20f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                            ConnectedCallCard(
                                specialist = specialist,
                                muted = muted,
                                microphoneAccess = microphoneAccess,
                                onToggleMute = {
                                    if (microphoneAccess == MicrophoneAccessState.Granted) {
                                        muted = !muted
                                    } else {
                                        requestMicrophone()
                                    }
                                },
                                onRequestMicrophone = ::requestMicrophone,
                            )
                            LiveCaptionCard(specialist)
                        }
                    }
                } else {
                    if (maxWidth >= HealthSpacing.DesktopBreakpoint) {
                        Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Md), verticalAlignment = Alignment.Top) {
                            ConnectingCallCard(specialist, Modifier.weight(0.80f))
                            WaitingCaptionCard(Modifier.weight(0.20f))
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
    val transition = rememberInfiniteTransition(label = "specialist-call-pulse")
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
                SpecialistAvatar(specialist, size = HealthSpacing.CouncilAvatar + HealthSpacing.Md)
            }
            Text(specialist, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            JournalLabel("Ringing secure line", color = HealthColors.Accent)
            Text("Waiting for the AI care specialist to join.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ConnectedCallCard(
    specialist: String,
    muted: Boolean,
    microphoneAccess: MicrophoneAccessState?,
    onToggleMute: () -> Unit,
    onRequestMicrophone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JournalCard(modifier = modifier) {
        JournalLabel("Connected")
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
        ) {
            SpecialistAvatar(specialist, size = HealthSpacing.CouncilAvatar + HealthSpacing.Lg)
            Text(specialist, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            JournalLabel("AI care specialist connected", color = HealthColors.Success)
            Text("Secure voice check-in in progress.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            MuteToggleButton(
                muted = muted,
                enabled = true,
                onClick = onToggleMute,
            )
            if (microphoneAccess != MicrophoneAccessState.Granted) {
                SecondaryButton(
                    text = if (microphoneAccess == MicrophoneAccessState.Denied) "Try microphone again" else "Enable microphone",
                    onClick = onRequestMicrophone,
                )
            }
            val microphoneLabel = when (microphoneAccess) {
                MicrophoneAccessState.Granted -> if (muted) "Microphone muted" else "Microphone on"
                MicrophoneAccessState.Denied -> "Microphone permission needed"
                MicrophoneAccessState.Unavailable -> "Microphone unavailable"
                null -> "Microphone off"
            }
            JournalLabel(
                microphoneLabel,
                color = if (microphoneAccess == MicrophoneAccessState.Granted && !muted) HealthColors.Accent else HealthColors.Danger,
            )
        }
    }
}

@Composable
private fun MuteToggleButton(muted: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val background = when {
        !enabled -> HealthColors.SurfaceSubtle
        muted -> HealthColors.Danger
        else -> HealthColors.SurfaceSubtle
    }
    val iconColor = when {
        !enabled -> HealthColors.TextSecondary
        muted -> HealthColors.Surface
        else -> HealthColors.TextPrimary
    }

    Box(
        modifier = Modifier
            .size(HealthSpacing.Xl + HealthSpacing.Sm)
            .clip(HealthShapes.Pill)
            .background(background, HealthShapes.Pill)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(HealthSpacing.Lg)) {
            val strokeWidth = HealthSpacing.Stroke.toPx() * 2.1f
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val micWidth = size.width * 0.28f
            val micHeight = size.height * 0.46f
            val micLeft = (size.width - micWidth) / 2f
            val micTop = size.height * 0.14f

            drawRoundRect(
                color = iconColor,
                topLeft = Offset(micLeft, micTop),
                size = Size(micWidth, micHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(micWidth / 2f, micWidth / 2f),
                style = stroke,
            )
            drawArc(
                color = iconColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(size.width * 0.25f, size.height * 0.36f),
                size = Size(size.width * 0.50f, size.height * 0.34f),
                style = stroke,
            )
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.50f, size.height * 0.70f),
                end = Offset(size.width * 0.50f, size.height * 0.84f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.36f, size.height * 0.84f),
                end = Offset(size.width * 0.64f, size.height * 0.84f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            if (muted) {
                drawLine(
                    color = iconColor,
                    start = Offset(size.width * 0.22f, size.height * 0.20f),
                    end = Offset(size.width * 0.78f, size.height * 0.82f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun SpecialistAvatar(specialist: String, size: androidx.compose.ui.unit.Dp) {
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
        Text("Captions will appear here when the AI care specialist joins.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
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
        specialist.contains("Caregiver Outreach") -> listOf(
            "Sova: I’m preparing a concise caregiver handoff with current vitals and risk context.",
            "Sova: Oxygen is low and medication was missed. This needs human follow-up.",
            "Sova: Calling the saved caregiver contact now.",
            "Sova: I’ll keep the summary focused on what changed and what action is needed.",
        )
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
