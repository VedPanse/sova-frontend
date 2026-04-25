package org.sova.data

object BigQueryConfig {
    const val projectId = "automaticbalancetransfer"
    const val datasetId = "sova"
    const val tableId = "PatientProfile"
    const val location = "US"
    const val serviceAccountJsonPath = "secrets/bigquery-service-account.json"
}
