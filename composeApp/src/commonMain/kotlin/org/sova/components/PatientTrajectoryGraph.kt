package org.sova.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.RiskLevel
import org.sova.model.Trajectory

@Composable
fun PatientTrajectoryGraph(
    trajectory: Trajectory,
    modifier: Modifier = Modifier,
) {
    val points = trajectory.points
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
            LegendDot("Now", HealthColors.Ink)
            LegendDot("Projected", HealthColors.Success)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HealthSpacing.ChartHeight)
                .background(HealthColors.SurfaceSubtle, HealthShapes.SmallCard)
                .padding(HealthSpacing.Sm),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(HealthSpacing.ChartHeight - HealthSpacing.Lg)) {
                if (points.isEmpty()) return@Canvas

                val left = size.width * 0.08f
                val right = size.width * 0.96f
                val top = size.height * 0.08f
                val bottom = size.height * 0.84f
                val graphWidth = right - left
                val graphHeight = bottom - top

                listOf(68 to RiskLevel.High, 36 to RiskLevel.Moderate, 18 to RiskLevel.Low).forEach { (score, risk) ->
                    val y = riskScoreY(score, top, graphHeight)
                    drawLine(
                        color = riskColor(risk).copy(alpha = 0.24f),
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = HealthSpacing.Stroke.toPx(),
                    )
                }

                val maxHours = points.mapNotNull { it.hoursFromNow }.maxOrNull()?.takeIf { it > 0.0 } ?: 6.0
                val plotted = points.mapIndexed { index, point ->
                    val xProgress = point.hoursFromNow?.let { (it / maxHours).toFloat().coerceIn(0f, 1f) }
                        ?: if (points.size == 1) 0f else index.toFloat() / points.lastIndex.toFloat()
                    val x = left + graphWidth * xProgress
                    Offset(x, riskScoreY(point.riskScore, top, graphHeight))
                }

                if (plotted.size > 1) {
                    val path = Path().apply {
                        moveTo(plotted.first().x, plotted.first().y)
                        plotted.windowed(2).forEach { (from, to) ->
                            val controlX = (from.x + to.x) / 2f
                            cubicTo(controlX, from.y, controlX, to.y, to.x, to.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = HealthColors.Success,
                        style = Stroke(width = HealthSpacing.SmallBar.toPx(), cap = StrokeCap.Round),
                    )
                }

                plotted.forEachIndexed { index, offset ->
                    val point = points[index]
                    val color = if (index == 0) HealthColors.Ink else riskColor(point.riskLevel)
                    drawCircle(color = HealthColors.Surface, radius = HealthSpacing.Xs.toPx(), center = offset)
                    drawCircle(color = color, radius = HealthSpacing.Xs.toPx() * 0.62f, center = offset)
                }
            }
            AxisLabel(
                text = "Projected risk score",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { rotationZ = -90f },
            )
            AxisLabel(
                text = "Time from now",
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { point ->
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs / 2)) {
                    Text(point.label, color = HealthColors.MutedBlue, style = MaterialTheme.typography.labelMedium)
                    Text("${point.riskLevel.name} ${point.riskScore}", color = riskColor(point.riskLevel), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.AxisLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = HealthColors.TextSecondary,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
        Box(
            modifier = Modifier
                .padding(top = HealthSpacing.Xs / 2)
                .height(HealthSpacing.SmallBar)
                .width(HealthSpacing.Md)
                .background(color, HealthShapes.Pill),
        )
        Text(label.uppercase(), color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

private fun riskScoreY(score: Int, top: Float, graphHeight: Float): Float =
    top + graphHeight * (1f - score.coerceIn(0, 100).toFloat() / 100f)

private fun riskColor(risk: RiskLevel): Color =
    when (risk) {
        RiskLevel.Low -> HealthColors.Success
        RiskLevel.Moderate -> HealthColors.Warning
        RiskLevel.High -> HealthColors.Danger
    }
