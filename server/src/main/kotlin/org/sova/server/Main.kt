package org.sova.server

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryException
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.FormatOptions
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.WriteChannelConfiguration
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger

private val serverLogger: Logger = Logger.getLogger("org.sova.server")
private const val GoogleCloudProjectId = "automaticbalancetransfer"
private val LocalBigQueryCredentialsPaths: List<Path> = listOf(
    Path.of("secrets", "bigquery-service-account.json"),
    Path.of("..", "secrets", "bigquery-service-account.json"),
)
private val ServerJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080) {
        patientProfileModule()
    }.start(wait = true)
}

fun Application.patientProfileModule() {
    val repository = BigQueryPatientProfileRepository()
    val vitalsRepository = BigQueryVitalsRepository()

    install(ContentNegotiation) {
        json(ServerJson)
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            serverLogger.log(Level.SEVERE, "Unhandled server error", cause)
            call.respond(HttpStatusCode.InternalServerError, SyncResponse(false, "", cause.message ?: "Sync failed"))
        }
    }

    routing {
        get("/api/patient-profile/{patientId}") {
            val patientId = call.parameters["patientId"].orEmpty()
            val profile = try {
                repository.find(patientId)
            } catch (cause: Throwable) {
                serverLogger.log(Level.SEVERE, "Unable to read patient profile from BigQuery for patientId=$patientId", cause)
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    SyncResponse(false, patientId, cause.databaseUnavailableMessage("Profile read failed")),
                )
                return@get
            }
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
            try {
                repository.upsert(request)
            } catch (cause: Throwable) {
                serverLogger.log(Level.SEVERE, "Unable to sync patient profile to BigQuery for patientId=${request.patientId}", cause)
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    SyncResponse(false, request.patientId, cause.databaseUnavailableMessage("Profile sync failed")),
                )
                return@post
            }
            call.respond(SyncResponse(true, request.patientId, "Profile synced"))
        }
        get("/api/vitals/latest/{patientId}") {
            val patientId = call.parameters["patientId"].orEmpty()
            if (patientId.isBlank()) {
                serverLogger.warning("Unable to read latest vitals: patientId was blank")
                call.respond(HttpStatusCode.BadRequest, SyncResponse(false, patientId, "patientId is required"))
                return@get
            }

            val vitals = try {
                vitalsRepository.latest(patientId)
            } catch (cause: Throwable) {
                serverLogger.log(Level.SEVERE, "Unable to read latest vitals from BigQuery for patientId=$patientId", cause)
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    SyncResponse(false, patientId, cause.databaseUnavailableMessage("Vitals read failed")),
                )
                return@get
            }

            if (vitals == null) {
                serverLogger.info("No vitals row found in BigQuery for patientId=$patientId")
                call.respond(HttpStatusCode.NotFound, SyncResponse(false, patientId, "Vitals not found"))
            } else {
                call.respond(vitals)
            }
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

