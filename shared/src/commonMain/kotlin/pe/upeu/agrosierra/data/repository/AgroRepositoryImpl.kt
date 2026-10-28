package pe.upeu.agrosierra.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.data.remote.MapeoErrores
import pe.upeu.agrosierra.data.remote.dto.LaborDto
import pe.upeu.agrosierra.data.remote.dto.LaborRequestDto
import pe.upeu.agrosierra.data.remote.dto.LoteDto
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.TipoLabor
import pe.upeu.agrosierra.domain.repository.AgroRepository
import pe.upeu.agrosierra.domain.model.ErrorApi

@Serializable
private data class PaginaDto<T>(val contenido: List<T> = emptyList())

private fun LoteDto.dominio() = Lote(id, codigo, nombre, hectareas, cultivo, valle, campaniaCerrada, dosisMaximaPorHectarea)
private fun LaborDto.dominio(pendiente: Boolean = false) = Labor(id, loteId, TipoLabor.valueOf(tipo), fecha, responsable, cantidad, observacion, pendiente)

class AgroRepositoryImpl(private val local: BaseLocal, private val http: HttpClient) : AgroRepository {
    private val q get() = local.db.agroDbQueries

    override fun observarLotes(valle: String?, soloAbiertos: Boolean): Flow<List<Lote>> =
        q.observarTodosLotes().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.filter { row ->
                val coincideValle = valle.isNullOrBlank() || row.valle == valle
                val coincideAbierto = !soloAbiertos || row.campaniaCerrada == 0L
                coincideValle && coincideAbierto
            }.map { Lote(it.id, it.codigo, it.nombre, it.hectareas, it.cultivo, it.valle, it.campaniaCerrada != 0L, it.dosisMaxima) }
        }

    fun observarLotesPorBusqueda(busqueda: String): Flow<List<Lote>> =
        q.observarTodosLotes().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.filter { it.nombre.contains(busqueda, true) || it.codigo.contains(busqueda, true) }
                .map { Lote(it.id, it.codigo, it.nombre, it.hectareas, it.cultivo, it.valle, it.campaniaCerrada != 0L, it.dosisMaxima) }
        }

    override fun observarLabores(loteId: Long): Flow<List<Labor>> =
        q.observarLaboresPorLote(loteId).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { Labor(it.id, it.loteId, TipoLabor.valueOf(it.tipo), it.fecha, it.responsable, it.cantidad, it.observacion, it.pendiente != 0L) }
        }

    override suspend fun registrarLabor(loteId: Long, tipo: TipoLabor, fecha: String, responsable: String, cantidad: Double, observacion: String) {
        val idLocal = minOf(q.siguienteIdLocal().executeAsOne(), 0L) - 1L
        q.transaction { q.insertarLabor(idLocal, loteId, tipo.name, fecha, responsable, cantidad, observacion, 1L) }
    }

    suspend fun actualizarLabor(id: Long, loteId: Long, tipo: TipoLabor, fecha: String, responsable: String, cantidad: Double, observacion: String): Labor {
        val response = http.put("api/v1/labores/$id") {
            setBody(LaborRequestDto(loteId, tipo.name, fecha, responsable, cantidad, observacion))
        }
        if (!response.status.isSuccess()) throw MapeoErrores.mapear(response)
        val real = response.body<LaborDto>()
        q.transaction {
            q.eliminarLaborPorId(id)
            q.insertarLabor(real.id, real.loteId, real.tipo, real.fecha, real.responsable, real.cantidad, real.observacion, 0L)
        }
        return real.dominio(false)
    }

    suspend fun eliminarLabor(id: Long) {
        val response = http.delete("api/v1/labores/$id")
        if (!response.status.isSuccess()) throw MapeoErrores.mapear(response)
        q.transaction {
            q.eliminarLaborPorId(id)
        }
    }

    override suspend fun refrescar(): Int {
        sincronizar()
        val page = http.get("api/v1/lotes?pagina=0&tamanio=100").also { if (!it.status.isSuccess()) throw MapeoErrores.mapear(it) }.body<PaginaDto<LoteDto>>()
        q.transaction {
            page.contenido.forEach { dto ->
                val l = dto.dominio()
                q.insertarLote(l.id, l.codigo, l.nombre, l.hectareas, l.cultivo, l.valle, if (l.campaniaCerrada) 1L else 0L, l.dosisMaximaPorHectarea)
            }
        }
        page.contenido.forEach { lote -> cargarLabores(lote.id) }
        return page.contenido.size
    }

    override suspend fun sincronizar(): Int = withContext(Dispatchers.Default) {
        // 1) Subir primero las labores pendientes (ids locales negativos).
        val pendientes = q.observarLaboresPendientes().executeAsList()
        val reemplazos = mutableListOf<Pair<Long, LaborDto>>()
        pendientes.forEach { row ->
            val response = http.post("api/v1/labores") {
                setBody(LaborRequestDto(row.loteId, row.tipo, row.fecha, row.responsable, row.cantidad, row.observacion))
            }
            if (!response.status.isSuccess()) throw MapeoErrores.mapear(response)
            val real = response.body<LaborDto>()
            if (real.id <= 0L) throw ErrorApi.Desconocido("El servidor devolvió un identificador inválido")
            reemplazos += row.id to real
        }
        // 2) Reemplazar los ids provisionales por los reales en una sola transacción.
        q.transaction {
            reemplazos.forEach { (idLocal, real) ->
                q.eliminarLaborPendiente(idLocal)
                q.insertarLabor(real.id, real.loteId, real.tipo, real.fecha, real.responsable, real.cantidad, real.observacion, 0L)
            }
        }
        reemplazos.size
    }

    suspend fun cargarLabores(loteId: Long) {
        val page = http.get("api/v1/lotes/$loteId/labores?pagina=0&tamanio=100").also {
            if (!it.status.isSuccess()) throw MapeoErrores.mapear(it)
        }.body<PaginaDto<LaborDto>>()
        q.transaction {
            page.contenido.forEach { labor ->
                q.insertarLabor(labor.id, labor.loteId, labor.tipo, labor.fecha, labor.responsable, labor.cantidad, labor.observacion, 0L)
            }
        }
    }

}
