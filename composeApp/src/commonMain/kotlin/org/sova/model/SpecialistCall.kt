package org.sova.model

data class SpecialistCallSession(
    val sessionId: String,
    val websocketUrl: String,
)

data class SpecialistCallLine(
    val speaker: String,
    val text: String,
    val fromPatient: Boolean,
)

sealed interface SpecialistCallEvent {
    data class Started(val sessionId: String, val specialistName: String) : SpecialistCallEvent
    data class Caption(val line: SpecialistCallLine) : SpecialistCallEvent
    data class Audio(val audioBase64: String, val format: String) : SpecialistCallEvent
    data class Error(val message: String) : SpecialistCallEvent
    data object Ended : SpecialistCallEvent
}

sealed interface SpecialistCallState {
    data object Connecting : SpecialistCallState
    data class Connected(
        val lines: List<SpecialistCallLine> = emptyList(),
        val error: String? = null,
    ) : SpecialistCallState
    data class Failed(val message: String) : SpecialistCallState
    data object Ended : SpecialistCallState
}