@Serializable
data class VitalsResponse(
    val patientId: String,
    val heartRate: Int? = null,
    val bloodPressure: String? = null,
    val temperature: Double? = null,
    val timestamp: String? = null,
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

private fun PatientProfileRequest.toBigQueryJsonLine(): String =
    ServerJson.encodeToString(
        JsonObject(
            mapOf(
                "patientId" to JsonPrimitive(patientId),
                "DateOfBirth" to JsonPrimitive(dateOfBirth),
                "Age" to JsonPrimitive(age),
                "Gender" to JsonPrimitive(gender),
                "Address" to JsonPrimitive(address),
                "Surgery" to JsonPrimitive(surgery),
                "DischargeDate" to JsonPrimitive(dischargeDate),
                "DoctorPhoneNumber" to JsonPrimitive(doctorPhoneNumber),
                "EmergencyContactName" to JsonPrimitive(emergencyContactName),
                "EmergencyContactPhone" to JsonPrimitive(emergencyContactPhone),
                "Allergies" to JsonPrimitive(allergies),
                "CurrentMedications" to JsonPrimitive(currentMedications),
            ),
        ),
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

private class BigQueryCredentialsMissingException : IllegalStateException(
    "BigQuery credentials are missing. Set GOOGLE_APPLICATION_CREDENTIALS to a service account JSON path, " +
        "or place the ignored local dev key at one of: ${LocalBigQueryCredentialsPaths.joinToString { it.toAbsolutePath().normalize().toString() }}.",
)

private fun Throwable.databaseUnavailableMessage(fallback: String): String =
    if (this is BigQueryCredentialsMissingException) message ?: fallback else fallback

private fun BigQueryException.isDmlBillingRestriction(): Boolean {
    val text = listOfNotNull(message, cause?.message).joinToString(" ")
    return "billing" in text.lowercase() && "dml" in text.lowercase()
}

private fun createBigQueryService(): BigQuery? {
    val credentials = loadGoogleCredentials()
    if (credentials == null) {
        serverLogger.warning(
            "BigQuery credentials are not configured. Set GOOGLE_APPLICATION_CREDENTIALS or add " +
                "one of ${LocalBigQueryCredentialsPaths.joinToString { it.toAbsolutePath().normalize().toString() }} for local development.",
        )
        return null
    }
    return BigQueryOptions.newBuilder()
        .setProjectId(GoogleCloudProjectId)
        .setCredentials(credentials)
        .build()
        .service
}

private fun loadGoogleCredentials(): GoogleCredentials? {
    val environmentPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        ?.takeIf { it.isNotBlank() }
        ?.let { Path.of(it) }

    if (environmentPath != null) {
        if (!Files.exists(environmentPath)) {
            serverLogger.warning("GOOGLE_APPLICATION_CREDENTIALS points to a missing file: $environmentPath")
            return null
        }
        return Files.newInputStream(environmentPath).use { ServiceAccountCredentials.fromStream(it) }
    }

    LocalBigQueryCredentialsPaths.firstOrNull { Files.exists(it) }?.let { localPath ->
        serverLogger.info("Using local ignored BigQuery credentials at ${localPath.toAbsolutePath().normalize()}")
        return Files.newInputStream(localPath).use { ServiceAccountCredentials.fromStream(it) }
    }

    return runCatching { GoogleCredentials.getApplicationDefault() }
        .onFailure { cause ->
            serverLogger.info("Google application default credentials are unavailable: ${cause.message.orEmpty()}")
        }
        .getOrNull()
}

class BigQueryPatientProfileRepository {
    private val bigQuery = createBigQueryService()
    private val table = "`automaticbalancetransfer.sova.patientProfile`"

    fun upsert(profile: PatientProfileRequest) {
        try {
            mergeUpsert(profile)
        } catch (cause: BigQueryException) {
            if (!cause.isDmlBillingRestriction()) throw cause

            val existing = find(profile.patientId)
            if (existing != null) {
                serverLogger.warning(
                    "BigQuery DML is blocked by free-tier billing for patientId=${profile.patientId}. " +
                        "Existing profile row found, so sync is being treated as successful without updating BigQuery.",
                )
                return
            }

            serverLogger.warning(
                "BigQuery DML is blocked by free-tier billing for patientId=${profile.patientId}. " +
                    "Appending new profile through a BigQuery load job instead.",
            )
            appendWithLoadJob(profile)
        }
    }

    private fun mergeUpsert(profile: PatientProfileRequest) {
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
        requireBigQuery().query(
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

    private fun appendWithLoadJob(profile: PatientProfileRequest) {
        val tableId = TableId.of(GoogleCloudProjectId, "sova", "patientProfile")
        val configuration = WriteChannelConfiguration.newBuilder(tableId)
            .setFormatOptions(FormatOptions.json())
            .setWriteDisposition(JobInfo.WriteDisposition.WRITE_APPEND)
            .build()
        val writer = requireBigQuery().writer(configuration)
        Channels.newOutputStream(writer).use { output ->
            output.write((profile.toBigQueryJsonLine() + "\n").toByteArray(StandardCharsets.UTF_8))
        }
        val job = writer.job.waitFor() ?: error("BigQuery load job disappeared before completion")
        job.status.error?.let { error ->
            throw IllegalStateException("BigQuery load job failed: ${error.message}")
        }
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
        val rows = requireBigQuery().query(
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

    private fun requireBigQuery(): BigQuery =
        bigQuery ?: throw BigQueryCredentialsMissingException()
}

class BigQueryVitalsRepository {
    private val bigQuery = createBigQueryService()
    private val table = "`automaticbalancetransfer.sova.vitals`"

    fun latest(patientId: String): VitalsResponse? {
        if (patientId.isBlank()) return null
        val sql = """
            SELECT patientId, HeartRate, BloodPressure, Temperature, TimeStamp
            FROM $table
            WHERE patientId = @patientId
            ORDER BY TimeStamp DESC
            LIMIT 1
        """.trimIndent()
        val rows = requireBigQuery().query(
            QueryJobConfiguration.newBuilder(sql)
                .addNamedParameter("patientId", QueryParameterValue.string(patientId))
                .build(),
        )
        val row = rows.iterateAll().firstOrNull() ?: return null
        return VitalsResponse(
            patientId = row["patientId"].stringValue,
            heartRate = row["HeartRate"].intOrNull(),
            bloodPressure = row["BloodPressure"].stringOrEmpty().ifBlank { null },
            temperature = row["Temperature"].doubleOrNull(),
            timestamp = row["TimeStamp"].stringOrEmpty().ifBlank { null },
        )
    }

    private fun requireBigQuery(): BigQuery =
        bigQuery ?: throw BigQueryCredentialsMissingException()
}

private fun com.google.cloud.bigquery.FieldValue.stringOrEmpty(): String =
    if (isNull) "" else stringValue

private fun com.google.cloud.bigquery.FieldValue.stringOrNone(): String =
    if (isNull || stringValue.isBlank()) "None" else stringValue

private fun com.google.cloud.bigquery.FieldValue.intOrNull(): Int? =
    if (isNull) null else longValue.toInt()

private fun com.google.cloud.bigquery.FieldValue.doubleOrNull(): Double? =
    if (isNull) null else doubleValue
