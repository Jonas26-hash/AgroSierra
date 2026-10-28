package pe.upeu.agrosierra.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.upeu.agrosierra.domain.repository.AgroRepository

class SincronizarAutomaticoUseCase(
    private val repo: AgroRepository
) {
    private val mutex = Mutex()
    private var lastSyncTime = 0L
    private val syncIntervalMs = 30000L

    suspend fun sincronizarSiNecesario() {
        val now = System.currentTimeMillis()
        if (now - lastSyncTime < syncIntervalMs) return
        mutex.withLock {
            val now2 = System.currentTimeMillis()
            if (now2 - lastSyncTime < syncIntervalMs) return@withLock
            try {
                repo.sincronizar()
                repo.refrescar()
                lastSyncTime = System.currentTimeMillis()
            } catch (e: Exception) {
                // No propagar error en sincronización automática
            }
        }
    }

    fun observarYSincronizar(conectividad: Flow<Boolean>): Flow<SyncResult> = 
        conectividad
            .distinctUntilChanged()
            .filter { it }
            .flatMapLatest {
                flow {
                    emit(SyncResult.Syncing)
                    try {
                        repo.sincronizar()
                        repo.refrescar()
                        lastSyncTime = System.currentTimeMillis()
                        emit(SyncResult.Success)
                    } catch (e: Exception) {
                        emit(SyncResult.Error(e.message ?: "Error"))
                    }
                }
            }

    sealed class SyncResult {
        object Syncing : SyncResult()
        object Success : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
}
