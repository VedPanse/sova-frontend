package org.sova.data

import kotlinx.serialization.json.Json
import org.sova.logic.PatientIdGenerator

expect object PatientLocalStorage {
    fun readPatientId(): String?
    fun writePatientId(value: String)
    fun readDraft(): String?
    fun writeDraft(value: String)
    fun clearDraft()
}

object PatientProfilePersistence {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun patientId(): String {
        val existing = PatientLocalStorage.readPatientId()
        if (!existing.isNullOrBlank()) return existing
        val created = PatientIdGenerator.newUuid()
        PatientLocalStorage.writePatientId(created)
        return created
    }

    fun saveDraft(payload: PatientProfilePayload) {
        PatientLocalStorage.writeDraft(json.encodeToString(PatientProfilePayload.serializer(), payload))
    }

    fun loadDraft(): PatientProfilePayload? =
        PatientLocalStorage.readDraft()?.let {
            runCatching { json.decodeFromString(PatientProfilePayload.serializer(), it) }.getOrNull()
        }

    fun clearDraft() {
        PatientLocalStorage.clearDraft()
    }
}
