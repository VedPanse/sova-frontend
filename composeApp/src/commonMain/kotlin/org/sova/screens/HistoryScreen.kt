package org.sova.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.model.HistoryItem

@Composable
fun HistoryScreen(
    history: List<HistoryItem>,
    modifier: Modifier = Modifier,
) {
    var showOlder by remember { mutableStateOf(false) }
    var selectedMeeting by remember { mutableStateOf<HistoryItem?>(null) }
    val allMeetings = historyWithSummaries(history)
    val visibleMeetings = if (showOlder) allMeetings else allMeetings.filter(::isTodayOrYesterday)

    selectedMeeting?.let { meeting ->
        MeetingSummaryReader(
            meeting = meeting,
            onBack = { selectedMeeting = null },
            modifier = modifier,
        )
        return
    }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                JournalLabel("Patient status: stable")
                Text("Actions &\nRecords", color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text(
                    "A chronological ledger of patient interactions and clinical escalations overseen by the SOVA Care Protocol.",
                    color = HealthColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            Text("Recent Meetings", color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
        }
        items(visibleMeetings) { item ->
            JournalCard(
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { selectedMeeting = item },
            ) {
                MeetingCardContent(item)
            }
        }
        if (!showOlder && allMeetings.any { !isTodayOrYesterday(it) }) item {
            Text(
                "Load Older Journal Entries",
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { showOlder = true },
                color = HealthColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun MeetingCardContent(item: HistoryItem) {
    BoxWithConstraints {
        if (maxWidth < 420.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        NoWrapLabel(item.time.substringBefore(",").ifBlank { "Today" })
                        NoWrapLabel(item.time.substringAfter(", ", item.time))
                    }
                    MeetingTag(item, modifier = Modifier.width(96.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                    Text(item.title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    Text(item.summary, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                Column(modifier = Modifier.width(104.dp)) {
                    NoWrapLabel(item.time.substringBefore(",").ifBlank { "Today" })
                    NoWrapLabel(item.time.substringAfter(", ", item.time))
                }
                MeetingTag(item, modifier = Modifier.width(92.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                    Text(item.title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    Text(item.summary, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun MeetingTag(item: HistoryItem, modifier: Modifier = Modifier) {
    Text(
        eventTag(item),
        modifier = modifier
            .background(HealthColors.SurfaceSubtle, HealthShapes.SmallCard)
            .padding(HealthSpacing.Xs),
        color = HealthColors.TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

@Composable
private fun NoWrapLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = HealthColors.MutedBlue,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

@Composable
private fun MeetingSummaryReader(
    meeting: HistoryItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            SummaryBackButton(onClick = onBack)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                JournalLabel("Meeting summary")
                Text(meeting.title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text(meeting.time, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            JournalCard {
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    JournalLabel("Summary")
                    Text(meeting.summary, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            JournalCard {
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    JournalLabel("Key points")
                    summaryPointsFor(meeting).forEach {
                        Text(it, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBackButton(onClick: () -> Unit) {
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

private fun historyWithSummaries(history: List<HistoryItem>): List<HistoryItem> =
    history + listOf(
        HistoryItem(
            title = "AI Care Summary",
            summary = "Patient reported feeling steady after medication. AI care team advised hydration and passive monitoring.",
            time = "Yesterday, 6:18 PM",
        ),
        HistoryItem(
            title = "Caregiver Summary",
            summary = "Sova generated a concise caregiver-ready summary covering vitals, medication adherence, and low short-term risk.",
            time = "Sep 22, 9:20 AM",
        ),
    )

private fun eventTag(item: HistoryItem): String =
    if (item.title.contains("Summary", ignoreCase = true)) {
        "SUMMARY"
    } else {
        "AI REPORT"
    }

private fun isTodayOrYesterday(item: HistoryItem): Boolean =
    item.time.startsWith("Today") || item.time.startsWith("Yesterday")

private fun summaryPointsFor(item: HistoryItem): List<String> =
    when {
        item.title.contains("Caregiver", ignoreCase = true) -> listOf(
            "Vitals and medication adherence were summarized for the caregiver.",
            "Short-term risk remained low at the time of review.",
            "Caregiver escalation remains available if symptoms change.",
        )
        item.title.contains("AI Care", ignoreCase = true) -> listOf(
            "Patient felt steady after medication.",
            "Hydration and passive monitoring were recommended.",
            "No urgent escalation was recommended.",
        )
        else -> listOf(
            item.summary,
            "Only the meeting summary is shown.",
            "Review the summary and continue with the recommended action.",
        )
    }
