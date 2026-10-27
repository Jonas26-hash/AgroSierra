package pe.upeu.agrosierra.data.remote

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import pe.upeu.agrosierra.data.remote.dto.ErrorResponseDto
import pe.upeu.agrosierra.domain.model.ErrorApi

object MapeoErrores {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun mapear(response: HttpResponse): ErrorApi {
        return try {
            val text = response.bodyAsText()
            val errorDto = json.decodeFromString<ErrorResponseDto>(text)
            when (response.status.value) {
                400 -> {
                    if (errorDto.validationErrors.isNotEmpty()) {
                        ErrorApi.Validacion(errorDto.validationErrors)
                    } else {
                        ErrorApi.Desconocido(errorDto.message ?: "Datos invalidos")
                    }
                }
                401 -> ErrorApi.NoAutorizado()
                403 -> ErrorApi.SinPrivilegio()
                404 -> ErrorApi.NoEncontrado()
                409 -> ErrorApi.Conflicto(errorDto.message ?: "Conflicto")
                500 -> ErrorApi.Servidor()
                else -> ErrorApi.Desconocido(errorDto.message ?: "Error desconocido")
            }
        } catch (e: Exception) {
            when (response.status.value) {
                401 -> ErrorApi.NoAutorizado()
                403 -> ErrorApi.SinPrivilegio()
                404 -> ErrorApi.NoEncontrado()
                409 -> ErrorApi.Conflicto("Conflicto")
                400 -> ErrorApi.Validacion(emptyMap())
                500 -> ErrorApi.Servidor()
                else -> ErrorApi.Desconocido(e.message ?: "Error")
            }
        }
    }
}
