package org.sova.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.AgentDeliberationDecision
import org.sova.model.AgentDeliberationMessage
import org.sova.model.DeliberationStance

@Composable
fun AgentDeliberationPanel(
    messages: List<AgentDeliberationMessage>,
    modifier: Modifier = Modifier,
    statusText: String = "Live",
    activeAgent: String? = null,
    convergence: Double = 0.0,
    decision: AgentDeliberationDecision? = null,
    errorText: String? = null,
    onRetry: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
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
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                    JournalLabel("Live deliberation")
                    Text("AI specialists are debating the safest next step.", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = HealthColors.AccentSoft,
                        shape = HealthShapes.Pill,
                        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = HealthSpacing.Sm, vertical = HealthSpacing.Xs),
                            color = HealthColors.Accent,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    if (onRefresh != null) {
                        RefreshIconButton(onClick = onRefresh)
                    }
                }
            }

            if (convergence > 0.0) {
                Text(
                    text = "Consensus ${(convergence * 100).toInt().coerceIn(0, 100)}%",
                    color = HealthColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            errorText?.let {
                Text(it, color = HealthColors.Danger, style = MaterialTheme.typography.bodyLarge)
                if (onRetry != null) {
                    SecondaryButton("Retry", onClick = onRetry)
                }
            }

            if (messages.isEmpty() && errorText == null) {
                Text("Waiting for the first specialist response...", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }

            messages.forEachIndexed { index, message ->
                DeliberationBubble(
                    message = message,
                    alignEnd = message.stance == DeliberationStance.Decision,
                    showConnector = index < messages.lastIndex,
                )
            }

            activeAgent?.let {
                TypingIndicator(it)
            }

            decision?.let {
                DecisionSummary(it)
            }
        }
    }
}

@Composable
private fun RefreshIconButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(HealthSpacing.Xl)
            .clip(HealthShapes.Pill)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
        color = HealthColors.SurfaceSubtle,
        shape = HealthShapes.Pill,
        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
    ) {
        Canvas(modifier = Modifier.padding(HealthSpacing.Xs)) {
            val strokeWidth = HealthSpacing.Stroke.toPx() * 2.3f
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            drawArc(
                color = HealthColors.Accent,
                startAngle = 130f,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = Offset(size.width * 0.20f, size.height * 0.20f),
                size = Size(size.width * 0.60f, size.height * 0.60f),
                style = stroke,
            )
            drawLine(
                color = HealthColors.Accent,
                start = Offset(size.width * 0.73f, size.height * 0.19f),
                end = Offset(size.width * 0.80f, size.height * 0.39f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = HealthColors.Accent,
                start = Offset(size.width * 0.73f, size.height * 0.19f),
                end = Offset(size.width * 0.52f, size.height * 0.21f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DeliberationBubble(
    message: AgentDeliberationMessage,
    alignEnd: Boolean,
    showConnector: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            if (!alignEnd) {
                AgentAvatar(message.initials, stanceColor(message.stance))
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (alignEnd) 0.86f else 0.92f)
                    .padding(horizontal = HealthSpacing.Xs),
                color = if (alignEnd) HealthColors.AccentSoft else HealthColors.SurfaceSubtle,
                shape = HealthShapes.SmallCard,
                border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
                shadowElevation = HealthSpacing.None,
            ) {
                Column(
                    modifier = Modifier.padding(HealthSpacing.Sm),
                    verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs / 2)) {
                            Text(message.agentName, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                message.specialty,
                                color = HealthColors.TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(message.message, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (alignEnd) {
                AgentAvatar(message.initials, stanceColor(message.stance))
            }
        }
        if (showConnector) {
            Box(
                modifier = Modifier
                    .padding(start = HealthSpacing.Md + HealthSpacing.Xs)
                    .size(width = HealthSpacing.Stroke, height = HealthSpacing.Sm)
                    .background(HealthColors.Border),
            )
        }
    }
}

@Composable
private fun TypingIndicator(agentName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        AgentAvatar("AI", HealthColors.AccentSoft)
        Text(
            text = "$agentName is reviewing...",
            color = HealthColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        BouncingDots()
    }
}

@Composable
private fun BouncingDots() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val transition = rememberInfiniteTransition(label = "deliberation-dot-$index")
            val offset by transition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, delayMillis = index * 120),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "deliberation-dot-offset-$index",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { translationY = offset }
                    .background(HealthColors.Accent, HealthShapes.Pill),
            )
        }
    }
}

@Composable
private fun DecisionSummary(decision: AgentDeliberationDecision) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HealthColors.AccentSoft,
        shape = HealthShapes.SmallCard,
        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(HealthSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
        ) {
            JournalLabel("Final recommendation")
            Text(decision.recommendation, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AgentAvatar(initials: String, color: Color) {
    Box(
        modifier = Modifier
            .size(HealthSpacing.Xl)
            .background(color, HealthShapes.Pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = HealthColors.Ink, style = MaterialTheme.typography.labelMedium)
    }
}

private fun stanceColor(stance: DeliberationStance): Color =
    when (stance) {
        DeliberationStance.Observe -> HealthColors.AccentSoft
        DeliberationStance.Concern -> HealthColors.Warning.copy(alpha = 0.35f)
        DeliberationStance.Support -> HealthColors.Success.copy(alpha = 0.35f)
        DeliberationStance.Decision -> HealthColors.Accent.copy(alpha = 0.28f)
    }
