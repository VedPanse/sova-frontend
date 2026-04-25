package org.sova.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(message.agentName, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(message.specialty, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
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
            if (decision.actions.isNotEmpty()) {
                Text(decision.actions.joinToString(" • "), color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
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
