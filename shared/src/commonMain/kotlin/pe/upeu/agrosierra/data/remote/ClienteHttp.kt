package pe.upeu.agrosierra.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createHttpClientEngine(): io.ktor.client.engine.HttpClientEngine

fun crearClienteHttp(
    leerToken: suspend () -> String? = { null },
    alExpirar: suspend () -> Unit = {}
): HttpClient {
    return HttpClient(createHttpClientEngine()) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                explicitNulls = false
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 15000
        }
        expectSuccess = false
        install(Auth) {
            bearer {
                loadTokens { leerToken()?.let { BearerTokens(it, null) } }
                sendWithoutRequest { request ->
                    val path = request.url.encodedPath
                    !path.contains("/auth/login")
                }
            }
        }
        defaultRequest {
            url(ApiConfig.baseUrl)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }
}

object ApiConfig {
    const val baseUrl = "http://10.0.2.2:8080/"
}
