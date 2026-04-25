package org.sova.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun sovaHttpClient(): HttpClient = HttpClient(Darwin)
