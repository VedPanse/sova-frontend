package org.sova.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
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
    val allMeetings = historyWithTranscripts(history)
    val visibleMeetings = if (showOlder) allMeetings else allMeetings.filter(::isTodayOrYesterday)

    selectedMeeting?.let { meeting ->
        TranscriptReader(
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
                Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    Column(modifier = Modifier.weight(0.34f)) {
                        JournalLabel(item.time.substringBefore(",").ifBlank { "Today" })
                        JournalLabel(item.time.substringAfter(", ", item.time))
                    }
                    Text(
                        eventTag(item),
                        modifier = Modifier
                            .widthIn(min = HealthSpacing.CouncilAvatar)
                            .background(HealthColors.SurfaceSubtle, HealthShapes.SmallCard)
                            .padding(HealthSpacing.Xs),
                        color = HealthColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                        Text(item.title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(item.summary, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
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
private fun TranscriptReader(
    meeting: HistoryItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Text(
                "Back to Recent Meetings",
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = onBack),
                color = HealthColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                JournalLabel("Meeting transcript")
                Text(meeting.title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text(meeting.time, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(transcriptFor(meeting)) { message ->
            TranscriptBubble(message)
        }
    }
}

@Composable
private fun TranscriptBubble(message: TranscriptMessage) {
    val isPatient = message.speaker == "Patient"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isPatient) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .background(
                    color = if (isPatient) HealthColors.AccentSoft else HealthColors.Surface,
                    shape = HealthShapes.Card,
                )
                .padding(HealthSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
                if (!isPatient) SpeakerDot(message.speaker)
                JournalLabel(message.speaker, color = if (isPatient) HealthColors.Accent else HealthColors.MutedBlue)
                if (isPatient) SpeakerDot(message.speaker)
            }
            Text(
                message.text,
                color = HealthColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SpeakerDot(speaker: String) {
    Box(
        modifier = Modifier
            .size(HealthSpacing.Lg)
            .background(if (speaker == "Patient") HealthColors.Accent else HealthColors.Success, HealthShapes.Pill)
            .padding(HealthSpacing.Xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(speaker.take(1), color = HealthColors.Surface, style = MaterialTheme.typography.labelMedium)
    }
}

private fun historyWithTranscripts(history: List<HistoryItem>): List<HistoryItem> =
    history + listOf(
        HistoryItem(
            title = "AI Care Transcript",
            summary = "Patient reported feeling steady after medication. AI care team advised hydration and passive monitoring.",
            time = "Yesterday, 6:18 PM",
        ),
        HistoryItem(
            title = "Doctor Summary Transcript",
            summary = "Sova generated a concise provider-ready summary covering vitals, medication adherence, and low short-term risk.",
            time = "Sep 22, 9:20 AM",
        ),
    )

private fun eventTag(item: HistoryItem): String =
    if (item.title.contains("Transcript", ignoreCase = true)) {
        "TRANSCRIPT"
    } else {
        "AI REPORT"
    }

private fun isTodayOrYesterday(item: HistoryItem): Boolean =
    item.time.startsWith("Today") || item.time.startsWith("Yesterday")

private data class TranscriptMessage(
    val speaker: String,
    val text: String,
)

private fun transcriptFor(item: HistoryItem): List<TranscriptMessage> =
    when {
        item.title.contains("Doctor", ignoreCase = true) -> listOf(
            TranscriptMessage("Clinician", "I reviewed your vitals and the Sova summary. Heart rate, oxygen, and medication adherence look stable."),
            TranscriptMessage("Patient", "I felt a little tired this morning, but breathing has been comfortable."),
            TranscriptMessage("Clinician", "That matches the trend. Continue hydration, normal meals, and light movement as tolerated."),
            TranscriptMessage("Clinician", "Escalate if oxygen drops, chest discomfort appears, or fatigue becomes sudden or severe."),
        )
        item.title.contains("Transcript", ignoreCase = true) -> listOf(
            TranscriptMessage("Sova AI", "I’m checking your recovery signals now. Heart rate is steady and oxygen is holding at 98%."),
            TranscriptMessage("Patient", "I feel okay. I took my medication and slept better than yesterday."),
            TranscriptMessage("Sova AI", "Good. HRV and sleep support continued passive monitoring. No urgent care action is recommended."),
            TranscriptMessage("Sova AI", "Recommended next step: hydrate, continue normal medication timing, and check back if symptoms change."),
        )
        else -> listOf(
            TranscriptMessage("Sova AI", item.summary),
            TranscriptMessage("Patient", "No new severe symptoms reported during this check-in."),
            TranscriptMessage("Sova AI", "The meeting closed with continued monitoring and no escalation required."),
        )
    }
