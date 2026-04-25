package org.sova.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun sovaHttpClient(): HttpClient = HttpClient(Js)
