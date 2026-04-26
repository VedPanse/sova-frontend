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
import org.sova.audio.MicrophoneAccess
import org.sova.audio.MicrophoneAccessState
import org.sova.audio.SpecialistCallAudio
import org.sova.components.AgentDeliberationPanel
import org.sova.components.CareCouncilPanel
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.components.SecondaryButton
import org.sova.data.SpecialistApi
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.Agent
import org.sova.model.AgentDeliberationState
import org.sova.model.MedicalProfile
import org.sova.model.Specialist
import org.sova.model.SpecialistCallEvent
import org.sova.model.SpecialistCallLine
import org.sova.model.SpecialistCallState
import org.sova.model.UserProfile
import org.sova.model.Vitals

@Composable
fun AgentsScreen(
    agents: List<Agent>,
    user: UserProfile,
    medical: MedicalProfile,
    vitals: Vitals,
    deliberationState: AgentDeliberationState,
    specialists: List<Specialist>,
    onRefreshDeliberation: () -> Unit,
    onConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSpecialist by remember { mutableStateOf<Specialist?>(null) }

    activeSpecialist?.let { specialist ->
        SpecialistCallView(
            user = user,
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
                specialists = specialists,
                onRetry = onRefreshDeliberation,
                onSpecialistSelected = { activeSpecialist = it },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AgentsCompact(
                deliberationState = deliberationState,
                specialists = specialists,
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
    specialists: List<Specialist>,
    onRetry: () -> Unit,
    onSpecialistSelected: (Specialist) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            AiCareHeader()
        }
        item { CareCouncilPanel(specialists = specialists, onSpecialistSelected = onSpecialistSelected) }
        item { DeliberationSection(deliberationState, onRetry, onRetry) }
    }
}

@Composable
private fun AgentsWide(
    deliberationState: AgentDeliberationState,
    specialists: List<Specialist>,
    onRetry: () -> Unit,
    onSpecialistSelected: (Specialist) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item { AiCareHeader() }
        item { CareCouncilPanel(specialists = specialists, onSpecialistSelected = onSpecialistSelected) }
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
        AgentDeliberationState.Idle -> DeliberationStandbyCard(modifier)
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
private fun DeliberationStandbyCard(modifier: Modifier = Modifier) {
    JournalCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
            JournalLabel("Live deliberation")
            Text(
                "If your condition deteriorates, Sova will ask AI specialist doctors to debate the safest next step.",
                color = HealthColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "For now, no council is needed.",
                color = HealthColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun SpecialistCallView(
    user: UserProfile,
    specialist: Specialist,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var muted by remember(specialist.id) { mutableStateOf(false) }
    var microphoneAccess by remember(specialist.id) { mutableStateOf<MicrophoneAccessState?>(null) }
    var callState by remember(specialist.id) { mutableStateOf<SpecialistCallState>(SpecialistCallState.Connecting) }
    val coroutineScope = rememberCoroutineScope()
    fun requestMicrophone() {
        coroutineScope.launch {
            microphoneAccess = MicrophoneAccess.request()
            muted = microphoneAccess != MicrophoneAccessState.Granted
        }
    }

    LaunchedEffect(user.patientId, specialist.id) {
        microphoneAccess = MicrophoneAccess.request()
        muted = microphoneAccess != MicrophoneAccessState.Granted
        callState = SpecialistCallState.Connecting
        runCatching {
            val session = SpecialistApi.startCall(user.patientId, specialist.id)
            SpecialistApi.observeCall(
                session = session,
                microphoneChunks = SpecialistCallAudio.microphoneChunks(),
                muted = { muted || microphoneAccess != MicrophoneAccessState.Granted },
            ).collect { event ->
                when (event) {
                    is SpecialistCallEvent.Started -> {
                        callState = SpecialistCallState.Connected()
                    }
                    is SpecialistCallEvent.Caption -> {
                        val existing = (callState as? SpecialistCallState.Connected)?.lines.orEmpty()
                        callState = SpecialistCallState.Connected(lines = existing + event.line)
                    }
                    is SpecialistCallEvent.Audio -> {
                        SpecialistCallAudio.playAgentAudio(event.audioBase64, event.format)
                    }
                    is SpecialistCallEvent.Error -> {
                        val existing = callState as? SpecialistCallState.Connected
                        callState = if (existing != null) {
                            existing.copy(error = event.message)
                        } else {
                            SpecialistCallState.Failed(event.message)
                        }
                    }
                    SpecialistCallEvent.Ended -> callState = SpecialistCallState.Ended
                }
            }
        }.onFailure {
            callState = SpecialistCallState.Failed(it.message ?: "Specialist call is unavailable.")
        }
    }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            CircularBackButton(onClick = onBack)
        }
        item {
            BoxWithConstraints {
                val connected = callState is SpecialistCallState.Connected || callState is SpecialistCallState.Ended
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
                            LiveCaptionCard(callState, Modifier.weight(0.20f))
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
                            LiveCaptionCard(callState)
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
private fun ConnectingCallCard(specialist: Specialist, modifier: Modifier = Modifier) {
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
            Text(specialist.name, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            JournalLabel("Ringing secure line", color = HealthColors.Accent)
            Text("Waiting for the AI care specialist to join.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ConnectedCallCard(
    specialist: Specialist,
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
            Text(specialist.name, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            JournalLabel("AI care specialist connected", color = HealthColors.Success)
            Text("Secure voice check-in in progress.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            MuteToggleButton(
                muted = muted,
                enabled = true,
                onClick = onToggleMute,
            )
            if (microphoneAccess == null || microphoneAccess == MicrophoneAccessState.Denied) {
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
private fun SpecialistAvatar(specialist: Specialist, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(HealthColors.AccentSoft, HealthShapes.Pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(specialist.initials, color = HealthColors.Ink, style = MaterialTheme.typography.titleLarge)
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
private fun LiveCaptionCard(state: SpecialistCallState, modifier: Modifier = Modifier) {
    JournalCard(modifier = modifier) {
        JournalLabel("Live captions")
        when (state) {
            SpecialistCallState.Connecting -> Text("Connecting...", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            SpecialistCallState.Ended -> Text("Call ended.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            is SpecialistCallState.Failed -> Text(state.message, color = HealthColors.Danger, style = MaterialTheme.typography.bodyLarge)
            is SpecialistCallState.Connected -> {
                if (state.lines.isEmpty()) {
                    Text("Listening for the specialist.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                } else {
                    state.lines.forEach { line ->
                        CaptionLine(line)
                    }
                }
                state.error?.let {
                    Text(it, color = HealthColors.Danger, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun CaptionLine(line: SpecialistCallLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (line.fromPatient) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (line.fromPatient) 0.88f else 0.96f)
                .background(
                    if (line.fromPatient) HealthColors.AccentSoft else HealthColors.SurfaceSubtle,
                    HealthShapes.SmallCard,
                )
                .padding(HealthSpacing.Xs),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs / 2),
        ) {
            JournalLabel(line.speaker)
            Text(line.text, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AiCareHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        Text("AI Care", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        JournalLabel("Virtual care team")
        Text("Choose a virtual specialist for a focused check-in.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}
