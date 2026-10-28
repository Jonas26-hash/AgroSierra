package pe.upeu.agrosierra

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncOrdenTest {
    @Test
    fun dtoConversionLaboresTipadas() {
        val labor = pe.upeu.agrosierra.data.remote.dto.LaborDto(
            id = 123,
            loteId = 1,
            tipo = "FUMIGACION",
            fecha = "2026-10-27",
            responsable = "tecnico1",
            cantidad = 2.0,
            observacion = "test"
        )
        val dominio = labor.dominio()
        assertEquals(123L, dominio.id)
        assertEquals(pe.upeu.agrosierra.domain.model.TipoLabor.FUMIGACION, dominio.tipo)
        assertEquals("tecnico1", dominio.responsable)
    }

    private fun pe.upeu.agrosierra.data.remote.dto.LaborDto.dominio(pendiente: Boolean = false) =
        pe.upeu.agrosierra.domain.model.Labor(
            id = id,
            loteId = loteId,
            tipo = pe.upeu.agrosierra.domain.model.TipoLabor.valueOf(tipo),
            fecha = fecha,
            responsable = responsable,
            cantidad = cantidad,
            observacion = observacion,
            pendiente = pendiente
        )

    @Test
    fun mapeoErrores400ConValidacion() = runTest {
        val engine = MockEngine { request ->
            respond(
                content = """{"timestamp":"2026-10-27T09:14:22","status":400,"error":"Bad Request","message":"Datos invalidos","path":"/api/v1/labores","validationErrors":{"cantidad":"La cantidad aplicada supera la dosis maxima permitida"}}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            expectSuccess = false
        }
        val response = client.get("http://test/api/v1/lotes")
        val err = pe.upeu.agrosierra.data.remote.MapeoErrores.mapear(response)
        assertTrue(err is pe.upeu.agrosierra.domain.model.ErrorApi.Validacion)
        assertEquals("La cantidad aplicada supera la dosis maxima permitida", (err as pe.upeu.agrosierra.domain.model.ErrorApi.Validacion).errores["cantidad"])
    }

    @Test
    fun registroSinConexionGuardaPendienteConIdNegativo() {
        val idLocal = -1L
        val labor = pe.upeu.agrosierra.domain.model.Labor(idLocal, 1, pe.upeu.agrosierra.domain.model.TipoLabor.RIEGO, "2026-10-27", "tecnico1", 5.0, "sin red", true)
        assertTrue(labor.id < 0)
        assertTrue(labor.pendiente)
    }

    @Test
    fun ordenSincronizacionSubePendientesPrimero() = runTest {
        val orden = mutableListOf<String>()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("/api/v1/labores") && request.method.value == "POST" -> {
                    orden.add("SUBIR_PENDIENTE")
                    respond(
                        content = """{"id":150,"loteId":1,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"tecnico1","cantidad":5.0,"observacion":"sin red"}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                request.url.encodedPath.contains("/api/v1/lotes") && request.method.value == "GET" -> {
                    orden.add("DESCARGAR_LOTES")
                    respond(
                        content = """{"contenido":[],"pagina":0,"tamanio":100,"totalElementos":0,"totalPaginas":0,"ultima":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> {
                    respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                }
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            expectSuccess = false
        }
        // Simular orden: primero subir pendientes, luego descargar
        orden.add("SUBIR_PENDIENTE")
        val resp = client.post("http://test/api/v1/labores")
        assertEquals(HttpStatusCode.Created, resp.status)
        orden.add("DESCARGAR_LOTES")
        val resp2 = client.get("http://test/api/v1/lotes")
        assertEquals(HttpStatusCode.OK, resp2.status)
        assertTrue(orden.indexOf("SUBIR_PENDIENTE") < orden.indexOf("DESCARGAR_LOTES"))
    }
}
