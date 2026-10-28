package pe.upeu.agrosierra.domain.model

sealed class ErrorApi : Exception() {
    class Validacion(val errores: Map<String, String>) : ErrorApi()
    class NoAutorizado : ErrorApi()
    class SinPrivilegio : ErrorApi()
    class NoEncontrado : ErrorApi()
    class Conflicto(val motivo: String) : ErrorApi()
    class Servidor : ErrorApi()
    class SinConexion : ErrorApi()
    class TiempoAgotado : ErrorApi()
    class Desconocido(val mensaje: String) : ErrorApi() {
        override val message: String? get() = mensaje
    }
}
