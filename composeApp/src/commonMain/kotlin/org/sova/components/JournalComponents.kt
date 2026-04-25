package org.sova.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing

@Composable
fun JournalTopBar(
    name: String = "Sova",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HealthColors.Background)
            .padding(horizontal = HealthSpacing.Sm, vertical = HealthSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
    ) {
        Box(
            modifier = Modifier
                .size(HealthSpacing.Lg)
                .background(HealthColors.Ink, HealthShapes.SmallCard),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(HealthSpacing.Icon)) {
                drawCircle(HealthColors.Success, radius = size.minDimension * 0.24f, center = Offset(size.width / 2f, size.height * 0.25f))
                drawRoundRect(
                    color = HealthColors.Blue,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.46f),
                    size = Size(size.width * 0.56f, size.height * 0.38f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
            }
        }
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            color = HealthColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic),
        )
        Text("**", color = HealthColors.Ink, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun JournalLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = HealthColors.MutedBlue,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
fun JournalCard(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (dark) HealthColors.Ink else HealthColors.Surface,
        shape = HealthShapes.Card,
        border = BorderStroke(HealthSpacing.Stroke, if (dark) HealthColors.Ink else HealthColors.Border),
        shadowElevation = HealthSpacing.None,
    ) {
        Column(
            modifier = Modifier.padding(HealthSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
            content = content,
        )
    }
}

@Composable
fun StabilityIndex(
    score: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        JournalLabel("Stability index")
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$score",
                color = HealthColors.TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text("/100", color = HealthColors.MutedBlue, style = MaterialTheme.typography.bodyLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .width(HealthSpacing.Lg)
                        .height(HealthSpacing.SmallBar)
                        .background(if (index < 3) HealthColors.Success else HealthColors.SurfaceSubtle),
                )
            }
        }
    }
}

@Composable
fun VitalJournalCard(
    label: String,
    value: String,
    unit: String,
    iconText: String,
    modifier: Modifier = Modifier,
    chip: String? = null,
    bars: Boolean = false,
) {
    JournalCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                JournalLabel(label)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                    Text(value, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Text(unit, color = HealthColors.MutedBlue, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(iconText, color = HealthColors.Danger, style = MaterialTheme.typography.titleLarge)
        }
        if (bars) {
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs), modifier = Modifier.fillMaxWidth()) {
                listOf(0.38f, 0.46f, 0.78f, 0.54f, 0.34f, 0.42f).forEachIndexed { index, heightFraction ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(HealthSpacing.VitalBar * heightFraction)
                            .background(if (index in 2..3) HealthColors.Success else HealthColors.SurfaceSubtle),
                    )
                }
            }
        }
        if (chip != null) {
            Text(
                chip.uppercase(),
                modifier = Modifier
                    .background(HealthColors.SurfaceSubtle, HealthShapes.SmallCard)
                    .padding(horizontal = HealthSpacing.Xs, vertical = HealthSpacing.Xs),
                color = HealthColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun RecoveryCurve(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(HealthSpacing.ChartHeight)
            .background(HealthColors.SurfaceSubtle),
    ) {
        val actual = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.82f)
            cubicTo(size.width * 0.18f, size.height * 0.54f, size.width * 0.35f, size.height * 0.42f, size.width * 0.52f, size.height * 0.48f)
        }
        val predicted = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.80f)
            cubicTo(size.width * 0.22f, size.height * 0.34f, size.width * 0.44f, size.height * 0.40f, size.width * 0.56f, size.height * 0.55f)
            cubicTo(size.width * 0.70f, size.height * 0.74f, size.width * 0.86f, size.height * 0.50f, size.width * 0.94f, size.height * 0.10f)
        }
        drawPath(predicted, HealthColors.Success, style = Stroke(width = (HealthSpacing.Stroke * 2).toPx(), cap = StrokeCap.Round))
        drawPath(actual, HealthColors.TextPrimary, style = Stroke(width = (HealthSpacing.Stroke * 4).toPx(), cap = StrokeCap.Round))
        drawLine(
            color = HealthColors.Border,
            start = Offset(size.width * 0.56f, size.height * 0.43f),
            end = Offset(size.width * 0.56f, size.height * 0.55f),
            strokeWidth = (HealthSpacing.Stroke * 2).toPx(),
        )
    }
}

@Composable
fun DividerTitle(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
    ) {
        Text(text, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(HealthSpacing.Stroke)
                .background(HealthColors.Border),
        )
    }
}

@Composable
fun SmallCapsPill(text: String, color: androidx.compose.ui.graphics.Color = HealthColors.Success) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .background(color.copy(alpha = 0.22f), HealthShapes.Pill)
            .padding(horizontal = HealthSpacing.Sm, vertical = HealthSpacing.Xs),
        color = if (color == HealthColors.Danger) HealthColors.Danger else HealthColors.TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
    )
}
