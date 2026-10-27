package pe.upeu.agrosierra.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.TipoLabor

interface AgroRepository {
    fun observarLotes(valle: String?, soloAbiertos: Boolean): Flow<List<Lote>>
    fun observarLabores(loteId: Long): Flow<List<Labor>>
    suspend fun registrarLabor(loteId: Long, tipo: TipoLabor, fecha: String, responsable: String, cantidad: Double, observacion: String)
    suspend fun sincronizar(): Int
    suspend fun refrescar(): Int
}
