package org.sova.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java

actual fun sovaHttpClient(): HttpClient = HttpClient(Java)
