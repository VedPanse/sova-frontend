package org.sova.server

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080) {
        patientProfileModule()
    }.start(wait = true)
}

fun Application.patientProfileModule() {
    val repository = BigQueryPatientProfileRepository()

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, SyncResponse(false, "", cause.message ?: "Sync failed"))
        }
    }

    routing {
        get("/api/patient-profile/{patientId}") {
            val patientId = call.parameters["patientId"].orEmpty()
            val profile = repository.find(patientId)
            if (profile == null) {
                call.respond(HttpStatusCode.NotFound, SyncResponse(false, patientId, "Profile not found"))
            } else {
                call.respond(profile)
            }
        }
        post("/api/patient-profile/sync") {
            val request = call.receive<PatientProfileRequest>()
            val validationError = request.validationError()
            if (validationError != null) {
                call.respond(HttpStatusCode.BadRequest, SyncResponse(false, request.patientId, validationError))
                return@post
            }
            repository.upsert(request)
            call.respond(SyncResponse(true, request.patientId, "Profile synced"))
        }
    }
}

@Serializable
data class PatientProfileRequest(
    val patientId: String,
    val dateOfBirth: String,
    val age: Int,
    val gender: String,
    val address: String = "",
    val surgery: String,
    val dischargeDate: String,
    val doctorPhoneNumber: String = "",
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val allergies: String = "None",
    val currentMedications: String = "None",
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val patientId: String,
    val message: String = "",
)

fun PatientProfileRequest.toBigQueryRow(): Map<String, Any?> =
    linkedMapOf(
        "patientId" to patientId,
        "DateOfBirth" to dateOfBirth,
        "Age" to age,
        "Gender" to gender,
        "Address" to address,
        "Surgery" to surgery,
        "DischargeDate" to dischargeDate,
        "DoctorPhoneNumber" to doctorPhoneNumber,
        "EmergencyContactName" to emergencyContactName,
        "EmergencyContactPhone" to emergencyContactPhone,
        "Allergies" to allergies,
        "CurrentMedications" to currentMedications,
        "BloodPressure" to null,
        "HeartRate" to null,
        "RiskLevel" to null,
    )

fun PatientProfileRequest.validationError(): String? =
    when {
        patientId.isBlank() -> "patientId is required."
        parseIsoDate(dateOfBirth) == null -> "dateOfBirth must be YYYY-MM-DD."
        age < 0 || age > 130 -> "age must be reasonable."
        gender.isBlank() -> "gender is required."
        surgery.isBlank() -> "surgery is required."
        parseIsoDate(dischargeDate) == null -> "dischargeDate must be YYYY-MM-DD."
        doctorPhoneNumber.isNotBlank() && !doctorPhoneNumber.hasValidPhoneShape() -> "doctorPhoneNumber is invalid."
        emergencyContactName.isBlank() -> "emergencyContactName is required."
        !emergencyContactPhone.hasValidPhoneShape() -> "emergencyContactPhone is invalid."
        else -> null
    }

private fun parseIsoDate(value: String): java.time.LocalDate? =
    runCatching { java.time.LocalDate.parse(value) }.getOrNull()

private fun String.hasValidPhoneShape(): Boolean =
    count { it.isDigit() } >= 7

class BigQueryPatientProfileRepository {
    private val bigQuery = BigQueryOptions.getDefaultInstance().service
    private val table = "`automaticbalancetransfer.sova.patientProfile`"

