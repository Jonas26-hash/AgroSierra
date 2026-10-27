package pe.upeu.agrosierra.data.local

expect class AlmacenSeguro {
    suspend fun guardarToken(token: String)
    suspend fun obtenerToken(): String?
    suspend fun borrarToken()
    suspend fun guardarSesion(usuario: String, rol: String, privilegios: Set<String>)
    suspend fun obtenerSesion(): Triple<String?, String?, Set<String>>?
    suspend fun borrarSesion()
}
