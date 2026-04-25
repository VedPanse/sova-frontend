package org.sova.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

actual fun sovaHttpClient(): HttpClient = HttpClient(Android)
