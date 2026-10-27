package pe.upeu.agrosierra.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PaginacionResponseDto<T>(
    val contenido: List<T>,
    val pagina: Int = 0,
    val tamanio: Int = 20,
    val totalElementos: Long = 0,
    val totalPaginas: Int = 0,
    val ultima: Boolean = false
)

@Serializable
data class ErrorResponseDto(
    val timestamp: String? = null,
    val status: Int = 0,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

@Serializable
data class LoteDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val hectareas: Double,
    val cultivo: String,
    val valle: String,
    val campaniaCerrada: Boolean = false,
    val dosisMaximaPorHectarea: Double = 0.0
)

@Serializable
data class LaborDto(
    val id: Long,
    val loteId: Long,
    val tipo: String,
    val fecha: String,
    val responsable: String,
    val cantidad: Double,
    val observacion: String = ""
)

@Serializable
data class LaborRequestDto(
    val loteId: Long,
    val tipo: String,
    val fecha: String,
    val responsable: String? = null,
    val cantidad: Double,
    val observacion: String = ""
)

@Serializable
data class UsuarioActualDto(
    val usuario: String,
    val rol: String = "",
    val privilegios: Set<String> = emptySet()
)