    fun upsert(profile: PatientProfileRequest) {
        val sql = """
            MERGE $table T
            USING (
              SELECT
                @patientId AS patientId,
                @dateOfBirth AS DateOfBirth,
                @age AS Age,
                @gender AS Gender,
                @address AS Address,
                @surgery AS Surgery,
                @dischargeDate AS DischargeDate,
                @doctorPhoneNumber AS DoctorPhoneNumber,
                @emergencyContactName AS EmergencyContactName,
                @emergencyContactPhone AS EmergencyContactPhone,
                @allergies AS Allergies,
                @currentMedications AS CurrentMedications
            ) S
            ON T.patientId = S.patientId
            WHEN MATCHED THEN UPDATE SET
              DateOfBirth = S.DateOfBirth,
              Age = S.Age,
              Gender = S.Gender,
              Address = S.Address,
              Surgery = S.Surgery,
              DischargeDate = S.DischargeDate,
              DoctorPhoneNumber = S.DoctorPhoneNumber,
              EmergencyContactName = S.EmergencyContactName,
              EmergencyContactPhone = S.EmergencyContactPhone,
              Allergies = S.Allergies,
              CurrentMedications = S.CurrentMedications
            WHEN NOT MATCHED THEN INSERT (
              patientId, DateOfBirth, Age, Gender, Address, Surgery, DischargeDate,
              DoctorPhoneNumber, EmergencyContactName, EmergencyContactPhone,
              Allergies, CurrentMedications, BloodPressure, HeartRate, RiskLevel
            ) VALUES (
              S.patientId, S.DateOfBirth, S.Age, S.Gender, S.Address, S.Surgery, S.DischargeDate,
              S.DoctorPhoneNumber, S.EmergencyContactName, S.EmergencyContactPhone,
              S.Allergies, S.CurrentMedications, NULL, NULL, NULL
            )
        """.trimIndent()
        bigQuery.query(
            QueryJobConfiguration.newBuilder(sql)
                .addNamedParameter("patientId", QueryParameterValue.string(profile.patientId))
                .addNamedParameter("dateOfBirth", QueryParameterValue.date(profile.dateOfBirth))
                .addNamedParameter("age", QueryParameterValue.int64(profile.age.toLong()))
                .addNamedParameter("gender", QueryParameterValue.string(profile.gender))
                .addNamedParameter("address", QueryParameterValue.string(profile.address))
                .addNamedParameter("surgery", QueryParameterValue.string(profile.surgery))
                .addNamedParameter("dischargeDate", QueryParameterValue.date(profile.dischargeDate))
                .addNamedParameter("doctorPhoneNumber", QueryParameterValue.string(profile.doctorPhoneNumber))
                .addNamedParameter("emergencyContactName", QueryParameterValue.string(profile.emergencyContactName))
                .addNamedParameter("emergencyContactPhone", QueryParameterValue.string(profile.emergencyContactPhone))
                .addNamedParameter("allergies", QueryParameterValue.string(profile.allergies))
                .addNamedParameter("currentMedications", QueryParameterValue.string(profile.currentMedications))
                .build(),
        )
    }

    fun find(patientId: String): PatientProfileRequest? {
        if (patientId.isBlank()) return null
        val sql = """
            SELECT patientId, DateOfBirth, Age, Gender, Address, Surgery, DischargeDate,
                   DoctorPhoneNumber, EmergencyContactName, EmergencyContactPhone,
                   Allergies, CurrentMedications
            FROM $table
            WHERE patientId = @patientId
            LIMIT 1
        """.trimIndent()
        val rows = bigQuery.query(
            QueryJobConfiguration.newBuilder(sql)
                .addNamedParameter("patientId", QueryParameterValue.string(patientId))
                .build(),
        )
        val row = rows.iterateAll().firstOrNull() ?: return null
        return PatientProfileRequest(
            patientId = row["patientId"].stringValue,
            dateOfBirth = row["DateOfBirth"].stringValue,
            age = row["Age"].longValue.toInt(),
            gender = row["Gender"].stringValue,
            address = row["Address"].stringOrEmpty(),
            surgery = row["Surgery"].stringValue,
            dischargeDate = row["DischargeDate"].stringValue,
            doctorPhoneNumber = row["DoctorPhoneNumber"].stringOrEmpty(),
            emergencyContactName = row["EmergencyContactName"].stringValue,
            emergencyContactPhone = row["EmergencyContactPhone"].stringValue,
            allergies = row["Allergies"].stringOrNone(),
            currentMedications = row["CurrentMedications"].stringOrNone(),
        )
    }
}

private fun com.google.cloud.bigquery.FieldValue.stringOrEmpty(): String =
    if (isNull) "" else stringValue

private fun com.google.cloud.bigquery.FieldValue.stringOrNone(): String =
    if (isNull || stringValue.isBlank()) "None" else stringValue
